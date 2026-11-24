package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import conexionCC.ConexionBaseDatos;

/**
 * Las consultas del tablero del plan de puntos (Puntos Pizza).
 *
 * DE DONDE SALE CADA NUMERO
 *
 * El LIBRO es pizzaamericana.fidelizacion_transaccion: una fila por pedido que
 * acumulo puntos. De ahi salen lo asignado (puntos), lo redimido
 * (puntos_redimidos), lo vencido (puntos_vencidos) y lo VIGENTE, que es
 * puntos - puntos_redimidos de las filas con vencidos = 'N'.
 *
 * Existe OTRO saldo, cliente_fidelizacion.puntos_vigentes, que es un acumulado
 * que nada reconcilia con el libro (ver memoria pos-bases-de-datos). Aqui manda
 * el libro, y el tablero muestra los dos para que la diferencia no sea un
 * secreto.
 *
 * LAS REDENCIONES salen de fidelizacion_redencion, contando solo las
 * CONFIRMADA. Las REVERSADA son redenciones que se devolvieron: se muestran
 * aparte, nunca sumadas.
 *
 * LOS EMPLEADOS. fidelizacion_*.usuario no siempre es una persona: tambien
 * guarda canales (CONTACT, BOT, APP...). Los rankings de empleados los dejan
 * por fuera, igual que la auditoria, o el contact center saldria de primero.
 * Y esa columna solo se llena desde septiembre de 2026.
 *
 * El acceso lo controla el servlet (mismos roles que la auditoria): este DAO
 * no pregunta por permisos.
 */
public class TableroPuntosDAO {

	/** Los usuarios que son un canal y no una persona. Mismos que la auditoria. */
	private static final String CANALES =
			" ('CONTACT','TIEVIRTUALKUNO','BOT','APP','CAJA','AJUSTE') ";

	/** Tope de filas que devuelve cualquier ranking o lista. */
	public static final int TOPE_FILAS = 500;

	// =======================================================================
	// Auxiliares
	// =======================================================================

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			// nada que hacer
		}
	}

	/** Un ResultSet entero como arreglo de objetos, con el nombre de cada columna como llave. */
	@SuppressWarnings("unchecked")
	private static JSONArray filas(final ResultSet rs) throws Exception {
		final JSONArray lista = new JSONArray();
		final ResultSetMetaData md = rs.getMetaData();
		final int n = md.getColumnCount();
		while (rs.next()) {
			final JSONObject o = new JSONObject();
			for (int i = 1; i <= n; i++) {
				final String col = md.getColumnLabel(i);
				final Object v = rs.getObject(i);
				if (v == null) {
					o.put(col, "");
				} else if (v instanceof java.math.BigDecimal) {
					o.put(col, Double.valueOf(((java.math.BigDecimal) v).doubleValue()));
				} else if (v instanceof java.math.BigInteger) {
					o.put(col, Long.valueOf(((java.math.BigInteger) v).longValue()));
				} else if (v instanceof Number || v instanceof Boolean) {
					o.put(col, v);
				} else {
					o.put(col, String.valueOf(v));
				}
			}
			lista.add(o);
		}
		return (lista);
	}

	/** La primera (y unica) fila, o un objeto vacio. */
	private static JSONObject unaFila(final ResultSet rs) throws Exception {
		final JSONArray a = filas(rs);
		return (a.isEmpty() ? new JSONObject() : (JSONObject) a.get(0));
	}

	private static JSONArray consulta(final Connection cn, final String sql, final Object... p) throws Exception {
		final PreparedStatement ps = cn.prepareStatement(sql);
		try {
			for (int i = 0; i < p.length; i++) {
				if (p[i] instanceof Integer) {
					ps.setInt(i + 1, ((Integer) p[i]).intValue());
				} else {
					ps.setString(i + 1, String.valueOf(p[i]));
				}
			}
			final ResultSet rs = ps.executeQuery();
			try {
				return (filas(rs));
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
	}

	private static JSONObject consultaUna(final Connection cn, final String sql, final Object... p) throws Exception {
		final JSONArray a = consulta(cn, sql, p);
		return (a.isEmpty() ? new JSONObject() : (JSONObject) a.get(0));
	}

	/** Nombre de cada tienda por id. */
	private static Map<Integer, String> nombresTienda(final Connection cn) throws Exception {
		final Map<Integer, String> m = new HashMap<Integer, String>();
		final JSONArray a = consulta(cn, "SELECT idtienda, nombre FROM pizzaamericana.tienda");
		for (int i = 0; i < a.size(); i++) {
			final JSONObject o = (JSONObject) a.get(i);
			m.put(Integer.valueOf(((Number) o.get("idtienda")).intValue()), String.valueOf(o.get("nombre")));
		}
		return (m);
	}

	// =======================================================================
	// El resumen: tarjetas, series y distribuciones
	// =======================================================================

	/**
	 * Todo lo que muestra la primera pestana.
	 *
	 * @param diasActivo un cliente es "activo" si acumulo puntos en los ultimos N dias
	 * @param desde      rango de redenciones y movimiento por tienda (aaaa-mm-dd)
	 * @param hasta      idem, inclusive
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject resumen(final int diasActivo, final String desde, final String hasta) {
		final JSONObject r = new JSONObject();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();

			// 1. Afiliados ------------------------------------------------
			r.put("afiliados", consultaUna(cn,
					"SELECT COUNT(*) AS total, IFNULL(SUM(activo = 'S'),0) AS activos,"
					+ " IFNULL(SUM(fecha_vinculacion >= DATE_SUB(NOW(), INTERVAL 7 DAY)),0) AS nuevos7,"
					+ " IFNULL(SUM(fecha_vinculacion >= DATE_SUB(NOW(), INTERVAL 30 DAY)),0) AS nuevos30,"
					+ " IFNULL(SUM(puntos_vigentes),0) AS saldo_cliente"
					+ " FROM pizzaamericana.cliente_fidelizacion"));

			// 2. El libro -------------------------------------------------
			r.put("libro", consultaUna(cn,
					"SELECT COUNT(*) AS filas, COUNT(DISTINCT correo) AS clientes,"
					+ " IFNULL(SUM(puntos),0) AS asignados,"
					+ " IFNULL(SUM(puntos_redimidos),0) AS redimidos,"
					+ " IFNULL(SUM(puntos_vencidos),0) AS vencidos,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' THEN puntos - puntos_redimidos ELSE 0 END),0) AS vigentes,"
					+ " COUNT(DISTINCT CASE WHEN fecha_transaccion >= DATE_SUB(CURDATE(), INTERVAL ? DAY) THEN correo END) AS activos,"
					+ " COUNT(DISTINCT CASE WHEN vencidos = 'N' AND puntos > puntos_redimidos THEN correo END) AS con_saldo,"
					+ " IFNULL(SUM(CASE WHEN vencidos = 'N' AND correo NOT LIKE '%@%' THEN puntos - puntos_redimidos ELSE 0 END),0) AS vigentes_invalidos,"
					+ " COUNT(DISTINCT CASE WHEN correo NOT LIKE '%@%' THEN correo END) AS correos_invalidos,"
					+ " MIN(fecha_transaccion) AS desde"
					+ " FROM pizzaamericana.fidelizacion_transaccion",
					Integer.valueOf(diasActivo)));

			// 3. Por vencer ----------------------------------------------
			r.put("por_vencer", consultaUna(cn,
					"SELECT"
					+ " IFNULL(SUM(CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 15 DAY) THEN puntos - puntos_redimidos END),0) AS p15,"
					+ " COUNT(DISTINCT CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 15 DAY) THEN correo END) AS c15,"
					+ " IFNULL(SUM(CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN puntos - puntos_redimidos END),0) AS p30,"
					+ " COUNT(DISTINCT CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN correo END) AS c30,"
					+ " IFNULL(SUM(CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY) THEN puntos - puntos_redimidos END),0) AS p60,"
					+ " COUNT(DISTINCT CASE WHEN fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY) THEN correo END) AS c60,"
					// De lo anterior, lo que esta en correos que no son correos ("no lo da", "no tiene"):
					// puntos que nadie va a poder avisar ni redimir.
					+ " IFNULL(SUM(CASE WHEN correo NOT LIKE '%@%' AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 15 DAY) THEN puntos - puntos_redimidos END),0) AS p15inv,"
					+ " IFNULL(SUM(CASE WHEN correo NOT LIKE '%@%' AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY) THEN puntos - puntos_redimidos END),0) AS p60inv,"
					// Fecha ya pasada y sin marcar como vencida: dato por revisar, no un aviso.
					+ " IFNULL(SUM(CASE WHEN fecha_vencimiento < CURDATE() THEN puntos - puntos_redimidos END),0) AS pasados,"
					+ " COUNT(DISTINCT CASE WHEN fecha_vencimiento < CURDATE() THEN correo END) AS cpasados"
					+ " FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE vencidos = 'N' AND puntos > puntos_redimidos"));

			// 4. Redenciones del rango y de toda la historia --------------
			r.put("redenciones_rango", consulta(cn,
					"SELECT estado, COUNT(*) AS n, IFNULL(SUM(puntos_redimidos),0) AS puntos,"
					+ " COUNT(DISTINCT correo) AS clientes"
					+ " FROM pizzaamericana.fidelizacion_redencion"
					+ " WHERE fecha_redencion >= ? AND fecha_redencion < DATE_ADD(?, INTERVAL 1 DAY)"
					+ " GROUP BY estado", desde, hasta));
			r.put("redenciones_total", consulta(cn,
					"SELECT estado, COUNT(*) AS n, IFNULL(SUM(puntos_redimidos),0) AS puntos,"
					+ " COUNT(DISTINCT correo) AS clientes, MIN(fecha_redencion) AS desde"
					+ " FROM pizzaamericana.fidelizacion_redencion GROUP BY estado"));

			// 5. Series de los ultimos 12 meses ---------------------------
			final String inicioMeses = "DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 11 MONTH), '%Y-%m-01')";
			final JSONArray asignados = consulta(cn,
					"SELECT DATE_FORMAT(fecha_transaccion, '%Y-%m') AS mes, SUM(puntos) AS v, COUNT(DISTINCT correo) AS c"
					+ " FROM pizzaamericana.fidelizacion_transaccion WHERE fecha_transaccion >= " + inicioMeses
					+ " GROUP BY mes");
			final JSONArray redimidos = consulta(cn,
					"SELECT DATE_FORMAT(fecha_redencion, '%Y-%m') AS mes, SUM(puntos_redimidos) AS v, COUNT(*) AS c"
					+ " FROM pizzaamericana.fidelizacion_redencion WHERE estado = 'CONFIRMADA'"
					+ " AND fecha_redencion >= " + inicioMeses + " GROUP BY mes");
			final JSONArray vencidos = consulta(cn,
					"SELECT DATE_FORMAT(fecha_vencimiento, '%Y-%m') AS mes, SUM(puntos_vencidos) AS v"
					+ " FROM pizzaamericana.fidelizacion_transaccion WHERE vencidos = 'S'"
					+ " AND fecha_vencimiento >= " + inicioMeses + " GROUP BY mes");
			final JSONArray afiliaciones = consulta(cn,
					"SELECT DATE_FORMAT(fecha_vinculacion, '%Y-%m') AS mes, COUNT(*) AS v"
					+ " FROM pizzaamericana.cliente_fidelizacion WHERE fecha_vinculacion >= " + inicioMeses
					+ " GROUP BY mes");
			final JSONArray meses = consulta(cn,
					"SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n MONTH), '%Y-%m') AS mes FROM"
					+ " (SELECT 11 AS n UNION SELECT 10 UNION SELECT 9 UNION SELECT 8 UNION SELECT 7 UNION SELECT 6"
					+ "  UNION SELECT 5 UNION SELECT 4 UNION SELECT 3 UNION SELECT 2 UNION SELECT 1 UNION SELECT 0) x"
					+ " ORDER BY mes");
			final JSONArray serie = new JSONArray();
			for (int i = 0; i < meses.size(); i++) {
				final String mes = String.valueOf(((JSONObject) meses.get(i)).get("mes"));
				final JSONObject o = new JSONObject();
				o.put("mes", mes);
				o.put("asignados", valor(asignados, mes, "v"));
				o.put("clientes", valor(asignados, mes, "c"));
				o.put("redimidos", valor(redimidos, mes, "v"));
				o.put("redenciones", valor(redimidos, mes, "c"));
				o.put("vencidos", valor(vencidos, mes, "v"));
				o.put("afiliaciones", valor(afiliaciones, mes, "v"));
				serie.add(o);
			}
			r.put("serie", serie);

			// 6. Vencimientos proximos por tramo ---------------------------
			r.put("vencimientos", consulta(cn,
					"SELECT CASE WHEN fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 15 DAY) THEN '1' "
					+ "          WHEN fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN '2' "
					+ "          WHEN fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL 45 DAY) THEN '3' ELSE '4' END AS tramo,"
					+ " SUM(puntos - puntos_redimidos) AS puntos, COUNT(DISTINCT correo) AS clientes"
					+ " FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE vencidos = 'N' AND puntos > puntos_redimidos"
					+ " AND fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY)"
					+ " GROUP BY tramo ORDER BY tramo"));

			// 7. Como se reparte el saldo (los mismos rangos de las plantillas de correo)
			r.put("saldos", consulta(cn,
					"SELECT CASE WHEN s < 100 THEN '1' WHEN s < 150 THEN '2' WHEN s < 200 THEN '3'"
					+ "          WHEN s < 250 THEN '4' WHEN s < 300 THEN '5' ELSE '6' END AS rango,"
					+ " COUNT(*) AS clientes, SUM(s) AS puntos"
					+ " FROM (SELECT correo, SUM(puntos - puntos_redimidos) AS s"
					+ "         FROM pizzaamericana.fidelizacion_transaccion"
					+ "        WHERE vencidos = 'N' AND puntos > puntos_redimidos GROUP BY correo) x"
					+ " GROUP BY rango ORDER BY rango"));

			// 8. Por tienda: lo que acumulan y lo que redimen en el rango --
			final Map<Integer, String> tiendas = nombresTienda(cn);
			final JSONArray acum = consulta(cn,
					"SELECT idtienda, COUNT(*) AS acumulaciones, COUNT(DISTINCT correo) AS clientes,"
					+ " IFNULL(SUM(puntos),0) AS puntos, IFNULL(SUM(valor_neto),0) AS venta"
					+ " FROM pizzaamericana.fidelizacion_transaccion"
					+ " WHERE fecha_transaccion BETWEEN ? AND ? GROUP BY idtienda", desde, hasta);
			final JSONArray redi = consulta(cn,
					"SELECT idtienda, COUNT(*) AS redenciones, IFNULL(SUM(puntos_redimidos),0) AS puntos"
					+ " FROM pizzaamericana.fidelizacion_redencion WHERE estado = 'CONFIRMADA'"
					+ " AND fecha_redencion >= ? AND fecha_redencion < DATE_ADD(?, INTERVAL 1 DAY)"
					+ " GROUP BY idtienda", desde, hasta);
			final JSONArray afil = consulta(cn,
					"SELECT canal, COUNT(*) AS n FROM pizzaamericana.cliente_fidelizacion GROUP BY canal");
			final java.util.TreeSet<Integer> ids = new java.util.TreeSet<Integer>();
			for (int i = 0; i < acum.size(); i++) {
				ids.add(Integer.valueOf(((Number) ((JSONObject) acum.get(i)).get("idtienda")).intValue()));
			}
			for (int i = 0; i < redi.size(); i++) {
				ids.add(Integer.valueOf(((Number) ((JSONObject) redi.get(i)).get("idtienda")).intValue()));
			}
			final JSONArray porTienda = new JSONArray();
			for (final Integer id : ids) {
				final JSONObject o = new JSONObject();
				o.put("idtienda", id);
				o.put("tienda", id.intValue() == 0 ? "Sin tienda (ajustes manuales)"
						: (tiendas.containsKey(id) ? tiendas.get(id) : ("Tienda " + id)));
				final JSONObject a = buscar(acum, "idtienda", id);
				final JSONObject d = buscar(redi, "idtienda", id);
				o.put("acumulaciones", a.get("acumulaciones") == null ? Long.valueOf(0) : a.get("acumulaciones"));
				o.put("clientes", a.get("clientes") == null ? Long.valueOf(0) : a.get("clientes"));
				o.put("puntos", a.get("puntos") == null ? Double.valueOf(0) : a.get("puntos"));
				o.put("venta", a.get("venta") == null ? Double.valueOf(0) : a.get("venta"));
				o.put("redenciones", d.get("redenciones") == null ? Long.valueOf(0) : d.get("redenciones"));
				o.put("puntos_redimidos", d.get("puntos") == null ? Double.valueOf(0) : d.get("puntos"));
				// Afiliados cuyo alta se hizo en esa tienda (canal 'TIENDA<id>').
				final JSONObject f = buscar(afil, "canal", "TIENDA" + id);
				o.put("afiliados", f.get("n") == null ? Long.valueOf(0) : f.get("n"));
				porTienda.add(o);
			}
			r.put("por_tienda", porTienda);

			// Como se afilian: tienda, contact center, web...
			final JSONArray canales = new JSONArray();
			long tiendaTotal = 0;
			long sinDato = 0;
			for (int i = 0; i < afil.size(); i++) {
				final JSONObject o = (JSONObject) afil.get(i);
				String c = String.valueOf(o.get("canal")).trim();
				if (c.equalsIgnoreCase("null")) {
					c = "";
				}
				final long n = ((Number) o.get("n")).longValue();
				if (c.toUpperCase().startsWith("TIENDA")) {
					tiendaTotal += n;
				} else if (c.length() == 0) {
					sinDato += n;
				} else {
					final JSONObject x = new JSONObject();
					x.put("canal", c);
					x.put("n", Long.valueOf(n));
					canales.add(x);
				}
			}
			if (sinDato > 0) {
				final JSONObject x = new JSONObject();
				x.put("canal", "SIN DATO");
				x.put("n", Long.valueOf(sinDato));
				canales.add(x);
			}
			final JSONObject t = new JSONObject();
			t.put("canal", "TIENDA (todas)");
			t.put("n", Long.valueOf(tiendaTotal));
			canales.add(0, t);
			r.put("afiliacion_canal", canales);

			// Las tiendas que se pueden escoger en los filtros.
			r.put("tiendas_lista", consulta(cn,
					"SELECT idtienda, nombre FROM pizzaamericana.tienda WHERE funcional = 'S' ORDER BY nombre"));

			r.put("resultado", "OK");
		} catch (final Exception e) {
			logger.error("TableroPuntosDAO.resumen: " + e.toString(), e);
			r.put("resultado", "ERROR");
			r.put("mensaje", "No se pudo armar el resumen: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	private static Object valor(final JSONArray a, final String mes, final String col) {
		for (int i = 0; i < a.size(); i++) {
			final JSONObject o = (JSONObject) a.get(i);
			if (mes.equals(String.valueOf(o.get("mes")))) {
				return (o.get(col));
			}
		}
		return (Long.valueOf(0));
	}

	private static JSONObject buscar(final JSONArray a, final String col, final Object clave) {
		for (int i = 0; i < a.size(); i++) {
			final JSONObject o = (JSONObject) a.get(i);
			final Object v = o.get(col);
			if (v != null && clave != null && String.valueOf(v).equals(String.valueOf(clave))) {
				return (o);
			}
		}
		return (new JSONObject());
	}

	// =======================================================================
	// Rankings y consultas
	// =======================================================================

	/**
	 * Un ranking.
	 *
	 * @param tipo CLIENTES_SALDO, CLIENTES_REDIMEN, EMPLEADOS_REDIMEN, EMPLEADOS_ACUMULAN
	 */
	public static JSONObject ranking(final String tipo, final String desde, final String hasta,
			final int idTienda, final int limite) {
		final JSONObject r = new JSONObject();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final int tope = Math.max(1, Math.min(limite, TOPE_FILAS));
		try {
			cn = con.obtenerConexionBDPrincipal();
			final Map<Integer, String> tiendas = nombresTienda(cn);
			JSONArray filas;
			final String filtroTiendaT = idTienda > 0 ? " AND t.idtienda = " + idTienda : "";
			final String filtroTiendaR = idTienda > 0 ? " AND r.idtienda = " + idTienda : "";

			if ("CLIENTES_SALDO".equals(tipo)) {
				// Quien tiene mas puntos vivos hoy. El rango de fechas no aplica: es el saldo de ahora.
				filas = consulta(cn,
						"SELECT t.correo, SUM(t.puntos - t.puntos_redimidos) AS saldo,"
						+ " SUM(CASE WHEN t.fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY)"
						+ "          THEN t.puntos - t.puntos_redimidos ELSE 0 END) AS por_vencer60,"
						+ " COUNT(*) AS acumulaciones, MAX(t.fecha_transaccion) AS ultima, COUNT(DISTINCT t.idtienda) AS tiendas"
						+ " FROM pizzaamericana.fidelizacion_transaccion t"
						+ " WHERE t.vencidos = 'N' AND t.puntos > t.puntos_redimidos AND t.correo LIKE '%@%'" + filtroTiendaT
						+ " GROUP BY t.correo ORDER BY saldo DESC LIMIT " + tope);
			} else if ("CLIENTES_REDIMEN".equals(tipo)) {
				filas = consulta(cn,
						"SELECT r.correo, COUNT(*) AS redenciones, SUM(r.puntos_redimidos) AS puntos,"
						+ " MAX(r.fecha_redencion) AS ultima, COUNT(DISTINCT r.idtienda) AS tiendas"
						+ " FROM pizzaamericana.fidelizacion_redencion r"
						+ " WHERE r.estado = 'CONFIRMADA' AND r.fecha_redencion >= ?"
						+ " AND r.fecha_redencion < DATE_ADD(?, INTERVAL 1 DAY)" + " AND r.correo LIKE '%@%'" + filtroTiendaR
						+ " GROUP BY r.correo ORDER BY puntos DESC LIMIT " + tope, desde, hasta);
			} else if ("EMPLEADOS_REDIMEN".equals(tipo)) {
				filas = consulta(cn,
						"SELECT r.usuario, MAX(IFNULL(NULLIF(emp.nombre_largo,''), IFNULL(usr.nombre_largo,''))) AS nombre,"
						+ " COUNT(*) AS redenciones,"
						+ " SUM(CASE WHEN r.estado = 'CONFIRMADA' THEN r.puntos_redimidos ELSE 0 END) AS puntos,"
						+ " COUNT(DISTINCT r.correo) AS clientes, SUM(r.estado = 'REVERSADA') AS reversadas,"
						+ " GROUP_CONCAT(DISTINCT r.idtienda ORDER BY r.idtienda) AS tiendas,"
						+ " MAX(r.fecha_redencion) AS ultima"
						+ " FROM pizzaamericana.fidelizacion_redencion r"
						+ " LEFT JOIN general.empleado emp ON emp.nombre = r.usuario"
						+ " LEFT JOIN pizzaamericana.usuario usr ON usr.nombre = r.usuario"
						+ " WHERE r.usuario <> '' AND UPPER(r.usuario) NOT IN " + CANALES
						+ " AND r.fecha_redencion >= ? AND r.fecha_redencion < DATE_ADD(?, INTERVAL 1 DAY)" + filtroTiendaR
						+ " GROUP BY r.usuario ORDER BY puntos DESC, redenciones DESC LIMIT " + tope,
						desde, hasta);
			} else if ("EMPLEADOS_ACUMULAN".equals(tipo)) {
				filas = consulta(cn,
						"SELECT t.usuario, MAX(IFNULL(NULLIF(emp.nombre_largo,''), IFNULL(usr.nombre_largo,''))) AS nombre,"
						+ " COUNT(*) AS acumulaciones, COUNT(DISTINCT t.correo) AS clientes,"
						+ " SUM(t.puntos) AS puntos, SUM(t.valor_neto) AS venta,"
						+ " GROUP_CONCAT(DISTINCT t.idtienda ORDER BY t.idtienda) AS tiendas"
						+ " FROM pizzaamericana.fidelizacion_transaccion t"
						+ " LEFT JOIN general.empleado emp ON emp.nombre = t.usuario"
						+ " LEFT JOIN pizzaamericana.usuario usr ON usr.nombre = t.usuario"
						+ " WHERE t.usuario <> '' AND UPPER(t.usuario) NOT IN " + CANALES
						+ " AND t.fecha_transaccion BETWEEN ? AND ?" + filtroTiendaT
						+ " GROUP BY t.usuario ORDER BY acumulaciones DESC, puntos DESC LIMIT " + tope,
						desde, hasta);
			} else {
				r.put("resultado", "ERROR");
				r.put("mensaje", "Consulta desconocida.");
				return (r);
			}

			// Las filas de empleados traen los ids de tienda: se vuelven nombres.
			for (int i = 0; i < filas.size(); i++) {
				final JSONObject o = (JSONObject) filas.get(i);
				final Object ts = o.get("tiendas");
				if (ts instanceof String && ((String) ts).length() > 0 && ((String) ts).matches("[0-9,]+")) {
					final StringBuilder sb = new StringBuilder();
					for (final String id : ((String) ts).split(",")) {
						final Integer k = Integer.valueOf(id);
						if (sb.length() > 0) {
							sb.append(", ");
						}
						sb.append(tiendas.containsKey(k) ? tiendas.get(k) : id);
					}
					o.put("tiendas", sb.toString());
				}
			}
			if ("CLIENTES_SALDO".equals(tipo) || "CLIENTES_REDIMEN".equals(tipo)) {
				ponerDatosCliente(cn, filas);
			}
			r.put("filas", filas);
			r.put("resultado", "OK");
		} catch (final Exception e) {
			logger.error("TableroPuntosDAO.ranking(" + tipo + "): " + e.toString(), e);
			r.put("resultado", "ERROR");
			r.put("mensaje", "No se pudo consultar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	/**
	 * Los clientes que tienen puntos por vencerse en los proximos N dias, para
	 * avisarles. Es la lista que se baja a CSV.
	 */
	public static JSONObject porVencer(final int dias, final int idTienda, final int limite) {
		final JSONObject r = new JSONObject();
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final int tope = Math.max(1, Math.min(limite, TOPE_FILAS * 20));
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String filtroTienda = idTienda > 0 ? " AND t.idtienda = " + idTienda : "";
			final JSONArray filas = consulta(cn,
					"SELECT t.correo,"
					+ " SUM(CASE WHEN t.fecha_vencimiento BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)"
					+ "          THEN t.puntos - t.puntos_redimidos ELSE 0 END) AS por_vencer,"
					+ " MIN(CASE WHEN t.fecha_vencimiento >= CURDATE() THEN t.fecha_vencimiento END) AS proxima,"
					+ " SUM(t.puntos - t.puntos_redimidos) AS saldo"
					+ " FROM pizzaamericana.fidelizacion_transaccion t"
					+ " WHERE t.vencidos = 'N' AND t.puntos > t.puntos_redimidos AND t.correo LIKE '%@%'" + filtroTienda
					+ " GROUP BY t.correo"
					+ " HAVING por_vencer > 0 ORDER BY por_vencer DESC LIMIT " + tope,
					Integer.valueOf(dias));
			ponerDatosCliente(cn, filas);
			r.put("filas", filas);
			r.put("resultado", "OK");
		} catch (final Exception e) {
			logger.error("TableroPuntosDAO.porVencer: " + e.toString(), e);
			r.put("resultado", "ERROR");
			r.put("mensaje", "No se pudo consultar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	/**
	 * Le pone a cada fila nombre, celular y si el correo es de un empleado.
	 *
	 * Se hace DESPUES, sobre las filas que ya salieron (a lo sumo unos miles), y no
	 * con un join: crm.persona_resumen no tiene indice por correo y unirla contra el
	 * libro entero recorreria 460 mil filas por cada una.
	 */
	@SuppressWarnings("unchecked")
	private static void ponerDatosCliente(final Connection cn, final JSONArray filas) throws Exception {
		final LinkedHashSet<String> correos = new LinkedHashSet<String>();
		for (int i = 0; i < filas.size(); i++) {
			final Object c = ((JSONObject) filas.get(i)).get("correo");
			if (c != null && String.valueOf(c).trim().length() > 0) {
				correos.add(String.valueOf(c).trim());
			}
		}
		final Map<String, String[]> datos = new HashMap<String, String[]>();
		final java.util.HashSet<String> empleados = new java.util.HashSet<String>();
		final ArrayList<String> lista = new ArrayList<String>(correos);
		// Por lotes de 400 para no armar un IN gigante.
		for (int ini = 0; ini < lista.size(); ini += 400) {
			final java.util.List<String> lote = lista.subList(ini, Math.min(ini + 400, lista.size()));
			final StringBuilder marcas = new StringBuilder();
			for (int k = 0; k < lote.size(); k++) {
				marcas.append(k == 0 ? "?" : ",?");
			}
			PreparedStatement ps = cn.prepareStatement(
					"SELECT email, nombre, apellido, celular, pedidos FROM crm.persona_resumen"
					+ " WHERE email IN (" + marcas + ") ORDER BY pedidos DESC");
			for (int k = 0; k < lote.size(); k++) {
				ps.setString(k + 1, lote.get(k));
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String e = rs.getString("email");
				if (e != null && !datos.containsKey(e)) {
					datos.put(e, new String[] {
							(rs.getString("nombre") == null ? "" : rs.getString("nombre")) + " "
									+ (rs.getString("apellido") == null ? "" : rs.getString("apellido")),
							rs.getString("celular") == null ? "" : rs.getString("celular") });
				}
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement("SELECT LOWER(TRIM(email)) AS e FROM general.empleado WHERE LOWER(TRIM(email)) IN ("
					+ marcas + ")");
			for (int k = 0; k < lote.size(); k++) {
				ps.setString(k + 1, lote.get(k).toLowerCase());
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				empleados.add(rs.getString("e"));
			}
			rs.close();
			ps.close();
		}
		for (int i = 0; i < filas.size(); i++) {
			final JSONObject o = (JSONObject) filas.get(i);
			final String c = String.valueOf(o.get("correo")).trim();
			final String[] d = datos.get(c);
			o.put("nombre", d == null ? "" : d[0].trim());
			o.put("celular", d == null ? "" : d[1]);
			o.put("es_empleado", empleados.contains(c.toLowerCase()) ? "S" : "N");
		}
	}
}
