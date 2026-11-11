package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Arnes de repeticion del motor de pedido (etapa 2).
 *
 * Toma pedidos que la tienda YA registro (datamart), los convierte a la forma del contrato -lo que el
 * cliente eligio- y guarda al lado lo que la tienda cobro. Luego se le pasa cada caso a un motor y se
 * compara: es la prueba de cada paquete del motor contra historial real, sin enviar nada a nadie.
 *
 * Solo LEE de datamart y de pos_central; solo ESCRIBE en pos_central.sombra_caso y sombra_resultado.
 *
 * Uso desde linea de comandos (la conexion sale de -Dmotor.jdbc, -Dmotor.user y -Dmotor.password; si no
 * se dan, usa la conexion principal del central):
 *   extraer  yyyy-MM-dd yyyy-MM-dd maximo       carga casos de pedidos de la tienda en ese rango
 *   repetir  eco|noimpl  maximo                 corre un motor sobre los casos que aun no tienen resultado
 *   resumen  version                            cuenta los resultados por clasificacion
 */
public class ArnesSombra {

	private static final Gson GSON = new Gson();
	/** Productos del POS que son costo de domicilio: no son una linea pedida sino parte del contexto. */
	private static final int[] PRODUCTOS_DOMICILIO = { 411, 412 };
	private static final int LOTE = 200;

	private final Connection con;
	/** id de producto del POS -> {tipo_producto, descripcion}. */
	private final Map<Integer, String[]> productos = new HashMap<Integer, String[]>();
	/** Catalogo en memoria: sirve para reconocer cuales hijas las agrego el sistema (productos incluidos). */
	private final CatalogoPos catalogo;

	public ArnesSombra(Connection con) throws Exception {
		this.con = con;
		this.catalogo = CatalogoPos.cargar(con);
		try (Statement st = con.createStatement();
				ResultSet rs = st.executeQuery("select idproducto, tipo_producto, descripcion from pos_central.producto")) {
			while (rs.next()) {
				productos.put(rs.getInt(1), new String[] { rs.getString(2), rs.getString(3) });
			}
		}
	}

	private static boolean esDomicilio(int idProducto) {
		for (int d : PRODUCTOS_DOMICILIO) {
			if (d == idProducto) {
				return true;
			}
		}
		return false;
	}

	private static double r2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	/** Linea tal como la guardo la tienda. */
	private static class LineaTienda {
		int id;
		int idProducto;
		double cantidad;
		double valorUnitario;
		double valorTotal;
		int master;
		int modificador;
		int anulada;
	}

	/**
	 * Convierte los pedidos de la tienda en el rango en casos. Devuelve el numero de casos nuevos. Los pedidos
	 * que no se pueden convertir (lineas anuladas, productos que no estan en el catalogo del POS, sin lineas)
	 * se cuentan y se imprimen, no se guardan.
	 */
	public int extraerCasos(Timestamp desde, Timestamp hasta, int maximo) throws Exception {
		List<int[]> pedidos = new ArrayList<int[]>();
		List<Timestamp> fechas = new ArrayList<Timestamp>();
		List<Integer> tipos = new ArrayList<Integer>();
		String sqlPed = "select idpedidotienda, idtienda, fechapedido, idtipopedido from datamart.pedido p "
				+ "where p.fechapedido >= ? and p.fechapedido < ? and (p.idmotivoanulacion is null or p.idmotivoanulacion = 0) "
				+ "and not exists (select 1 from pos_central.sombra_caso c where c.origen = 'TIENDA_HIST' "
				+ "and c.idtienda = p.idtienda and c.idpedidotienda = p.idpedidotienda) "
				// Solo pedidos tomados en la pantalla de la tienda: los que vienen del central (BOT, contact center,
				// plataformas) llegan "planos" y se arman distinto; esos se prueban con el traductor del BOT.
				+ "and not exists (select 1 from pizzaamericana.pedido cp where cp.idtienda = p.idtienda "
				+ "and cp.numposheader = p.idpedidotienda) "
				// y tampoco los de plataformas, tienda virtual o CRM, que entran por otros caminos y quedan planos
				+ "and (p.idpedidoalt is null or p.idpedidoalt = '') and p.estacion not like '%Plat%' "
				+ "and p.estacion not in ('TIENDA VIRTUAL', 'CRM') "
				+ "order by p.fechapedido limit ?";
		try (PreparedStatement ps = con.prepareStatement(sqlPed)) {
			ps.setTimestamp(1, desde);
			ps.setTimestamp(2, hasta);
			ps.setInt(3, maximo);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					pedidos.add(new int[] { rs.getInt(1), rs.getInt(2) });
					fechas.add(rs.getTimestamp(3));
					tipos.add(rs.getInt(4));
				}
			}
		}
		int nuevos = 0, sinLineas = 0, anuladas = 0, desconocidos = 0;
		for (int ini = 0; ini < pedidos.size(); ini += LOTE) {
			int fin = Math.min(ini + LOTE, pedidos.size());
			Map<String, List<LineaTienda>> porPedido = leerLineas(pedidos.subList(ini, fin));
			for (int i = ini; i < fin; i++) {
				int idPed = pedidos.get(i)[0], idTienda = pedidos.get(i)[1];
				List<LineaTienda> lineas = porPedido.get(idTienda + "|" + idPed);
				if (lineas == null || lineas.isEmpty()) {
					sinLineas++;
					continue;
				}
				boolean anulada = false, desconocido = false;
				for (LineaTienda l : lineas) {
					if (l.anulada > 0) {
						anulada = true;
					}
					if (!productos.containsKey(l.idProducto)) {
						desconocido = true;
					}
				}
				if (anulada) {
					anuladas++;
					continue;
				}
				if (desconocido) {
					desconocidos++;
					continue;
				}
				MotorSolicitud sol = construirSolicitud(idTienda, tipos.get(i), fechas.get(i), lineas);
				MotorResultado esp = construirEsperado(lineas);
				if (insertarCaso("TIENDA_HIST", idTienda, idPed, fechas.get(i), sol, esp)) {
					nuevos++;
				}
			}
		}
		System.out.println("Pedidos revisados: " + pedidos.size() + " | casos nuevos: " + nuevos + " | sin lineas: "
				+ sinLineas + " | con lineas anuladas: " + anuladas + " | con producto fuera del catalogo POS: "
				+ desconocidos);
		return nuevos;
	}

	private Map<String, List<LineaTienda>> leerLineas(List<int[]> lote) throws Exception {
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < lote.size(); i++) {
			in.append(i == 0 ? "(?,?)" : ",(?,?)");
		}
		String sql = "select idpedidotienda, idtienda, iddetalle_pedido, idproducto, cantidad, valorunitario, valortotal, "
				+ "iddetalle_pedido_master, iddetalle_modificador, ifnull(idmotivoanulacion,0) from datamart.detalle_pedido "
				+ "where (idpedidotienda, idtienda) in (" + in + ") order by iddetalle_pedido";
		Map<String, List<LineaTienda>> mapa = new HashMap<String, List<LineaTienda>>();
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			int k = 1;
			for (int[] p : lote) {
				ps.setInt(k++, p[0]);
				ps.setInt(k++, p[1]);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					LineaTienda l = new LineaTienda();
					l.id = rs.getInt(3);
					l.idProducto = rs.getInt(4);
					l.cantidad = rs.getDouble(5);
					l.valorUnitario = rs.getDouble(6);
					l.valorTotal = rs.getDouble(7);
					l.master = rs.getInt(8);
					l.modificador = rs.getInt(9);
					l.anulada = rs.getInt(10);
					String clave = rs.getInt(2) + "|" + rs.getInt(1);
					List<LineaTienda> lista = mapa.get(clave);
					if (lista == null) {
						lista = new ArrayList<LineaTienda>();
						mapa.put(clave, lista);
					}
					lista.add(l);
				}
			}
		}
		return mapa;
	}

	/** Reconstruye lo que el cajero eligio: cada linea de nivel superior con sus hijas como selecciones. */
	private MotorSolicitud construirSolicitud(int idTienda, int idTipoPedido, Timestamp fecha, List<LineaTienda> lineas) {
		MotorSolicitud s = new MotorSolicitud();
		s.contexto.canal = "TIENDA";
		s.contexto.idtienda = idTienda;
		s.contexto.idtipopedido = idTipoPedido;
		s.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fecha);
		Map<Integer, LineaTienda> porId = new HashMap<Integer, LineaTienda>();
		for (LineaTienda l : lineas) {
			porId.put(l.id, l);
		}
		Map<Integer, MotorSolicitud.Linea> lineaDeMaster = new LinkedHashMap<Integer, MotorSolicitud.Linea>();
		for (LineaTienda l : lineas) {
			if (l.master != 0) {
				continue;
			}
			if (esDomicilio(l.idProducto)) {
				s.domicilio = new MotorSolicitud.Domicilio();
				s.domicilio.idproducto = l.idProducto;
				s.domicilio.valor = l.valorTotal;
				s.contexto.esDomicilio = true;
				continue;
			}
			MotorSolicitud.Linea ln = new MotorSolicitud.Linea();
			ln.idproducto = l.idProducto;
			ln.cantidad = l.cantidad;
			ln.valorReferencia = l.valorTotal;
			s.lineas.add(ln);
			lineaDeMaster.put(l.id, ln);
		}
		for (LineaTienda l : lineas) {
			if (l.master == 0) {
				continue;
			}
			MotorSolicitud.Linea padre = lineaDeMaster.get(l.master);
			if (padre == null) {
				continue;
			}
			if (l.modificador > 0) {
				MotorSolicitud.Modificador m = new MotorSolicitud.Modificador();
				String[] info = productos.get(l.idProducto);
				m.tipo = info != null && "MS".equals(info[0]) ? "SIN" : "CON";
				m.idproducto = l.idProducto;
				// cantidad POR UNIDAD de la linea principal (la tienda guarda el total ya multiplicado)
				m.cantidad = padre.cantidad > 0 ? l.cantidad / padre.cantidad : l.cantidad;
				LineaTienda modificada = porId.get(l.modificador);
				m.sobreProducto = modificada != null ? modificada.idProducto : 0;
				padre.modificadores.add(m);
			} else {
				if (loAgregoElSistema(l, lineas)) {
					continue;
				}
				MotorSolicitud.Seleccion sel = new MotorSolicitud.Seleccion();
				sel.idproductoOpcion = l.idProducto;
				// cantidad POR UNIDAD de la linea principal (la tienda guarda el total ya multiplicado)
				sel.cantidad = padre.cantidad > 0 ? l.cantidad / padre.cantidad : l.cantidad;
				padre.selecciones.add(sel);
			}
		}
		return s;
	}

	/**
	 * Una hija no es una eleccion del cajero si otra linea del mismo pedido (la maestra o una opcion hermana)
	 * trae ese producto como "incluido" en la tabla producto_incluido: la agrego el sistema solo.
	 */
	private boolean loAgregoElSistema(LineaTienda hija, List<LineaTienda> lineas) {
		for (LineaTienda x : lineas) {
			if (x == hija) {
				continue;
			}
			boolean mismoGrupo = x.id == hija.master || (x.master == hija.master && x.master != 0);
			if (!mismoGrupo) {
				continue;
			}
			List<CatalogoPos.Incluido> inc = catalogo.incluidos.get(x.idProducto);
			if (inc == null) {
				continue;
			}
			for (CatalogoPos.Incluido i : inc) {
				if (i.idProductoQueSeAgrega == hija.idProducto) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Concilia la sombra en vivo: para las filas de sombra_vivo que ya tuvieron tiempo de convertirse en pedido
	 * (mas de {@code minutosEspera} minutos), busca el pedido que se creo para ese lead, trae lo que la tienda
	 * registro y compara contra lo que calculo el motor. Escribe idpedido, subtotal_tienda y clasificacion.
	 * Las consultas a log_pedido_crmbot van por rango de idlog en lotes (esa tabla solo tiene indice por idlog).
	 */
	public int conciliarVivo(int maximo, int minutosEspera) throws Exception {
		List<Object[]> pend = new ArrayList<Object[]>();
		String sql = "select idsombra, lead_id, idlog_bot, resultado_json from pos_central.sombra_vivo "
				+ "where idpedido is null and clasificacion is null and fecha < now() - interval ? minute order by idsombra limit ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, minutosEspera);
			ps.setInt(2, maximo);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					pend.add(new Object[] { rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getString(4) });
				}
			}
		}
		int conciliados = 0, sinPedido = 0;
		for (int ini = 0; ini < pend.size(); ini += LOTE) {
			List<Object[]> lote = pend.subList(ini, Math.min(ini + LOTE, pend.size()));
			long minLog = Long.MAX_VALUE, maxLog = 0;
			StringBuilder in = new StringBuilder();
			for (Object[] o : lote) {
				minLog = Math.min(minLog, (Integer) o[2]);
				maxLog = Math.max(maxLog, (Integer) o[2]);
				in.append(in.length() == 0 ? "?" : ",?");
			}
			Map<Long, int[]> pedidoDeLead = new HashMap<Long, int[]>();
			String sqlP = "select l.lead_id, p.idpedido, p.idtienda, p.numposheader from pizzaamericana.log_pedido_crmbot l "
					+ "join pizzaamericana.pedido p on p.idpedido = l.idpedido where l.idlog >= ? and l.idlog < ? and l.tipo = 'I' "
					+ "and p.numposheader > 0 and l.lead_id in (" + in + ") order by l.idlog";
			try (PreparedStatement ps = con.prepareStatement(sqlP)) {
				ps.setLong(1, minLog);
				ps.setLong(2, maxLog + 3000);
				int k = 3;
				for (Object[] o : lote) {
					ps.setLong(k++, (Long) o[1]);
				}
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						pedidoDeLead.put(rs.getLong(1), new int[] { rs.getInt(2), rs.getInt(3), rs.getInt(4) });
					}
				}
			}
			List<int[]> claves = new ArrayList<int[]>();
			for (int[] v : pedidoDeLead.values()) {
				claves.add(new int[] { v[2], v[1] });
			}
			Map<String, List<LineaTienda>> lineasPorPedido = claves.isEmpty() ? new HashMap<String, List<LineaTienda>>()
					: leerLineas(claves);
			for (Object[] o : lote) {
				int[] v = pedidoDeLead.get((Long) o[1]);
				if (v == null) {
					sinPedido++;
					continue;
				}
				List<LineaTienda> lineas = lineasPorPedido.get(v[1] + "|" + v[2]);
				if (lineas == null || lineas.isEmpty()) {
					continue;
				}
				lineas.removeIf(x -> x.idProducto == 245 && x.master == 0);
				MotorResultado esp = construirEsperado(lineas);
				MotorResultado res = GSON.fromJson((String) o[3], MotorResultado.class);
				String[] cmp = comparar(esp, res);
				double dom = 0;
				for (MotorResultado.Linea l : esp.lineas) {
					if ("DOMICILIO".equals(l.origen)) {
						dom += l.valorTotal;
					}
				}
				try (PreparedStatement up = con.prepareStatement("update pos_central.sombra_vivo set idpedido = ?, "
						+ "subtotal_tienda = ?, clasificacion = ?, fecha_conciliacion = now() where idsombra = ?")) {
					up.setInt(1, v[0]);
					up.setDouble(2, esp.subtotal - dom);
					up.setString(3, cmp[0]);
					up.setLong(4, (Long) o[0]);
					up.executeUpdate();
					conciliados++;
				}
			}
		}
		System.out.println("Conciliados: " + conciliados + " | sin pedido todavia: " + sinPedido);
		return conciliados;
	}

	/** Borra los casos de pedidos de la tienda y sus resultados, para volver a extraer con otra regla. */
	public void limpiarCasos(String origen) throws Exception {
		try (PreparedStatement p1 = con.prepareStatement("delete r from pos_central.sombra_resultado r join "
				+ "pos_central.sombra_caso c on c.idcaso = r.idcaso where c.origen = ?");
				PreparedStatement p2 = con.prepareStatement("delete from pos_central.sombra_caso where origen = ?")) {
			p1.setString(1, origen);
			p1.executeUpdate();
			p2.setString(1, origen);
			System.out.println("Casos borrados (" + origen + "): " + p2.executeUpdate());
		}
	}

	/**
	 * Casos del BOT: toma los leads que el BOT dejo en log_pedido_crmbot (tipo 'I', con pedido creado en el
	 * central y enviado a la tienda), los traduce con el TraductorBot y guarda al lado lo que la tienda
	 * registro. El rango es por idlog (la tabla solo tiene indice por esa llave).
	 */
	public int extraerCasosBot(long idLogDesde, long idLogHasta, int maximo) throws Exception {
		TraductorBot traductor = new TraductorBot(con, catalogo);
		String sql = "select l.idlog, l.info_lead, p.idpedido, p.idtienda, p.numposheader, p.fechapedido "
				+ "from pizzaamericana.log_pedido_crmbot l join pizzaamericana.pedido p on p.idpedido = l.idpedido "
				+ "where l.idlog >= ? and l.idlog < ? and l.tipo = 'I' and p.numposheader > 0 order by l.idlog limit ?";
		List<Object[]> leads = new ArrayList<Object[]>();
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, idLogDesde);
			ps.setLong(2, idLogHasta);
			ps.setInt(3, maximo);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					leads.add(new Object[] { rs.getString(2), rs.getInt(4), rs.getInt(5), rs.getTimestamp(6) });
				}
			}
		}
		List<int[]> pedidos = new ArrayList<int[]>();
		for (Object[] o : leads) {
			pedidos.add(new int[] { (Integer) o[2], (Integer) o[1] });
		}
		int nuevos = 0, sinLineas = 0, anuladas = 0, desconocidos = 0, sinLead = 0;
		for (int ini = 0; ini < leads.size(); ini += LOTE) {
			int fin = Math.min(ini + LOTE, leads.size());
			Map<String, List<LineaTienda>> porPedido = leerLineas(pedidos.subList(ini, fin));
			for (int i = ini; i < fin; i++) {
				Object[] o = leads.get(i);
				int idTienda = (Integer) o[1], idPed = (Integer) o[2];
				List<LineaTienda> lineas = porPedido.get(idTienda + "|" + idPed);
				if (o[0] == null || ((String) o[0]).length() < 20) {
					sinLead++;
					continue;
				}
				if (lineas == null || lineas.isEmpty()) {
					sinLineas++;
					continue;
				}
				boolean anulada = false, desconocido = false;
				for (LineaTienda l : lineas) {
					anulada |= l.anulada > 0;
					desconocido |= !productos.containsKey(l.idProducto);
				}
				if (anulada) {
					anuladas++;
					continue;
				}
				if (desconocido) {
					desconocidos++;
					continue;
				}
				// El "Magnetico" (producto 245) lo agrega la tienda al recibir el pedido cuando el cliente es nuevo
				// en esa tienda (InsertarPedidoPixel); no lo pide el BOT ni es parte del calculo del motor.
				lineas.removeIf(x -> x.idProducto == 245 && x.master == 0);
				MotorSolicitud sol = traductor.traducir((String) o[0], idTienda);
				sol.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp) o[3]);
				MotorResultado esp = construirEsperado(lineas);
				if (insertarCaso("BOT", idTienda, idPed, (Timestamp) o[3], sol, esp)) {
					nuevos++;
				}
			}
		}
		System.out.println("Leads revisados: " + leads.size() + " | casos BOT nuevos: " + nuevos + " | sin lead: " + sinLead
				+ " | sin lineas: " + sinLineas + " | con lineas anuladas: " + anuladas + " | producto fuera del POS: "
				+ desconocidos);
		return nuevos;
	}

	/** Lo que la tienda registro, en la forma del resultado del motor. */
	private MotorResultado construirEsperado(List<LineaTienda> lineas) {
		MotorResultado r = new MotorResultado();
		r.estado = MotorResultado.OK;
		r.versionMotor = "tienda";
		Map<Integer, Integer> refDeId = new HashMap<Integer, Integer>();
		int ref = 1;
		for (LineaTienda l : lineas) {
			refDeId.put(l.id, ref++);
		}
		for (LineaTienda l : lineas) {
			MotorResultado.Linea ml = new MotorResultado.Linea();
			ml.ref = refDeId.get(l.id);
			ml.idproducto = l.idProducto;
			ml.cantidad = l.cantidad;
			ml.valorUnitario = l.valorUnitario;
			ml.valorTotal = l.valorTotal;
			ml.refMaster = l.master == 0 ? 0 : (refDeId.containsKey(l.master) ? refDeId.get(l.master) : 0);
			ml.refModificador = l.modificador == 0 ? 0 : (refDeId.containsKey(l.modificador) ? refDeId.get(l.modificador) : 0);
			ml.origen = esDomicilio(l.idProducto) ? "DOMICILIO"
					: (l.modificador > 0 ? "MODIFICADOR" : (l.master > 0 ? "OPCION" : "SOLICITADA"));
			r.lineas.add(ml);
		}
		r.recalcularTotales();
		return r;
	}

	private boolean insertarCaso(String origen, int idTienda, int idPedido, Timestamp fecha, MotorSolicitud sol,
			MotorResultado esp) throws Exception {
		String sql = "insert ignore into pos_central.sombra_caso (origen, idtienda, idpedidotienda, fechapedido, "
				+ "solicitud_json, esperado_json, subtotal_esperado, lineas_esperadas) values (?,?,?,?,?,?,?,?)";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, origen);
			ps.setInt(2, idTienda);
			ps.setInt(3, idPedido);
			ps.setTimestamp(4, fecha);
			ps.setString(5, GSON.toJson(sol));
			ps.setString(6, GSON.toJson(esp));
			ps.setDouble(7, esp.subtotal);
			ps.setInt(8, esp.lineas.size());
			return ps.executeUpdate() > 0;
		}
	}

	/**
	 * Corre un motor sobre los casos que aun no tienen resultado de esa version. Con eco=true no usa el motor:
	 * devuelve lo esperado tal cual, para comprobar que el arnes mismo funciona (debe dar 100 % OK).
	 */
	public int repetir(MotorPedido motor, boolean eco, int maximo, String origen) throws Exception {
		String version = eco ? "eco" : motor.version();
		String sqlCasos = "select idcaso, solicitud_json, esperado_json from pos_central.sombra_caso c where "
				+ "(? = '' or c.origen = ?) and not exists "
				+ "(select 1 from pos_central.sombra_resultado r where r.idcaso = c.idcaso and r.version_motor = ?) "
				+ "order by idcaso limit ?";
		List<Object[]> casos = new ArrayList<Object[]>();
		try (PreparedStatement ps = con.prepareStatement(sqlCasos)) {
			ps.setString(1, origen);
			ps.setString(2, origen);
			ps.setString(3, version);
			ps.setInt(4, maximo);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					casos.add(new Object[] { rs.getLong(1), rs.getString(2), rs.getString(3) });
				}
			}
		}
		Timestamp ahora = new Timestamp(System.currentTimeMillis());
		String sqlIns = "insert into pos_central.sombra_resultado (idcaso, version_motor, fecha_corrida, estado_motor, "
				+ "clasificacion, subtotal_motor, diferencia, detalle_json) values (?,?,?,?,?,?,?,?)";
		int hechos = 0;
		try (PreparedStatement ins = con.prepareStatement(sqlIns)) {
			for (Object[] c : casos) {
				MotorSolicitud sol = GSON.fromJson((String) c[1], MotorSolicitud.class);
				MotorResultado esp = GSON.fromJson((String) c[2], MotorResultado.class);
				MotorResultado res;
				if (eco) {
					res = GSON.fromJson((String) c[2], MotorResultado.class);
					res.versionMotor = "eco";
				} else {
					try {
						res = motor.cotizar(sol);
					} catch (Throwable t) {
						res = MotorResultado.conEstado(MotorResultado.ERROR, version, "EXCEPCION", String.valueOf(t));
					}
				}
				String[] cmp = comparar(esp, res);
				ins.setLong(1, (Long) c[0]);
				ins.setString(2, version);
				ins.setTimestamp(3, ahora);
				ins.setString(4, res.estado);
				ins.setString(5, cmp[0]);
				if (MotorResultado.OK.equals(res.estado)) {
					ins.setDouble(6, res.subtotal);
					ins.setDouble(7, r2(res.subtotal - esp.subtotal));
				} else {
					ins.setNull(6, java.sql.Types.DOUBLE);
					ins.setNull(7, java.sql.Types.DOUBLE);
				}
				// Un resultado OK puede traer advertencias (pregunta sin respuesta, opcion fuera de pregunta...):
				// se guardan para saber que regla falta aunque el total coincida.
				String detalle = cmp[1];
				if (detalle == null && res.advertencias != null && !res.advertencias.isEmpty()) {
					detalle = GSON.toJson(res.advertencias);
				}
				ins.setString(8, detalle);
				ins.addBatch();
				if (++hechos % 500 == 0) {
					ins.executeBatch();
				}
			}
			ins.executeBatch();
		}
		System.out.println("Casos corridos con '" + version + "': " + hechos);
		return hechos;
	}

	private static boolean mismasLineasSinPrecio(MotorResultado esp, MotorResultado res) {
		Map<String, Integer> a = new HashMap<String, Integer>();
		Map<String, Integer> b = new HashMap<String, Integer>();
		for (MotorResultado.Linea l : esp.lineas) {
			a.merge(l.idproducto + "|" + r2(l.cantidad), 1, Integer::sum);
		}
		for (MotorResultado.Linea l : res.lineas) {
			b.merge(l.idproducto + "|" + r2(l.cantidad), 1, Integer::sum);
		}
		return a.equals(b);
	}

	/** Clave de comparacion de una linea: producto, cantidad y valor unitario (sin refs ni orden). */
	private static String clave(MotorResultado.Linea l) {
		return l.idproducto + "|" + r2(l.cantidad) + "|" + r2(l.valorUnitario);
	}

	/** Devuelve {clasificacion, detalle}. */
	static String[] comparar(MotorResultado esp, MotorResultado res) {
		if (MotorResultado.NO_IMPLEMENTADO.equals(res.estado)) {
			return new String[] { "NO_IMPLEMENTADO", GSON.toJson(res.advertencias) };
		}
		if (MotorResultado.RECHAZADO.equals(res.estado)) {
			return new String[] { "RECHAZADO", GSON.toJson(res.advertencias) };
		}
		if (!MotorResultado.OK.equals(res.estado)) {
			return new String[] { "ERROR", GSON.toJson(res.advertencias) };
		}
		Map<String, Integer> faltan = new LinkedHashMap<String, Integer>();
		Map<String, Integer> sobran = new LinkedHashMap<String, Integer>();
		for (MotorResultado.Linea l : esp.lineas) {
			faltan.merge(clave(l), 1, Integer::sum);
		}
		for (MotorResultado.Linea l : res.lineas) {
			sobran.merge(clave(l), 1, Integer::sum);
		}
		Map<String, Integer> soloEsperado = new LinkedHashMap<String, Integer>();
		Map<String, Integer> soloMotor = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> e : faltan.entrySet()) {
			int otro = sobran.containsKey(e.getKey()) ? sobran.get(e.getKey()) : 0;
			if (e.getValue() > otro) {
				soloEsperado.put(e.getKey(), e.getValue() - otro);
			}
		}
		for (Map.Entry<String, Integer> e : sobran.entrySet()) {
			int otro = faltan.containsKey(e.getKey()) ? faltan.get(e.getKey()) : 0;
			if (e.getValue() > otro) {
				soloMotor.put(e.getKey(), e.getValue() - otro);
			}
		}
		boolean totalIgual = Math.abs(res.subtotal - esp.subtotal) < 0.5;
		boolean lineasIguales = soloEsperado.isEmpty() && soloMotor.isEmpty();
		if (totalIgual && lineasIguales) {
			return new String[] { "OK", null };
		}
		Map<String, Object> detalle = new LinkedHashMap<String, Object>();
		detalle.put("subtotal_esperado", esp.subtotal);
		detalle.put("subtotal_motor", res.subtotal);
		detalle.put("solo_en_tienda", soloEsperado);
		detalle.put("solo_en_motor", soloMotor);
		// Si las mismas lineas (producto y cantidad) estan en los dos lados y solo cambia el valor unitario, es
		// una diferencia de PRECIO (por ejemplo un cambio de precio posterior al pedido), no de estructura.
		if (mismasLineasSinPrecio(esp, res)) {
			return new String[] { "PRECIO_DISTINTO", GSON.toJson(detalle) };
		}
		return new String[] { totalIgual ? "LINEAS_DISTINTAS" : "TOTAL_DISTINTO", GSON.toJson(detalle) };
	}

	public void resumen(String version, String origen) throws Exception {
		String sql = "select r.clasificacion, count(*) n from pos_central.sombra_resultado r join pos_central.sombra_caso c "
				+ "on c.idcaso = r.idcaso where r.version_motor = ? and (? = '' or c.origen = ?) "
				+ "group by r.clasificacion order by n desc";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, version);
			ps.setString(2, origen);
			ps.setString(3, origen);
			try (ResultSet rs = ps.executeQuery()) {
				int total = 0;
				List<String> filas = new ArrayList<String>();
				while (rs.next()) {
					filas.add(rs.getString(1) + ": " + rs.getInt(2));
					total += rs.getInt(2);
				}
				System.out.println("Version '" + version + "' - " + total + " resultados");
				for (String f : filas) {
					System.out.println("  " + f);
				}
			}
		}
	}

	/**
	 * Analisis de diferencias: cuenta los productos que mas aparecen solo en la tienda o solo en el motor, y
	 * muestra algunos casos completos. Sirve para decidir que regla falta.
	 */
	@SuppressWarnings("unchecked")
	public void detalle(String version, String clasificacion, int ejemplos, String origen) throws Exception {
		Map<String, Integer> soloTienda = new HashMap<String, Integer>();
		Map<String, Integer> soloMotor = new HashMap<String, Integer>();
		List<String> muestras = new ArrayList<String>();
		String sql = "select r.idcaso, r.detalle_json, c.solicitud_json from pos_central.sombra_resultado r "
				+ "join pos_central.sombra_caso c on c.idcaso = r.idcaso where r.version_motor = ? and r.clasificacion = ? "
				+ "and (? = '' or c.origen = ?)";
		int total = 0;
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, version);
			ps.setString(2, clasificacion);
			ps.setString(3, origen);
			ps.setString(4, origen);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					total++;
					Map<String, Object> d = GSON.fromJson(rs.getString(2), Map.class);
					if (d == null) {
						continue;
					}
					acumular((Map<String, Object>) d.get("solo_en_tienda"), soloTienda);
					acumular((Map<String, Object>) d.get("solo_en_motor"), soloMotor);
					if (muestras.size() < ejemplos) {
						muestras.add("caso " + rs.getLong(1) + "\n  detalle: " + rs.getString(2) + "\n  solicitud: " + rs.getString(3));
					}
				}
			}
		}
		System.out.println(clasificacion + " (" + version + "): " + total + " casos");
		imprimirTop("Productos que aparecen SOLO EN LA TIENDA (producto|cantidad|valor unitario)", soloTienda);
		imprimirTop("Productos que aparecen SOLO EN EL MOTOR", soloMotor);
		for (String m : muestras) {
			System.out.println(m);
		}
	}

	private void acumular(Map<String, Object> origen, Map<String, Integer> destino) {
		if (origen == null) {
			return;
		}
		for (Map.Entry<String, Object> e : origen.entrySet()) {
			destino.merge(e.getKey(), ((Number) e.getValue()).intValue(), Integer::sum);
		}
	}

	private void imprimirTop(String titulo, Map<String, Integer> mapa) {
		System.out.println(titulo);
		mapa.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25).forEach(e -> {
			String idp = e.getKey().split("\\|")[0];
			String[] info = productos.get(Integer.parseInt(idp));
			System.out.println("  " + e.getValue() + " x " + e.getKey() + "  " + (info == null ? "?" : info[1]));
		});
	}

	private static Connection abrirConexion() throws Exception {
		String url = System.getProperty("motor.jdbc");
		if (url == null || url.isEmpty()) {
			return new conexionCC.ConexionBaseDatos().obtenerConexionBDPrincipal();
		}
		Class.forName("com.mysql.cj.jdbc.Driver");
		return DriverManager.getConnection(url, System.getProperty("motor.user", ""), System.getProperty("motor.password", ""));
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Uso: extraer <desde> <hasta> <max> | extraerbot <idlogDesde> <idlogHasta> <max> | "
					+ "repetir <eco|noimpl|v1> <max> [origen] | resumen <version> [origen] | "
					+ "detalle <version> <clasificacion> <n> [origen] | limpiar <origen>");
			return;
		}
		try (Connection con = abrirConexion()) {
			ArnesSombra arnes = new ArnesSombra(con);
			switch (args[0]) {
			case "extraer":
				arnes.extraerCasos(Timestamp.valueOf(args[1] + " 00:00:00"), Timestamp.valueOf(args[2] + " 00:00:00"),
						Integer.parseInt(args[3]));
				break;
			case "conciliarvivo":
				arnes.conciliarVivo(Integer.parseInt(args[1]), args.length > 2 ? Integer.parseInt(args[2]) : 30);
				break;
			case "extraerbot":
				arnes.extraerCasosBot(Long.parseLong(args[1]), Long.parseLong(args[2]), Integer.parseInt(args[3]));
				break;
			case "repetir":
				String vArg = args[1];
				String vMotor = "v1".equals(vArg) ? MotorPedidoV1.VERSION : vArg;
				MotorPedido motor = vArg.startsWith("v") ? new MotorPedidoV1(arnes.catalogo, vMotor)
						: new MotorPedidoNoImplementado();
				arnes.repetir(motor, "eco".equals(vArg), Integer.parseInt(args[2]), args.length > 3 ? args[3] : "");
				break;
			case "limpiar":
				arnes.limpiarCasos(args.length > 1 ? args[1] : "TIENDA_HIST");
				break;
			case "detalle":
				arnes.detalle(args[1], args[2], Integer.parseInt(args[3]), args.length > 4 ? args[4] : "");
				break;
			case "resumen":
				arnes.resumen(args[1], args.length > 2 ? args[2] : "");
				break;
			default:
				System.out.println("Comando desconocido: " + args[0]);
			}
		}
	}
}
