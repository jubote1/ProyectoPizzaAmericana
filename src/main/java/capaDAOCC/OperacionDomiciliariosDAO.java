package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Lo que hicieron los domiciliarios de UNA tienda en UN dia, para el reporte
 * de operacion de domiciliarios diaria.
 *
 * DE DONDE SALE CADA COSA
 *
 * Es la misma fuente y las mismas definiciones de DesempenoDomiciliarioDAO, que
 * mide a UN domiciliario a lo largo de un rango; aqui se mira a TODOS los de
 * una tienda en un dia. Se leen despacho_real (la salida: hora de salida y de
 * regreso) y despacho_real_det (los pedidos de esa salida con su hora de
 * entrega), cruzados con pedido para la promesa al cliente.
 *
 *  - Dia de HOY: se le pregunta a la tienda en vivo. El datamart nunca trae hoy.
 *  - Dias anteriores: el DATAMART, que se llena de madrugada con el dia anterior
 *    completo y no le pesa a los computadores de las tiendas. Si el datamart no
 *    tiene ese dia de esa tienda, se le pregunta a la tienda.
 *
 * Los ids se repiten entre tiendas en el datamart (despacho_real.id,
 * idpedidotienda...), asi que TODA consulta de alla filtra y cruza por idtienda.
 *
 * LOS TRES TIEMPOS QUE SE MIDEN
 *
 *  - Tiempo total del pedido: de que entro (pedido.fechainsercion) a que se
 *    entrego, contra la promesa (pedido.tiempopedido, 30 o 60 minutos). Ahi
 *    adentro esta la cocina, que no es del domiciliario.
 *  - Tiempo en ruta (min_calle): de que el domiciliario salio a que entrego ESE
 *    pedido. Es el que si es suyo.
 *  - Regreso: de la ULTIMA entrega de la salida a que marco su llegada a la
 *    tienda. El dato viene sucio -regresos sin marcar, de dias, o negativos- y
 *    se clasifica igual que en DesempenoDomiciliarioDAO para que las dos
 *    pantallas no cuenten distinto: el promedio sale solo de los validos.
 *
 * LA JORNADA
 *
 * despacho_real.fecha es la fecha de la jornada, no la del reloj: una salida a
 * las 00:30 pertenece a la jornada del dia anterior. Por eso se filtra por esa
 * columna y nunca por DATE(hora_salida).
 */
public class OperacionDomiciliariosDAO {

	/** Por encima de esto no fue un regreso: fue que no lo marcaron. Igual que en DesempenoDomiciliarioDAO. */
	public static final int MINUTOS_REGRESO_MAXIMO = DesempenoDomiciliarioDAO.MINUTOS_REGRESO_MAXIMO;

	/** A partir de aca el regreso cuenta como demorado. Igual que en DesempenoDomiciliarioDAO. */
	public static final int MINUTOS_REGRESO_ALERTA = DesempenoDomiciliarioDAO.MINUTOS_REGRESO_ALERTA;

	// =======================================================================
	// Lo que se devuelve
	// =======================================================================

	/** Un pedido que salio con un domiciliario. */
	public static class Pedido {
		public int idPedido;
		/** Posicion en la salida, segun lo planeado. */
		public int orden;
		public int prometido;
		/** Hora (HH:mm) en que entro el pedido a la tienda, o "" si no se sabe. */
		public String horaEntrada = "";
		public String horaEntrega = "";
		/** Minutos de que entro a que se entrego; -1 si no hay con que medirlo. */
		public int minutosTotal = -1;
		/** Minutos de que el domiciliario salio a que entrego; -1 si no hay hora de entrega. */
		public int minutosRuta = -1;
		/** Minutos de que entro el pedido a que salio la ruta (cocina, horno, espera); -1 si no se sabe. */
		public int minutosEspera = -1;
		public boolean programado;
		/** Si se puede juzgar contra la promesa. Un programado o uno sin promesa no se juzga. */
		public boolean medible;
		public boolean entregado;
	}

	/** Una salida: el domiciliario sale con uno o varios pedidos y vuelve. */
	public static class Salida {
		public int id;
		public int idDomiciliario;
		public String horaSalida = "";
		/** Hora del reloj (0 a 23) de la salida: la que agrupa la grafica por hora. */
		public int horaDelDia = -1;
		public String horaRegreso = "";
		public String origen = "";
		/** Minutos de la ultima entrega al regreso; solo vale si estadoRegreso es OK. */
		public int minutosRegreso = 0;
		/** OK, SINREGRESO, SINENTREGA, NEGATIVO o SINMARCAR. */
		public String estadoRegreso = "";
		/** De que salio a que entrego el ultimo de sus pedidos; -1 si no entrego ninguno. */
		public int minutosHastaUltima = -1;
		public ArrayList<Pedido> pedidos = new ArrayList<Pedido>();
	}

	/** Lo que se sabe de la presencia de un domiciliario ese dia, por la biometria. */
	public static class Turno {
		public int idDomiciliario;
		/** Hora (HH:mm) del primer ingreso, o "". */
		public String ingreso = "";
		/** Hora (HH:mm) de la ultima salida, o "". */
		public String salida = "";
		/** Minutos entre el primer ingreso y la ultima salida; -1 si falta alguno de los dos. */
		public int minutos = -1;
	}

	/**
	 * Un domiciliario EXTERNO ese dia: la persona de una empresa temporal que
	 * trabajo con un cupo "Domiciliario NN".
	 *
	 * Los externos no tienen usuario propio: entran con un cupo del dia, una
	 * cuenta "Domiciliario 01", "Domiciliario 02"... de la tienda, y es el cupo
	 * -no la persona- lo que queda en despacho_real. Quien fue esa persona, y de
	 * que empresa, se sabe por empleado_temporal_dia (general.empleado_temporal_dia_tienda
	 * en el central). El cupo es el mismo todos los dias y lo usan personas
	 * distintas, asi que la persona se busca SIEMPRE por (tienda, cupo, dia).
	 */
	public static class Externo {
		public int idCupo;
		/** Nombre(s) de la(s) persona(s) que usaron el cupo ese dia, separados por " / ". Puede quedar vacio. */
		public String nombres = "";
		public String empresas = "";
		public String ingreso = "";
		public String salida = "";
		/** Minutos en la tienda, sumando todos los turnos del cupo; -1 si falta algun dato. */
		public int minutos = -1;
	}

	public static class Resultado {
		public int idTienda;
		public String tienda = "";
		public String fecha = "";
		public boolean conecto;
		public String error = "";
		/** TIENDA si se le pregunto en vivo, DATAMART si salio de la replica. */
		public String fuente = "TIENDA";
		/** Solo DATAMART: la ultima fecha que el datamart tiene de esta tienda. */
		public String datosHasta = "";
		public ArrayList<Salida> salidas = new ArrayList<Salida>();
		public LinkedHashMap<Integer, Turno> turnos = new LinkedHashMap<Integer, Turno>();
		/** Los cupos de externos de ese dia, por id del cupo. */
		public LinkedHashMap<Integer, Externo> externos = new LinkedHashMap<Integer, Externo>();
		/** id del empleado -> nombre largo, de todos los que aparecen. */
		public HashMap<Integer, String> nombres = new HashMap<Integer, String>();

		/**
		 * Si ese id es un cupo de externo. Se sabe por el nombre de la cuenta
		 * ("Domiciliario NN"): es la misma marca que usa DesempenoDomiciliarioDAO
		 * para NO tratar esas cuentas como personas.
		 */
		public boolean esExterno(final int idDomiciliario) {
			if (externos.containsKey(Integer.valueOf(idDomiciliario))) {
				return (true);
			}
			final String n = nombres.get(Integer.valueOf(idDomiciliario));
			return (n != null && n.toLowerCase().startsWith("domiciliario "));
		}
	}

	// =======================================================================
	// Una tienda, un dia
	// =======================================================================

	/**
	 * Lo del dia de una tienda. Devuelve siempre un resultado, nunca null: si
	 * ninguna fuente contesta se marca conecto=false y la pantalla lo dice, que
	 * es distinto de decir que ese dia nadie trabajo.
	 *
	 * @param fecha yyyy-MM-dd, fecha de la jornada
	 */
	public static Resultado consultarDia(final int idTienda, final String nombreTienda, final String hosbd,
			final String fecha) {
		final Logger logger = Logger.getLogger("log_file");
		final Resultado res = new Resultado();
		res.idTienda = idTienda;
		res.tienda = texto(nombreTienda);
		res.fecha = fecha;

		final String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		final boolean esPasado = (fecha.compareTo(hoy) < 0);

		//Un dia ya cerrado se lee del datamart: esta completo, es rapido y no
		//molesta al computador de la tienda. Solo si no lo tiene se va a la tienda.
		if (esPasado && leerDatamart(res)) {
			cargarPresencia(res);
			return (res);
		}

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = (hosbd == null || hosbd.trim().length() == 0) ? null : con.obtenerConexionBDTiendaRemota(hosbd);
			if (cn == null) {
				res.conecto = false;
				res.error = esPasado
						? "El datamart no tiene ese dia de esta tienda y su computador no contesto."
						: "No contesto el computador de la tienda. Hoy solo se puede ver con la tienda encendida.";
				return (res);
			}
			res.conecto = true;
			res.fuente = "TIENDA";
			res.salidas = cargarSalidas(cn, idTienda, fecha, false);
			cargarPedidos(cn, idTienda, fecha, false, res.salidas);
		} catch (final Exception e) {
			logger.error("OperacionDomiciliariosDAO.consultarDia tienda " + idTienda + ": " + e.toString());
			res.conecto = false;
			res.error = "Fallo la consulta a la tienda: " + e.toString();
			return (res);
		} finally {
			cerrar(cn);
		}
		cargarPresencia(res);
		return (res);
	}

	/**
	 * Intenta llenar el resultado desde el datamart. Devuelve true si el
	 * datamart tiene salidas de esa tienda ese dia.
	 */
	private static boolean leerDatamart(final Resultado res) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDDatamartLocal();
			if (cn == null) {
				return (false);
			}
			res.datosHasta = DesempenoDomiciliarioDAO.ultimaFechaDatamart(cn, res.idTienda);
			final ArrayList<Salida> salidas = cargarSalidas(cn, res.idTienda, res.fecha, true);
			if (salidas.isEmpty()) {
				//Sin salidas puede ser "ese dia nadie salio" o "el datamart no llega
				//hasta ahi". Solo se da por bueno si el datamart ya cubre la fecha.
				if (res.datosHasta.length() == 0 || res.datosHasta.compareTo(res.fecha) < 0) {
					return (false);
				}
			}
			cargarPedidos(cn, res.idTienda, res.fecha, true, salidas);
			res.salidas = salidas;
			res.conecto = true;
			res.fuente = "DATAMART";
			return (true);
		} catch (final Exception e) {
			logger.error("OperacionDomiciliariosDAO.leerDatamart tienda " + res.idTienda + ": " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	/** Un renglon por salida, con el regreso a la tienda ya clasificado. */
	private static ArrayList<Salida> cargarSalidas(final Connection cn, final int idTienda, final String fecha,
			final boolean datamart) throws Exception {
		final ArrayList<Salida> lista = new ArrayList<Salida>();
		//Los minutos los calcula MySQL: traer las fechas y restarlas en Java
		//obligaria a parsear, y el formato de fecha ya ha dado guerra.
		final String sql = "select r.id, r.id_domiciliario, date_format(r.hora_salida, '%H:%i') as hs, "
				+ "hour(r.hora_salida) as h, date_format(r.hora_regreso, '%H:%i') as hr, "
				+ "(r.hora_regreso is null) as sin_regreso, ifnull(r.origen, '') as origen, "
				+ "timestampdiff(minute, max(d.hora_entrega), r.hora_regreso) as min_regreso, "
				+ "timestampdiff(minute, r.hora_salida, max(d.hora_entrega)) as min_hasta_ultima "
				+ "from despacho_real r "
				+ "left join despacho_real_det d on d.despacho_real_id = r.id"
				+ (datamart ? " and d.idtienda = r.idtienda " : " ")
				+ "where " + (datamart ? "r.idtienda = ? and " : "") + "r.fecha = ? "
				+ "group by r.id, r.id_domiciliario, r.hora_salida, r.hora_regreso, r.origen "
				+ "order by r.hora_salida, r.id";
		final PreparedStatement ps = cn.prepareStatement(sql);
		int k = 1;
		if (datamart) {
			ps.setInt(k++, idTienda);
		}
		ps.setString(k++, fecha);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final Salida s = new Salida();
			s.id = rs.getInt("id");
			s.idDomiciliario = rs.getInt("id_domiciliario");
			s.horaSalida = texto(rs.getString("hs"));
			s.horaDelDia = rs.getInt("h");
			if (rs.wasNull()) {
				s.horaDelDia = -1;
			}
			s.horaRegreso = texto(rs.getString("hr"));
			s.origen = texto(rs.getString("origen"));
			final boolean sinRegreso = rs.getBoolean("sin_regreso");
			final int minutos = rs.getInt("min_regreso");
			final boolean nulo = rs.wasNull();
			s.minutosRegreso = minutos;
			//Misma clasificacion de DesempenoDomiciliarioDAO.cargarSalidas.
			if (sinRegreso) {
				s.estadoRegreso = "SINREGRESO";
			} else if (nulo) {
				s.estadoRegreso = "SINENTREGA";
			} else if (minutos < 0) {
				s.estadoRegreso = "NEGATIVO";
			} else if (minutos > MINUTOS_REGRESO_MAXIMO) {
				s.estadoRegreso = "SINMARCAR";
			} else {
				s.estadoRegreso = "OK";
			}
			final int hastaUltima = rs.getInt("min_hasta_ultima");
			s.minutosHastaUltima = rs.wasNull() ? -1 : hastaUltima;
			lista.add(s);
		}
		rs.close();
		ps.close();
		return (lista);
	}

	/** Los pedidos de cada salida, con sus tres tiempos. */
	private static void cargarPedidos(final Connection cn, final int idTienda, final String fecha,
			final boolean datamart, final ArrayList<Salida> salidas) throws Exception {
		final HashMap<Integer, Salida> porId = new HashMap<Integer, Salida>();
		for (final Salida s : salidas) {
			porId.put(Integer.valueOf(s.id), s);
		}
		final String sql = "select d.despacho_real_id, d.id_pedido, d.orden_planificada, "
				+ "date_format(p.fechainsercion, '%H:%i') as hora_entrada, "
				+ "date_format(d.hora_entrega, '%H:%i') as hora_entrega, "
				+ "(d.hora_entrega is null) as sin_entrega, "
				+ "ifnull(p.tiempopedido, 0) as prometido, ifnull(p.programado, 'N') as programado, "
				+ "timestampdiff(minute, p.fechainsercion, d.hora_entrega) as min_total, "
				+ "timestampdiff(minute, r.hora_salida, d.hora_entrega) as min_ruta, "
				+ "timestampdiff(minute, p.fechainsercion, r.hora_salida) as min_espera "
				+ "from despacho_real r "
				+ "inner join despacho_real_det d on d.despacho_real_id = r.id"
				+ (datamart ? " and d.idtienda = r.idtienda " : " ")
				+ "left join pedido p on p.idpedidotienda = d.id_pedido"
				+ (datamart ? " and p.idtienda = r.idtienda " : " ")
				+ "where " + (datamart ? "r.idtienda = ? and " : "") + "r.fecha = ? "
				+ "order by r.hora_salida, r.id, d.orden_planificada";
		final PreparedStatement ps = cn.prepareStatement(sql);
		int k = 1;
		if (datamart) {
			ps.setInt(k++, idTienda);
		}
		ps.setString(k++, fecha);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final Salida s = porId.get(Integer.valueOf(rs.getInt("despacho_real_id")));
			if (s == null) {
				continue;
			}
			final Pedido p = new Pedido();
			p.idPedido = rs.getInt("id_pedido");
			p.orden = rs.getInt("orden_planificada");
			p.prometido = rs.getInt("prometido");
			p.horaEntrada = texto(rs.getString("hora_entrada"));
			p.horaEntrega = texto(rs.getString("hora_entrega"));
			p.entregado = !rs.getBoolean("sin_entrega");
			final int total = rs.getInt("min_total");
			final boolean sinTotal = rs.wasNull();
			p.minutosTotal = sinTotal ? -1 : total;
			final int ruta = rs.getInt("min_ruta");
			final boolean sinRuta = rs.wasNull();
			//Un tiempo negativo es un dato malo -hora de entrega anterior a la de
			//salida-, no una entrega instantanea: se deja como "sin dato".
			p.minutosRuta = (sinRuta || ruta < 0) ? -1 : ruta;
			final int espera = rs.getInt("min_espera");
			final boolean sinEspera = rs.wasNull();
			p.minutosEspera = (sinEspera || espera < 0) ? -1 : espera;
			p.programado = "S".equals(rs.getString("programado"));
			//Mismo criterio de DesempenoDomiciliarioDAO.cargarEntregas: un
			//programado nunca se juzga por tiempo, y sin promesa o con tiempo
			//negativo no hay contra que medir.
			p.medible = (p.prometido > 0 && !sinTotal && p.minutosTotal >= 0 && !p.programado && p.entregado);
			s.pedidos.add(p);
		}
		rs.close();
		ps.close();
	}

	// =======================================================================
	// Presencia y nombres (siempre del central)
	// =======================================================================

	/**
	 * Quien marco ingreso en la tienda ese dia, y los nombres de todos.
	 *
	 * La presencia sale de general.empleado_evento, que el central tiene al dia
	 * -el POS escribe la biometria directo ahi-. Se toman SOLO los que son
	 * domiciliarios (tipo_empleado.es_domiciliario, en cualquiera de sus dos tipos)
	 * y se descartan las cuentas comodin "Domiciliario NN", que no son personas.
	 * Sirve para dos cosas: decir cuantas horas estuvo cada uno en la tienda y
	 * mostrar al que vino y no llevo ningun pedido, que no deja rastro en
	 * despacho_real.
	 *
	 * Esto NUNCA tumba el reporte: si falla, se sigue sin presencia.
	 */
	private static void cargarPresencia(final Resultado res) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDGeneral();
			if (cn == null) {
				return;
			}
			final String sql = "select ev.id, ev.tipo_evento, date_format(ev.fecha_hora_log, '%H:%i') as hora, "
					+ "timestampdiff(minute, '" + res.fecha + " 00:00:00', ev.fecha_hora_log) as minuto_del_dia, "
					+ "e.nombre_largo "
					+ "from empleado_evento ev "
					+ "inner join empleado e on e.id = ev.id "
					+ "where ev.idtienda = ? and ev.fecha = ? "
					+ "and e.nombre not like 'Domiciliario %' and e.nombre_largo not like 'Domiciliario %' "
					+ "and exists (select 1 from tipo_empleado t where t.es_domiciliario = '1' "
					+ "and t.idtipoempleado in (e.idtipoempleado, e.idtipoempleado2)) "
					+ "order by ev.id, ev.fecha_hora_log";
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setInt(1, res.idTienda);
			ps.setString(2, res.fecha);
			final ResultSet rs = ps.executeQuery();
			final HashMap<Integer, Integer> minIngreso = new HashMap<Integer, Integer>();
			final HashMap<Integer, Integer> minSalida = new HashMap<Integer, Integer>();
			while (rs.next()) {
				final Integer id = Integer.valueOf(rs.getInt("id"));
				Turno t = res.turnos.get(id);
				if (t == null) {
					t = new Turno();
					t.idDomiciliario = id.intValue();
					res.turnos.put(id, t);
				}
				res.nombres.put(id, texto(rs.getString("nombre_largo")));
				final String tipo = texto(rs.getString("tipo_evento"));
				final String hora = texto(rs.getString("hora"));
				final int minuto = rs.getInt("minuto_del_dia");
				if ("INGRESO".equals(tipo)) {
					//El primer ingreso del dia.
					if (t.ingreso.length() == 0) {
						t.ingreso = hora;
						minIngreso.put(id, Integer.valueOf(minuto));
					}
				} else if ("SALIDA".equals(tipo)) {
					//La ultima salida del dia.
					t.salida = hora;
					minSalida.put(id, Integer.valueOf(minuto));
				}
			}
			rs.close();
			ps.close();
			for (final Map.Entry<Integer, Turno> e : res.turnos.entrySet()) {
				final Turno t = e.getValue();
				final Integer a = minIngreso.get(e.getKey());
				final Integer b = minSalida.get(e.getKey());
				//Una salida anterior al ingreso es el cierre de la jornada pasada que
				//cae en este dia, no horas trabajadas: se deja sin cifra.
				if (a != null && b != null && b.intValue() >= a.intValue()) {
					t.minutos = b.intValue() - a.intValue();
				}
			}

			//Los nombres de quienes salieron a repartir y no marcaron ingreso aqui.
			final ArrayList<Integer> faltantes = new ArrayList<Integer>();
			for (final Salida s : res.salidas) {
				final Integer id = Integer.valueOf(s.idDomiciliario);
				if (!res.nombres.containsKey(id) && !faltantes.contains(id)) {
					faltantes.add(id);
				}
			}
			if (!faltantes.isEmpty()) {
				final StringBuilder lista = new StringBuilder();
				for (final Integer id : faltantes) {
					if (lista.length() > 0) {
						lista.append(",");
					}
					lista.append(id.intValue());
				}
				final PreparedStatement psN = cn.prepareStatement(
						"select id, nombre_largo from empleado where id in (" + lista + ")");
				final ResultSet rsN = psN.executeQuery();
				while (rsN.next()) {
					res.nombres.put(Integer.valueOf(rsN.getInt("id")), texto(rsN.getString("nombre_largo")));
				}
				rsN.close();
				psN.close();
			}
			cargarExternos(cn, res);
		} catch (final Exception e) {
			logger.error("OperacionDomiciliariosDAO.cargarPresencia tienda " + res.idTienda + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Los domiciliarios externos de ese dia: quien fue la persona detras de
	 * cada cupo "Domiciliario NN" y de que empresa.
	 *
	 * Se lee primero general.empleado_temporal_dia_tienda, donde el POS avisa
	 * cada ingreso y salida, y para los cupos que ahi no aparecen -una tienda con
	 * el POS sin actualizar- se intenta datamart.empleado_temporal_dia, que llega
	 * hasta unos dias atras. Un cupo que salio a repartir y no se encuentra en
	 * ninguna queda como externo SIN nombre: es externo igual, solo que no se
	 * sabe quien. Salen tambien los externos que vinieron y no llevaron ningun
	 * pedido.
	 *
	 * Nunca tumba el reporte.
	 */
	private static void cargarExternos(final Connection cnGeneral, final Resultado res) {
		final Logger logger = Logger.getLogger("log_file");
		try {
			cargarExternosDe(cnGeneral, res,
					"select id, nombre, empresa, left(horaingreso, 5) as hi, left(horasalida, 5) as hs, "
							+ "case when horasalida is null or horasalida = '' or horaingreso is null or horaingreso = '' "
							+ "then null else mod(time_to_sec(horasalida) - time_to_sec(horaingreso) + 86400, 86400) div 60 end as minutos "
							+ "from empleado_temporal_dia_tienda "
							+ "where idtienda = ? and fecha_sistema = ? and ifnull(anulado, 'N') <> 'S' order by id, horaingreso",
					res.idTienda, res.fecha);
		} catch (final Exception e) {
			logger.error("OperacionDomiciliariosDAO.cargarExternos (central) tienda " + res.idTienda + ": " + e.toString());
		}

		//Los cupos que salieron a repartir y el central no conoce.
		boolean faltaAlguno = false;
		for (final Salida s : res.salidas) {
			if (res.esExterno(s.idDomiciliario) && !res.externos.containsKey(Integer.valueOf(s.idDomiciliario))) {
				faltaAlguno = true;
				break;
			}
		}
		if (faltaAlguno) {
			Connection cnDm = null;
			try {
				cnDm = new ConexionBaseDatos().obtenerConexionBDDatamartLocal();
				if (cnDm != null) {
					cargarExternosDe(cnDm, res,
							"select id, nombre, empresa, left(horaingreso, 5) as hi, left(horasalida, 5) as hs, "
									+ "case when horasalida is null or horasalida = '' or horaingreso is null or horaingreso = '' "
									+ "then null else mod(time_to_sec(horasalida) - time_to_sec(horaingreso) + 86400, 86400) div 60 end as minutos "
									+ "from empleado_temporal_dia where idtienda = ? and fecha_sistema = ? order by id, horaingreso",
							res.idTienda, res.fecha);
				}
			} catch (final Exception e) {
				logger.error("OperacionDomiciliariosDAO.cargarExternos (datamart) tienda " + res.idTienda + ": "
						+ e.toString());
			} finally {
				cerrar(cnDm);
			}
		}

		//Lo que sigue sin conocerse se anota como externo sin nombre.
		for (final Salida s : res.salidas) {
			final Integer id = Integer.valueOf(s.idDomiciliario);
			if (res.esExterno(s.idDomiciliario) && !res.externos.containsKey(id)) {
				final Externo e = new Externo();
				e.idCupo = s.idDomiciliario;
				res.externos.put(id, e);
			}
		}
	}

	/**
	 * Lee los turnos de externos con la consulta dada y los suma por cupo. Los
	 * cupos que YA estan en el resultado no se tocan: lo que dijo la primera
	 * fuente manda.
	 */
	private static void cargarExternosDe(final Connection cn, final Resultado res, final String sql,
			final int idTienda, final String fecha) throws Exception {
		final PreparedStatement ps = cn.prepareStatement(sql);
		ps.setInt(1, idTienda);
		ps.setString(2, fecha);
		final ResultSet rs = ps.executeQuery();
		final LinkedHashMap<Integer, Externo> nuevos = new LinkedHashMap<Integer, Externo>();
		final java.util.HashSet<Integer> conTurnoAbierto = new java.util.HashSet<Integer>();
		while (rs.next()) {
			final Integer id = Integer.valueOf(rs.getInt("id"));
			if (res.externos.containsKey(id)) {
				continue;
			}
			Externo e = nuevos.get(id);
			if (e == null) {
				e = new Externo();
				e.idCupo = id.intValue();
				e.minutos = 0;
				nuevos.put(id, e);
			}
			final String nombre = texto(rs.getString("nombre")).trim();
			if (nombre.length() > 0 && !contiene(e.nombres, nombre)) {
				e.nombres = (e.nombres.length() == 0 ? nombre : e.nombres + " / " + nombre);
			}
			final String empresa = texto(rs.getString("empresa")).trim();
			if (empresa.length() > 0 && !contiene(e.empresas, empresa)) {
				e.empresas = (e.empresas.length() == 0 ? empresa : e.empresas + " / " + empresa);
			}
			final String hi = texto(rs.getString("hi"));
			final String hs = texto(rs.getString("hs"));
			if (e.ingreso.length() == 0) {
				e.ingreso = hi;
			}
			if (hs.length() > 0) {
				e.salida = hs;
			}
			final int minutos = rs.getInt("minutos");
			if (rs.wasNull()) {
				//Un turno sin salida o sin ingreso: no se inventa la hora.
				conTurnoAbierto.add(id);
			} else if (e.minutos >= 0) {
				e.minutos = e.minutos + minutos;
			}
		}
		rs.close();
		ps.close();
		for (final Map.Entry<Integer, Externo> e : nuevos.entrySet()) {
			if (conTurnoAbierto.contains(e.getKey())) {
				e.getValue().minutos = -1;
			}
			res.externos.put(e.getKey(), e.getValue());
		}
	}

	private static boolean contiene(final String lista, final String valor) {
		return (lista.toLowerCase().contains(valor.toLowerCase()));
	}

	// =======================================================================
	// Auxiliares
	// =======================================================================

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static void cerrar(final Connection cn) {
		if (cn != null) {
			try {
				cn.close();
			} catch (final Exception e) {
				//Nada que hacer si no cierra.
			}
		}
	}
}
