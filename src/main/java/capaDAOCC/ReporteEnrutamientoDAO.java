package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El reporte de uso del enrutamiento, leido de la base LOCAL de una tienda.
 *
 * QUE MIDE
 *
 * De los domicilios propios de la tienda en un rango de fechas, cuantos salieron
 * dentro de una ruta que el sistema sugirio (enrutamiento) y cuantos no, y a que
 * horas entran y a que horas salen los pedidos.
 *
 * DOMICILIO PROPIO
 *
 * Es el mismo criterio con el que el monitoreo del POS cuenta "Domicilio":
 * idtipopedido = 1, sin las plataformas (estacion con DOMI-Plat, DIDI o RAPPI),
 * sin los domicilios tercerizados (domicilio_tercerizado = 'S', Rappi Cargo, que
 * los lleva un tercero y no pasan por un domiciliario de la tienda, asi que no
 * pueden usar el enrutamiento) y sin los anulados. Las plataformas y los
 * tercerizados no se mezclan: inflarian el total y harian ver peor a la tienda.
 *
 * QUE ES "USO EL ENRUTAMIENTO"
 *
 * Que el pedido salio con un domiciliario que lo llevaba en la ruta que se le
 * habia sugerido: despacho_real_det.origen_pedido = 'SUGERIDO'. Es el dato que
 * el POS graba al momento de la salida. Una sugerencia que el domiciliario solto
 * y que despues salio "libre" NO cuenta como enrutada: lo que se quiere saber es
 * si el enrutamiento se uso de verdad, no si se intento.
 *
 * LAS DOS HORAS
 *
 *  - Entrada: la hora de pedido.fechainsercion, o sea cuando el pedido entro a la
 *    tienda.
 *  - Salida: la hora del ultimo cambio de estado del pedido a "En ruta" en
 *    cambios_estado_pedido. Es la ultima y no la primera porque un pedido puede
 *    devolverse a "Empacado" y volver a salir; la que vale es la salida real.
 *    El id del estado "En ruta" se lee del parametro ENRUTADOMICILIO de la
 *    tienda, con 7 como respaldo (es el que usa el POS en sus consultas fijas).
 *
 * Las horas son las del reloj de la tienda, 0 a 23. Los pedidos se agrupan por
 * la fecha de JORNADA (pedido.fechapedido), no por la del reloj: un pedido de
 * las 00:30 pertenece a la jornada anterior.
 *
 * UNA TIENDA ATRASADA NO TUMBA EL REPORTE
 *
 * Si la tienda todavia no tiene la columna despacho_real_det.origen_pedido (la
 * migracion 2026_09_10_03 del POS), el reporte igual muestra sus pedidos y sus
 * horas, pero marca enrutamientoInstalado = false para que la pantalla lo diga
 * y no cuente a esa tienda como "cero por ciento".
 */
public class ReporteEnrutamientoDAO {

	public static final String ESTADO_OK = "OK";
	public static final String ESTADO_SIN_CONEXION = "SIN_CONEXION";
	public static final String ESTADO_TARDO = "TARDO";
	public static final String ESTADO_ERROR = "ERROR";

	/** Lo que se encontro en una tienda. */
	public static class ResultadoTienda {
		public int idTienda = 0;
		public String tienda = "";
		/** OK, SIN_CONEXION, TARDO o ERROR. */
		public String estado = ESTADO_ERROR;
		public String error = "";
		/** La tienda ya tiene el enrutamiento instalado (existe la columna origen_pedido). */
		public boolean enrutamientoInstalado = false;

		/** Domicilios propios del rango. */
		public int pedidos = 0;
		/** De ellos, los que salieron dentro de una ruta sugerida. */
		public int enrutados = 0;

		/** Por hora de entrada a la tienda (0 a 23): pedidos y cuantos de ellos salieron enrutados. */
		public int[] entradas = new int[24];
		public int[] entradasEnrutadas = new int[24];

		/** Por hora de salida (cambio a En ruta): pedidos y cuantos de ellos salieron enrutados. */
		public int[] salidas = new int[24];
		public int[] salidasEnrutadas = new int[24];

		/** Suma de salidas: los pedidos que tienen registro de salida. */
		public int salidasTotal = 0;
		public int salidasEnrutadasTotal = 0;

		/** Lo que NO entra al reporte y se cuenta aparte para que se sepa. */
		public int excluidosPlataforma = 0;
		public int excluidosTercerizados = 0;
	}

	/** Plataformas: la estacion lo dice. */
	private static final String ES_PLATAFORMA =
			"(IFNULL(p.estacion,'') LIKE '%DOMI-Plat%' OR IFNULL(p.estacion,'') LIKE '%DIDI%' OR IFNULL(p.estacion,'') LIKE '%RAPPI%')";

	/** Domicilio propio, sin plataformas, sin tercerizados y sin anulados. */
	private static final String DOMICILIO_PROPIO =
			"p.idtipopedido = 1 AND NOT " + ES_PLATAFORMA
			+ " AND IFNULL(p.domicilio_tercerizado,'N') <> 'S' AND IFNULL(p.idmotivoanulacion,0) = 0";

	/**
	 * Los pedidos que salieron en una ruta sugerida. Se arma como tabla derivada
	 * y no como EXISTS por fila: asi MySQL la materializa una vez en vez de
	 * recorrer despacho_real_det por cada pedido.
	 */
	private static final String JOIN_ENRUTADOS =
			" LEFT JOIN (SELECT DISTINCT id_pedido FROM despacho_real_det WHERE origen_pedido = 'SUGERIDO') e"
			+ " ON e.id_pedido = p.idpedidotienda";

	/** El estado "En ruta" de ESA tienda; 7 si el parametro no esta. */
	private static final String ESTADO_EN_RUTA =
			"IFNULL((SELECT valornumerico FROM parametros WHERE valorparametro = 'ENRUTADOMICILIO' LIMIT 1), 7)";

	/**
	 * Todo lo del reporte en UNA tienda. Devuelve siempre un resultado, nunca
	 * null: si la tienda no contesta queda en SIN_CONEXION, que es distinto de
	 * "no tuvo pedidos".
	 *
	 * @param desde y hasta en yyyy-MM-dd, inclusive
	 */
	public static ResultadoTienda consultarTienda(final int idTienda, final String nombreTienda,
			final String hosbd, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ResultadoTienda res = new ResultadoTienda();
		res.idTienda = idTienda;
		res.tienda = (nombreTienda == null ? "" : nombreTienda);

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDTiendaRemota(hosbd);
			if (cn == null) {
				res.estado = ESTADO_SIN_CONEXION;
				res.error = "El computador de la tienda no se encuentra en linea";
				return (res);
			}
			res.enrutamientoInstalado = existeColumna(cn, "despacho_real_det", "origen_pedido");
			cargarEntradas(cn, res, desde, hasta);
			cargarSalidas(cn, res, desde, hasta);
			cargarExcluidos(cn, res, desde, hasta);
			res.estado = ESTADO_OK;
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoDAO.consultarTienda " + hosbd + ": " + e.toString());
			res.estado = ESTADO_ERROR;
			res.error = e.toString();
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/** Pedidos por hora de entrada, y de ellos cuantos usaron el enrutamiento. */
	private static void cargarEntradas(final Connection cn, final ResultadoTienda res, final String desde,
			final String hasta) throws Exception {
		final boolean inst = res.enrutamientoInstalado;
		final String sql = "SELECT HOUR(p.fechainsercion) AS hora, COUNT(*) AS pedidos, "
				+ (inst ? "SUM(e.id_pedido IS NOT NULL)" : "0") + " AS enrutados"
				+ " FROM pedido p" + (inst ? JOIN_ENRUTADOS : "")
				+ " WHERE p.fechapedido BETWEEN ? AND ? AND " + DOMICILIO_PROPIO
				+ " GROUP BY HOUR(p.fechainsercion)";
		final PreparedStatement ps = cn.prepareStatement(sql);
		ps.setString(1, desde);
		ps.setString(2, hasta);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final int hora = rs.getInt("hora");
			final boolean sinHora = rs.wasNull();
			final int pedidos = rs.getInt("pedidos");
			final int enrutados = rs.getInt("enrutados");
			//Un pedido sin fecha de insercion cuenta en el total pero no tiene hora
			//donde pintarse.
			res.pedidos = res.pedidos + pedidos;
			res.enrutados = res.enrutados + enrutados;
			if (!sinHora && hora >= 0 && hora <= 23) {
				res.entradas[hora] = pedidos;
				res.entradasEnrutadas[hora] = enrutados;
			}
		}
		rs.close();
		ps.close();
	}

	/**
	 * Pedidos por hora de salida. La salida es el ultimo cambio a "En ruta".
	 *
	 * La tabla derivada de cambios_estado_pedido se acota por fecha: un pedido de
	 * la jornada D sale entre D y D+1 (despues de medianoche), asi que alcanza con
	 * mirar los cambios de desde hasta hasta + 2 dias. Sin ese acote se agruparia
	 * toda la historia de la tabla, que es la mas grande de la base.
	 */
	private static void cargarSalidas(final Connection cn, final ResultadoTienda res, final String desde,
			final String hasta) throws Exception {
		final boolean inst = res.enrutamientoInstalado;
		final String sql = "SELECT HOUR(s.salida) AS hora, COUNT(*) AS salidas, SUM(s.enr) AS enrutados FROM ("
				+ " SELECT c.salida AS salida, " + (inst ? "(e.id_pedido IS NOT NULL)" : "0") + " AS enr"
				+ " FROM pedido p"
				+ " INNER JOIN (SELECT idpedidotienda, MAX(fechacambio) AS salida FROM cambios_estado_pedido"
				+ "   WHERE idestadoposterior = " + ESTADO_EN_RUTA
				+ "   AND fechacambio >= ? AND fechacambio < DATE_ADD(?, INTERVAL 3 DAY)"
				+ "   GROUP BY idpedidotienda) c ON c.idpedidotienda = p.idpedidotienda"
				+ (inst ? JOIN_ENRUTADOS : "")
				+ " WHERE p.fechapedido BETWEEN ? AND ? AND " + DOMICILIO_PROPIO
				+ ") s GROUP BY HOUR(s.salida)";
		final PreparedStatement ps = cn.prepareStatement(sql);
		ps.setString(1, desde);
		ps.setString(2, hasta);
		ps.setString(3, desde);
		ps.setString(4, hasta);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final int hora = rs.getInt("hora");
			final boolean sinHora = rs.wasNull();
			final int salidas = rs.getInt("salidas");
			final int enrutados = rs.getInt("enrutados");
			res.salidasTotal = res.salidasTotal + salidas;
			res.salidasEnrutadasTotal = res.salidasEnrutadasTotal + enrutados;
			if (!sinHora && hora >= 0 && hora <= 23) {
				res.salidas[hora] = salidas;
				res.salidasEnrutadas[hora] = enrutados;
			}
		}
		rs.close();
		ps.close();
	}

	/** Lo que se deja por fuera, para decirlo en pantalla. */
	private static void cargarExcluidos(final Connection cn, final ResultadoTienda res, final String desde,
			final String hasta) throws Exception {
		final String sql = "SELECT"
				+ " SUM(CASE WHEN " + ES_PLATAFORMA + " THEN 1 ELSE 0 END) AS plataforma,"
				+ " SUM(CASE WHEN NOT " + ES_PLATAFORMA + " AND IFNULL(p.domicilio_tercerizado,'N') = 'S' THEN 1 ELSE 0 END) AS tercerizado"
				+ " FROM pedido p"
				+ " WHERE p.fechapedido BETWEEN ? AND ? AND p.idtipopedido = 1 AND IFNULL(p.idmotivoanulacion,0) = 0";
		final PreparedStatement ps = cn.prepareStatement(sql);
		ps.setString(1, desde);
		ps.setString(2, hasta);
		final ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			res.excluidosPlataforma = rs.getInt("plataforma");
			res.excluidosTercerizados = rs.getInt("tercerizado");
		}
		rs.close();
		ps.close();
	}

	/**
	 * Si una columna existe en la base a la que ya estamos conectados. Ante la
	 * duda devuelve false, que es el camino que no reviente en una tienda atrasada.
	 */
	private static boolean existeColumna(final Connection cn, final String tabla, final String columna) {
		try {
			final Statement stm = cn.createStatement();
			final ResultSet rs = stm.executeQuery("SELECT COUNT(*) AS hay FROM information_schema.columns"
					+ " WHERE table_schema = DATABASE() AND table_name = '" + tabla + "' AND column_name = '"
					+ columna + "'");
			boolean existe = false;
			if (rs.next()) {
				existe = (rs.getInt("hay") > 0);
			}
			rs.close();
			stm.close();
			return (existe);
		} catch (final Exception e) {
			return (false);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			//Nada que hacer: la conexion ya estaba muerta.
		}
	}
}
