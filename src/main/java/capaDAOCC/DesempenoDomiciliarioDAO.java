package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El desempeno de un domiciliario, barriendo las tiendas.
 *
 * DE DONDE SALE CADA COSA
 *
 * El domiciliario vive en general.empleado, pero su trabajo queda en la base
 * LOCAL de cada tienda: despacho_real y despacho_real_det. Las copias que hay
 * en el central -pizzaamericana.despacho_real y _det- estan en cero, nadie las
 * replica. Por eso hay que ir tienda por tienda.
 *
 * De despacho_real se usa hora_salida y hora_regreso; de despacho_real_det,
 * hora_entrega por pedido. Las otras columnas del detalle estan vacias en
 * produccion -verificado el 2026-09-17 en Bello, 1.321 filas de 30 dias:
 * orden_entrega, hora_llegada_tienda e incidencia_tipo_despacho_id en NULL
 * todas-, asi que no se leen: mostrarlas seria mostrar una columna vacia.
 *
 * LOS DOS TIEMPOS, QUE NO SON EL MISMO
 *
 * La promesa al cliente se mide desde que entra el pedido -pedido.fechainsercion-
 * hasta la entrega, contra pedido.tiempopedido, que es 30 o 60 minutos. Ahi
 * adentro esta la cocina y el horno, que no dependen del domiciliario.
 *
 * Por eso se calcula tambien el tiempo EN CALLE, de hora_salida a hora_entrega,
 * que si es suyo. Juzgar a un domiciliario solo por la promesa es cobrarle la
 * demora de la cocina.
 *
 * EL REGRESO A LA TIENDA
 *
 * Es la ultima entrega de la salida contra hora_regreso. Verificado que
 * hora_regreso es exactamente la marcada de llegada que queda en
 * log_entrada_domiciliario: los dos valores coinciden al segundo, asi que no
 * hace falta cruzar las dos tablas.
 *
 * Pero el dato viene sucio y hay que decirlo en vez de promediarlo. En Bello,
 * 30 dias, 840 salidas: 659 regresos entre 0 y 15 minutos, 53 entre 16 y 120,
 * 50 por encima de 120 -hasta 8.497 minutos, seis dias- y 78 NEGATIVOS, con el
 * regreso marcado antes que la ultima entrega. Promediar todo eso da 156
 * minutos y no significa nada. Aca los tres casos se cuentan aparte y el
 * promedio sale solo de los validos.
 */
public class DesempenoDomiciliarioDAO {

	/** Por encima de esto no fue un regreso: fue que no lo marcaron. */
	public static final int MINUTOS_REGRESO_MAXIMO = 120;

	/**
	 * A partir de aca el regreso cuenta como demorado. Es el mismo valor de
	 * ESPERAALERTAMINUTOS del enrutamiento, para que las dos pantallas no
	 * llamen tarde a cosas distintas.
	 */
	public static final int MINUTOS_REGRESO_ALERTA = 15;

	// =======================================================================
	// Lo que se devuelve
	// =======================================================================

	public static class Domiciliario {
		public int id;
		public String nombre = "";
		public String nombreLargo = "";
		public String tipo = "";
	}

	/** Un pedido entregado. */
	public static class Entrega {
		public int idTienda;
		public String tienda = "";
		public String fecha = "";
		public int idPedido;
		public int prometido;
		public int minutosTotal;
		public int minutosCalle;
		public int orden;
		public boolean medible;
		/** Pedido programado (horario acordado con el cliente): se entrega, pero no se juzga por tiempo. */
		public boolean programado;
	}

	/** Una salida: el domiciliario sale con uno o varios pedidos y vuelve. */
	public static class Salida {
		public int idTienda;
		public String tienda = "";
		public String fecha = "";
		public int pedidos;
		public int minutosRegreso;
		public String estadoRegreso = "";
	}

	/** Lo que paso con las sugerencias de enrutamiento. */
	public static class Enrutamiento {
		public int sugerenciasRecibidas;
		public int aceptadas;
		public int rechazadas;
		public int expiradas;
		public int pendientes;
		public int vecesReasignada;
		public int pedidosLlevados;
		public int pedidosReasignados;
		public int pedidosSinLlevar;
		public int salidasDesdeSugerencia;
	}

	public static class ResultadoTienda {
		public int idTienda;
		public String tienda = "";
		public boolean conecto;
		public String error = "";
		/** TIENDA si se le pregunto a la tienda en vivo, DATAMART si salio de la replica diaria. */
		public String fuente = "TIENDA";
		/** Solo DATAMART: la ultima fecha que el datamart tiene de esta tienda. */
		public String datosHasta = "";
		public ArrayList<Entrega> entregas = new ArrayList<Entrega>();
		public ArrayList<Salida> salidas = new ArrayList<Salida>();
		public Enrutamiento enrutamiento = new Enrutamiento();
	}

	// =======================================================================
	// Los domiciliarios
	// =======================================================================

	/**
	 * Los domiciliarios activos.
	 *
	 * Quien es domiciliario no se decide por el nombre del cargo sino por la
	 * marca tipo_empleado.es_domiciliario, que hoy solo tiene el tipo 3. Y se
	 * miran los DOS tipos del empleado: empleado.idtipoempleado2 existe
	 * justamente para el que hace dos cosas, y si solo se mirara el primero se
	 * perderia al que reparte como segundo oficio.
	 */
	public static ArrayList<Domiciliario> obtenerDomiciliarios() {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<Domiciliario> lista = new ArrayList<Domiciliario>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDGeneral();
			final String sql = "select e.id, e.nombre, e.nombre_largo, t.descripcion "
					+ "from empleado e, tipo_empleado t "
					+ "where t.idtipoempleado in (e.idtipoempleado, e.idtipoempleado2) "
					+ "and t.es_domiciliario = '1' and e.activo = '1' "
					+ "group by e.id, e.nombre, e.nombre_largo, t.descripcion "
					+ "order by e.nombre_largo";
			final PreparedStatement ps = cn.prepareStatement(sql);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Domiciliario d = new Domiciliario();
				d.id = rs.getInt("id");
				d.nombre = texto(rs.getString("nombre"));
				d.nombreLargo = texto(rs.getString("nombre_largo"));
				d.tipo = texto(rs.getString("descripcion"));
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("DesempenoDomiciliarioDAO.obtenerDomiciliarios: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// =======================================================================
	// Una tienda
	// =======================================================================

	/**
	 * Todo lo del domiciliario en UNA tienda. Devuelve siempre un resultado,
	 * nunca null: si la tienda no contesta se marca conecto=false y la
	 * pantalla lo dice, que es distinto de decir que ese dia no trabajo.
	 *
	 * @param desde y hasta en yyyy-MM-dd
	 */
	public static ResultadoTienda consultarTienda(final int idTienda, final String nombreTienda,
			final String hosbd, final int idEmpleado, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ResultadoTienda res = new ResultadoTienda();
		res.idTienda = idTienda;
		res.tienda = texto(nombreTienda);

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDTiendaRemota(hosbd);
			if (cn == null) {
				res.conecto = false;
				res.error = "No contesto el computador de la tienda";
				return (res);
			}
			res.conecto = true;
			cargarEntregas(cn, res, idEmpleado, desde, hasta, false);
			cargarSalidas(cn, res, idEmpleado, desde, hasta, false);
			cargarEnrutamiento(cn, res, idEmpleado, desde, hasta, false);
		} catch (final Exception e) {
			logger.error("DesempenoDomiciliarioDAO.consultarTienda " + hosbd + ": " + e.toString());
			res.error = e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/**
	 * Lo mismo que consultarTienda, pero leyendo del DATAMART en vez de la tienda.
	 *
	 * Sirve cuando la tienda esta apagada. El datamart lo llena ServicioReplicaPedidos una vez al dia, de
	 * madrugada, con el dia de ayer: por eso trae datos HASTA AYER y no incluye lo de hoy. Se devuelve en
	 * datosHasta la ultima fecha que tiene esa tienda, para que la pantalla lo diga.
	 *
	 * En el datamart los ids se repiten de una tienda a otra (idpedidotienda, despacho_real.id...), asi que
	 * TODA consulta de aqui filtra y cruza por idtienda.
	 */
	public static ResultadoTienda consultarTiendaDatamart(final int idTienda, final String nombreTienda,
			final int idEmpleado, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ResultadoTienda res = new ResultadoTienda();
		res.idTienda = idTienda;
		res.tienda = texto(nombreTienda);
		res.fuente = "DATAMART";

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDDatamartLocal();
			if (cn == null) {
				res.conecto = false;
				res.error = "No se pudo conectar con el datamart";
				return (res);
			}
			res.conecto = true;
			res.datosHasta = ultimaFechaDatamart(cn, idTienda);
			cargarEntregas(cn, res, idEmpleado, desde, hasta, true);
			cargarSalidas(cn, res, idEmpleado, desde, hasta, true);
			cargarEnrutamiento(cn, res, idEmpleado, desde, hasta, true);
		} catch (final Exception e) {
			logger.error("DesempenoDomiciliarioDAO.consultarTiendaDatamart " + idTienda + ": " + e.toString());
			res.error = e.toString();
			res.conecto = false;
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/**
	 * La ultima fecha de despachos que el datamart tiene de una tienda, o "" si no tiene ninguna. Es hasta
	 * donde se puede confiar en lo que se muestra de esa tienda.
	 */
	public static String ultimaFechaDatamart(final Connection cn, final int idTienda) {
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"select max(fecha) as f from despacho_real where idtienda = ?");
			ps.setInt(1, idTienda);
			final ResultSet rs = ps.executeQuery();
			String f = "";
			if (rs.next() && rs.getString("f") != null) {
				f = rs.getString("f");
			}
			rs.close();
			ps.close();
			return (f);
		} catch (final Exception e) {
			return ("");
		}
	}

	/**
	 * Cual de las tiendas dadas tiene datos en el datamart y hasta que fecha, para ofrecerle a quien consulta
	 * "no contesto, pero lo tengo hasta tal dia". Devuelve idtienda -> ultima fecha.
	 */
	public static java.util.HashMap<Integer, String> datosDisponiblesDatamart(final ArrayList<Integer> idsTienda) {
		final java.util.HashMap<Integer, String> disponibles = new java.util.HashMap<Integer, String>();
		if (idsTienda == null || idsTienda.isEmpty()) {
			return (disponibles);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDDatamartLocal();
			if (cn == null) {
				return (disponibles);
			}
			for (final Integer id : idsTienda) {
				final String f = ultimaFechaDatamart(cn, id.intValue());
				if (f.length() > 0) {
					disponibles.put(id, f);
				}
			}
		} finally {
			cerrar(cn);
		}
		return (disponibles);
	}

	/** Un renglon por pedido entregado. */
	private static void cargarEntregas(final Connection cn, final ResultadoTienda res,
			final int idEmpleado, final String desde, final String hasta, final boolean datamart) throws Exception {
		//Los minutos los calcula MySQL: traer las fechas y restarlas en Java
		//obligaria a parsear, y el formato de fecha ya ha dado guerra aca.
		//En el datamart cada cruce lleva idtienda: los ids se repiten entre tiendas.
		final String sql = "select r.fecha, d.id_pedido, d.orden_planificada, "
				+ "ifnull(p.tiempopedido, 0) as prometido, ifnull(p.programado, 'N') as programado, "
				+ "timestampdiff(minute, p.fechainsercion, d.hora_entrega) as min_total, "
				+ "timestampdiff(minute, r.hora_salida, d.hora_entrega) as min_calle "
				+ "from despacho_real r "
				+ "inner join despacho_real_det d on d.despacho_real_id = r.id"
				+ (datamart ? " and d.idtienda = r.idtienda " : " ")
				+ "left join pedido p on p.idpedidotienda = d.id_pedido"
				+ (datamart ? " and p.idtienda = r.idtienda " : " ")
				+ "where " + (datamart ? "r.idtienda = ? and " : "")
				+ "r.id_domiciliario = ? and r.fecha >= ? and r.fecha <= ? "
				+ "and d.hora_entrega is not null "
				+ "order by r.fecha, r.id, d.orden_planificada";
		final PreparedStatement ps = cn.prepareStatement(sql);
		int k = 1;
		if (datamart) {
			ps.setInt(k++, res.idTienda);
		}
		ps.setInt(k++, idEmpleado);
		ps.setString(k++, desde);
		ps.setString(k++, hasta);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final Entrega e = new Entrega();
			e.idTienda = res.idTienda;
			e.tienda = res.tienda;
			e.fecha = texto(rs.getString("fecha"));
			e.idPedido = rs.getInt("id_pedido");
			e.prometido = rs.getInt("prometido");
			e.minutosTotal = rs.getInt("min_total");
			final boolean sinTotal = rs.wasNull();
			e.minutosCalle = rs.getInt("min_calle");
			e.orden = rs.getInt("orden_planificada");
			e.programado = "S".equals(rs.getString("programado"));
			//Sin promesa no hay contra que medir, y un tiempo negativo es un
			//dato malo, no una entrega instantanea. Y un pedido PROGRAMADO nunca es medible por tiempo:
			//el cliente pidio una hora concreta, no "lo mas rapido posible", asi que compararlo contra
			//pedido.tiempopedido (pensado para el pedido inmediato) mezclaria dos promesas distintas. Su
			//cumplimiento se cuenta aparte, en "programados" (ver DesempenoDomiciliarioCtrl).
			e.medible = (e.prometido > 0 && !sinTotal && e.minutosTotal >= 0 && !e.programado);
			res.entregas.add(e);
		}
		rs.close();
		ps.close();
	}

	/** Un renglon por salida, con el regreso a la tienda. */
	private static void cargarSalidas(final Connection cn, final ResultadoTienda res,
			final int idEmpleado, final String desde, final String hasta, final boolean datamart) throws Exception {
		final String sql = "select r.id, r.fecha, count(d.id) as pedidos, "
				+ "(r.hora_regreso is null) as sin_regreso, "
				+ "timestampdiff(minute, max(d.hora_entrega), r.hora_regreso) as min_regreso "
				+ "from despacho_real r "
				+ "left join despacho_real_det d on d.despacho_real_id = r.id"
				+ (datamart ? " and d.idtienda = r.idtienda " : " ")
				+ "where " + (datamart ? "r.idtienda = ? and " : "")
				+ "r.id_domiciliario = ? and r.fecha >= ? and r.fecha <= ? "
				+ "group by r.id, r.fecha, r.hora_regreso "
				+ "order by r.fecha, r.id";
		final PreparedStatement ps = cn.prepareStatement(sql);
		int k = 1;
		if (datamart) {
			ps.setInt(k++, res.idTienda);
		}
		ps.setInt(k++, idEmpleado);
		ps.setString(k++, desde);
		ps.setString(k++, hasta);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final Salida s = new Salida();
			s.idTienda = res.idTienda;
			s.tienda = res.tienda;
			s.fecha = texto(rs.getString("fecha"));
			s.pedidos = rs.getInt("pedidos");
			final boolean sinRegreso = rs.getBoolean("sin_regreso");
			final int minutos = rs.getInt("min_regreso");
			final boolean nulo = rs.wasNull();
			s.minutosRegreso = minutos;
			if (sinRegreso) {
				s.estadoRegreso = "SINREGRESO";
			} else if (nulo) {
				//Hay regreso pero no hay entrega contra que medirlo.
				s.estadoRegreso = "SINENTREGA";
			} else if (minutos < 0) {
				s.estadoRegreso = "NEGATIVO";
			} else if (minutos > MINUTOS_REGRESO_MAXIMO) {
				s.estadoRegreso = "SINMARCAR";
			} else {
				s.estadoRegreso = "OK";
			}
			res.salidas.add(s);
		}
		rs.close();
		ps.close();
	}

	/**
	 * Lo del enrutamiento.
	 *
	 * Va en su propio try: es lo mas nuevo que hay -las tablas las creo la
	 * migracion 2026_09_10_03- y si en alguna tienda no alcanzaron a correrla,
	 * el resto de la pantalla tiene que seguir funcionando. Lo mismo en el datamart, donde estas tablas
	 * las crea la migracion 2026_10_06_01 y empiezan a llenarse con la replica de la madrugada.
	 */
	private static void cargarEnrutamiento(final Connection cn, final ResultadoTienda res,
			final int idEmpleado, final String desde, final String hasta, final boolean datamart) {
		final Logger logger = Logger.getLogger("log_file");
		final Enrutamiento en = res.enrutamiento;
		//En el datamart cada consulta filtra por idtienda y los cruces entre tablas tambien.
		final String filtroS = datamart ? " and s.idtienda = ? " : " ";
		try {
			PreparedStatement ps = cn.prepareStatement(
					"select s.estado, count(*) as veces from pedido_sugerencia s "
					+ "where s.id_domiciliario = ? and s.fecha_jornada >= ? and s.fecha_jornada <= ?" + filtroS
					+ "group by s.estado");
			int k = 1;
			ps.setInt(k++, idEmpleado);
			ps.setString(k++, desde);
			ps.setString(k++, hasta);
			if (datamart) {
				ps.setInt(k++, res.idTienda);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String estado = texto(rs.getString("estado"));
				final int veces = rs.getInt("veces");
				en.sugerenciasRecibidas = en.sugerenciasRecibidas + veces;
				if ("ACEPTADA".equals(estado)) {
					en.aceptadas = veces;
				} else if ("RECHAZADA".equals(estado)) {
					en.rechazadas = veces;
				} else if ("EXPIRADA".equals(estado)) {
					en.expiradas = veces;
				} else if ("PENDIENTE".equals(estado)) {
					en.pendientes = veces;
				}
			}
			rs.close();
			ps.close();

			//Cuantas veces le movieron un pedido que ya le habian sugerido.
			ps = cn.prepareStatement(
					"select count(*) as veces from pedido_sugerencia_log l "
					+ "inner join pedido_sugerencia s on s.id = l.pedido_sugerencia_id"
					+ (datamart ? " and s.idtienda = l.idtienda " : " ")
					+ "where s.id_domiciliario = ? and s.fecha_jornada >= ? and s.fecha_jornada <= ?" + filtroS
					+ "and l.accion = 'REASIGNADA'");
			k = 1;
			ps.setInt(k++, idEmpleado);
			ps.setString(k++, desde);
			ps.setString(k++, hasta);
			if (datamart) {
				ps.setInt(k++, res.idTienda);
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				en.vecesReasignada = rs.getInt("veces");
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement(
					"select t.estado_det, count(*) as veces from pedido_sugerencia_det t "
					+ "inner join pedido_sugerencia s on s.id = t.pedido_sugerencia_id"
					+ (datamart ? " and s.idtienda = t.idtienda " : " ")
					+ "where s.id_domiciliario = ? and s.fecha_jornada >= ? and s.fecha_jornada <= ?" + filtroS
					+ "group by t.estado_det");
			k = 1;
			ps.setInt(k++, idEmpleado);
			ps.setString(k++, desde);
			ps.setString(k++, hasta);
			if (datamart) {
				ps.setInt(k++, res.idTienda);
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				final String estado = texto(rs.getString("estado_det"));
				final int veces = rs.getInt("veces");
				if ("LLEVADO".equals(estado)) {
					en.pedidosLlevados = veces;
				} else if ("REASIGNADO".equals(estado)) {
					en.pedidosReasignados = veces;
				} else {
					en.pedidosSinLlevar = en.pedidosSinLlevar + veces;
				}
			}
			rs.close();
			ps.close();

			//Cuantas de sus salidas nacieron de una sugerencia y no de la
			//asignacion a mano de siempre.
			ps = cn.prepareStatement(
					"select count(*) as veces from despacho_real r "
					+ "where " + (datamart ? "r.idtienda = ? and " : "")
					+ "r.id_domiciliario = ? and r.fecha >= ? and r.fecha <= ? "
					+ "and r.origen = 'SUGERIDO'");
			k = 1;
			if (datamart) {
				ps.setInt(k++, res.idTienda);
			}
			ps.setInt(k++, idEmpleado);
			ps.setString(k++, desde);
			ps.setString(k++, hasta);
			rs = ps.executeQuery();
			if (rs.next()) {
				en.salidasDesdeSugerencia = rs.getInt("veces");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			logger.error("DesempenoDomiciliarioDAO.cargarEnrutamiento tienda " + res.idTienda + ": "
					+ e.toString());
		}
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DesempenoDomiciliarioDAO: no cerro la conexion, "
					+ e.toString());
		}
	}
}
