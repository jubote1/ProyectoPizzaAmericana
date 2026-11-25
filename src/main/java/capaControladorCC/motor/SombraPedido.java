package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * La sombra de un pedido: leer lo que la tienda guardo, reconstruir lo que el
 * cajero eligio, y comparar eso contra lo que calcula el motor.
 *
 * POR QUE ESTA CLASE EXISTE APARTE. Esto mismo vivia dentro de ArnesSombra, que
 * solo corre en el central leyendo el datamart. Pero la tienda necesita hacer
 * exactamente lo mismo contra su propia base, y si se escribe dos veces van a
 * ser dos reglas distintas: una va a clasificar OK lo que la otra marca como
 * diferencia, y entonces la sombra deja de servir para decidir nada. Asi que la
 * regla esta UNA sola vez y la llaman los dos lados. ArnesSombra delega aqui.
 *
 * Solo depende del JDK, de Gson y del catalogo, nunca de un esquema fijo: el
 * esquema se recibe por parametro, igual que en CatalogoPos. En el central es
 * "datamart"; en la tienda, "tiendaamericana". Las dos bases tienen las tablas
 * pedido y detalle_pedido con las mismas columnas (comprobado 2026-11-24), por
 * eso el mismo codigo sirve en las dos.
 *
 * NO ESCRIBE NADA. Lee y compara; quien llama decide donde guarda el resultado.
 */
public class SombraPedido {

	private static final Gson GSON = new Gson();

	/** Productos del POS que son costo de domicilio: no son una linea pedida sino parte del contexto. */
	private static final int[] PRODUCTOS_DOMICILIO = { 411, 412 };

	private SombraPedido() {
	}

	public static boolean esDomicilio(int idProducto) {
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

	/** Una linea tal como la guardo la tienda. */
	public static class LineaTienda {
		public int id;
		public int idProducto;
		public double cantidad;
		public double valorUnitario;
		public double valorTotal;
		public int master;
		public int modificador;
		public int anulada;
	}

	/**
	 * Trae las lineas de un lote de pedidos, agrupadas por "idtienda|idpedidotienda".
	 * El lote son parejas {idpedidotienda, idtienda}.
	 */
	public static Map<String, List<LineaTienda>> leerLineas(Connection con, String esquema, List<int[]> lote)
			throws Exception {
		Map<String, List<LineaTienda>> mapa = new HashMap<String, List<LineaTienda>>();
		if (lote == null || lote.isEmpty()) {
			return mapa;
		}
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < lote.size(); i++) {
			in.append(i == 0 ? "(?,?)" : ",(?,?)");
		}
		String sql = "select idpedidotienda, idtienda, iddetalle_pedido, idproducto, cantidad, valorunitario, "
				+ "valortotal, iddetalle_pedido_master, iddetalle_modificador, ifnull(idmotivoanulacion,0) "
				+ "from " + esquema + ".detalle_pedido "
				+ "where (idpedidotienda, idtienda) in (" + in + ") order by iddetalle_pedido";
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

	/**
	 * Reconstruye lo que el cajero eligio: cada linea de nivel superior con sus
	 * hijas convertidas en selecciones o modificadores. El catalogo se usa para
	 * reconocer cuales hijas las agrego el sistema solo (productos incluidos) y
	 * para saber si un modificador es CON o SIN.
	 */
	public static MotorSolicitud construirSolicitud(CatalogoPos catalogo, String canal, int idTienda,
			int idTipoPedido, Timestamp fecha, List<LineaTienda> lineas) {
		MotorSolicitud s = new MotorSolicitud();
		s.contexto.canal = canal;
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
				CatalogoPos.Producto info = catalogo.productos.get(l.idProducto);
				m.tipo = info != null && "MS".equals(info.tipo) ? "SIN" : "CON";
				m.idproducto = l.idProducto;
				// cantidad POR UNIDAD de la linea principal (la tienda guarda el total ya multiplicado)
				m.cantidad = padre.cantidad > 0 ? l.cantidad / padre.cantidad : l.cantidad;
				LineaTienda modificada = porId.get(l.modificador);
				m.sobreProducto = modificada != null ? modificada.idProducto : 0;
				padre.modificadores.add(m);
			} else {
				if (loAgregoElSistema(catalogo, l, lineas)) {
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
	 * Una hija no es una eleccion del cajero si otra linea del mismo grupo (la
	 * maestra o una opcion hermana) trae ese producto como "incluido" en la tabla
	 * producto_incluido: la agrego el sistema solo.
	 */
	private static boolean loAgregoElSistema(CatalogoPos catalogo, LineaTienda hija, List<LineaTienda> lineas) {
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

	/** Lo que la tienda registro, puesto en la forma del resultado del motor. */
	public static MotorResultado construirEsperado(List<LineaTienda> lineas) {
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
			ml.refModificador = l.modificador == 0 ? 0
					: (refDeId.containsKey(l.modificador) ? refDeId.get(l.modificador) : 0);
			ml.origen = esDomicilio(l.idProducto) ? "DOMICILIO"
					: (l.modificador > 0 ? "MODIFICADOR" : (l.master > 0 ? "OPCION" : "SOLICITADA"));
			r.lineas.add(ml);
		}
		r.recalcularTotales();
		return r;
	}

	/** Clave de comparacion de una linea: producto, cantidad y valor unitario (sin refs ni orden). */
	private static String clave(MotorResultado.Linea l) {
		return l.idproducto + "|" + r2(l.cantidad) + "|" + r2(l.valorUnitario);
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

	/**
	 * Compara lo que registro la tienda contra lo que calculo el motor.
	 * Devuelve {clasificacion, detalle en json} y detalle es null cuando da OK.
	 *
	 * PRECIO_DISTINTO quiere decir que estan las mismas lineas (mismo producto y
	 * misma cantidad) y lo unico que cambia es el valor: tipicamente un cambio de
	 * precio posterior al pedido, no una falla del motor. LINEAS_DISTINTAS es lo
	 * grave: el motor armaria otras lineas.
	 */
	public static String[] comparar(MotorResultado esp, MotorResultado res) {
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
		if (mismasLineasSinPrecio(esp, res)) {
			return new String[] { "PRECIO_DISTINTO", GSON.toJson(detalle) };
		}
		return new String[] { totalIgual ? "LINEAS_DISTINTAS" : "TOTAL_DISTINTO", GSON.toJson(detalle) };
	}
}
