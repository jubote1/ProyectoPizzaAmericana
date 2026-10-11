package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Los envios de publicidad programados para una fecha y hora futura (crm.campana_programada).
 *
 * UNA PROGRAMACION GUARDA PARAMETROS, NO DESTINATARIOS
 *
 * Lo que se guarda es lo que la pantalla de Envio de Publicidad mando -filtros, canal, plantilla,
 * tope, oferta-. El publico se arma al momento del envio, con los datos de ese momento: asi quien
 * autorizo, compro o dejo de recibir entre la programacion y la hora queda bien clasificado, y el
 * descanso entre envios (cliente.ultima_fecha_publicidad) cuenta lo que de verdad se haya mandado
 * mientras tanto. Ver sql/2026_10_11_01_crm_campana_programada.sql.
 *
 * QUIEN LA TOMA
 *
 * reclamar() es un UPDATE condicionado (PROGRAMADA -> EJECUTANDO): si dos instancias del central
 * corrieran el mismo reloj, la base decide y solo una se lleva la fila. Un SELECT y despues un
 * UPDATE las dejaria disparar la misma campana dos veces, que es exactamente lo que no puede pasar
 * con una campana a clientes.
 */
public class CampanaProgramadaDAO {

	public static final String PROGRAMADA = "PROGRAMADA";
	public static final String EJECUTANDO = "EJECUTANDO";
	public static final String ENVIADA = "ENVIADA";
	public static final String FALLIDA = "FALLIDA";
	public static final String VENCIDA = "VENCIDA";
	public static final String CANCELADA = "CANCELADA";

	/** Lo mas lejos que se puede programar un envio. Mas alla de eso casi seguro es un error de digitacion. */
	public static final int MAXIMO_DIAS = 90;

	/** Cuanto debe faltar, como minimo, para que algo se considere "futuro". */
	public static final int MINIMO_MINUTOS = 2;

	public static class Programacion {
		public long idProgramacion;
		public long idCampana;
		public String nombre = "";
		public String canal = "";
		public int idOferta;
		public int esperados;
		public int tope;
		public String resumenFiltros = "";
		public String parametros = "";
		public String ejecutarEn = "";
		public String estado = "";
		public long idEnvio;
		public String resultado = "";
		public String usuario = "";
		public String creadoEn = "";
		/** Minutos que lleva pasada de su hora; negativo si todavia no llega. Lo calcula la base. */
		public int minutosVencida;
	}

	private CampanaProgramadaDAO() {
	}

	/**
	 * Guarda la programacion.
	 *
	 * @param ejecutarEn texto "yyyy-MM-dd HH:mm:ss" en la hora del servidor
	 * @return el id, o 0 si no se pudo guardar
	 */
	public static long crear(final long idCampana, final String nombre, final String canal,
			final int idOferta, final int esperados, final int tope, final String resumenFiltros,
			final String parametrosJson, final String ejecutarEn, final String usuario) {
		long id = 0;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO crm.campana_programada (idcampana, nombre, canal, idoferta, esperados,"
					+ " tope, resumen_filtros, parametros, ejecutar_en, estado, usuario, creado_en)"
					+ " VALUES (?,?,?,?,?,?,?,?,?, 'PROGRAMADA', ?, NOW())",
					Statement.RETURN_GENERATED_KEYS);
			if (idCampana > 0) {
				ps.setLong(1, idCampana);
			} else {
				ps.setNull(1, java.sql.Types.BIGINT);
			}
			ps.setString(2, recortar(nombre, 150));
			ps.setString(3, canal);
			ps.setInt(4, idOferta);
			ps.setInt(5, esperados);
			ps.setInt(6, tope);
			ps.setString(7, recortar(resumenFiltros, 2000));
			ps.setString(8, parametrosJson);
			ps.setString(9, ejecutarEn);
			ps.setString(10, recortar(usuario, 50));
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.crear: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	/**
	 * Cuantos minutos faltan para esa fecha, segun el reloj de la BASE (no el del navegador ni el de la
	 * JVM): es el mismo reloj contra el que despues se decide si ya es hora.
	 *
	 * @return los minutos; Integer.MIN_VALUE si la fecha no se pudo leer
	 */
	public static int minutosHasta(final String ejecutarEn) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement("SELECT TIMESTAMPDIFF(MINUTE, NOW(), ?)");
			ps.setString(1, ejecutarEn);
			final ResultSet rs = ps.executeQuery();
			int minutos = Integer.MIN_VALUE;
			if (rs.next()) {
				final int valor = rs.getInt(1);
				minutos = rs.wasNull() ? Integer.MIN_VALUE : valor;
			}
			rs.close();
			ps.close();
			return (minutos);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.minutosHasta: " + e.toString());
			return (Integer.MIN_VALUE);
		} finally {
			cerrar(cn);
		}
	}

	/** Las que ya llegaron a su hora y siguen esperando, la mas antigua primero. */
	public static ArrayList<Long> idsListas(final int limite) {
		final ArrayList<Long> ids = new ArrayList<Long>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idprogramacion FROM crm.campana_programada"
					+ " WHERE estado = 'PROGRAMADA' AND ejecutar_en <= NOW()"
					+ " ORDER BY ejecutar_en, idprogramacion LIMIT ?");
			ps.setInt(1, limite);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(Long.valueOf(rs.getLong(1)));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.idsListas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (ids);
	}

	public static Programacion obtener(final long id) {
		Programacion p = null;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(SELECT + " WHERE idprogramacion = ?");
			ps.setLong(1, id);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				p = leer(rs, true);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.obtener: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (p);
	}

	/**
	 * Las que esperan y las de los ultimos dias, para la pantalla. Sin los parametros: son largos y la
	 * pantalla no los necesita.
	 */
	public static ArrayList<Programacion> listar(final int diasAtras) {
		final ArrayList<Programacion> lista = new ArrayList<Programacion>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					SELECT_SIN_PARAMETROS
					+ " WHERE estado IN ('PROGRAMADA','EJECUTANDO')"
					+ "    OR ejecutar_en >= DATE_SUB(NOW(), INTERVAL ? DAY)"
					+ " ORDER BY (estado IN ('PROGRAMADA','EJECUTANDO')) DESC, ejecutar_en DESC");
			ps.setInt(1, diasAtras);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(leer(rs, false));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.listar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * PROGRAMADA -> EJECUTANDO. Devuelve true solo si ESTA llamada la tomo.
	 */
	public static boolean reclamar(final long id) {
		return (cambiar(id, PROGRAMADA, EJECUTANDO, null, null));
	}

	/** Cancela una que todavia espera. Devuelve false si ya no estaba en espera (la tomo el reloj o ya salio). */
	public static boolean cancelar(final long id, final String usuario) {
		return (cambiar(id, PROGRAMADA, CANCELADA, null, "Cancelada por " + usuario));
	}

	/** Cierra una programacion que se estaba ejecutando con el estado final y, si hubo, la tanda creada. */
	public static void terminar(final long id, final String estado, final Long idEnvio, final String resultado) {
		cambiar(id, EJECUTANDO, estado, idEnvio, resultado);
	}

	/** Marca VENCIDA una que nunca se tomo: paso su plazo de gracia. */
	public static boolean vencer(final long id, final String resultado) {
		return (cambiar(id, PROGRAMADA, VENCIDA, null, resultado));
	}

	/**
	 * Al arrancar la aplicacion: las que quedaron EJECUTANDO es que el servidor se cayo o se redesplego
	 * justo al dispararlas. NO se reintentan: pudieron haber alcanzado a crear la tanda y a mandar parte,
	 * y reintentar es lo que le escribe dos veces a un cliente. Quedan FALLIDA, con el motivo, para que
	 * una persona mire la tanda y decida si reprograma.
	 *
	 * @return cuantas marco
	 */
	public static int marcarInterrumpidas() {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana_programada SET estado = 'FALLIDA', actualizado_en = NOW(),"
					+ " resultado = 'El servidor se reinicio mientras se disparaba. Revise Envios anteriores:"
					+ " pudo haber salido en parte. Si hace falta, vuelva a programarla.'"
					+ " WHERE estado = 'EJECUTANDO'");
			final int filas = ps.executeUpdate();
			ps.close();
			return (filas);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.marcarInterrumpidas: " + e.toString());
			return (0);
		} finally {
			cerrar(cn);
		}
	}

	// -----------------------------------------------------------------------

	private static final String COLUMNAS =
			//El nombre: el que se escribio, o el de la campana escogida del desplegable (ahi el campo
			//"nombre" viene vacio).
			"SELECT idprogramacion, IFNULL(idcampana,0) idcampana,"
			+ " IFNULL(NULLIF(nombre,''), IFNULL((SELECT c.nombre FROM crm.campana c"
			+ "   WHERE c.idcampana = campana_programada.idcampana),'')) nombre, canal,"
			+ " idoferta, esperados, tope, IFNULL(resumen_filtros,'') resumen,";

	private static final String SELECT = COLUMNAS
			+ " parametros, DATE_FORMAT(ejecutar_en,'%Y-%m-%d %H:%i') ejecutar_en, estado,"
			+ " IFNULL(idenvio,0) idenvio, IFNULL(resultado,'') resultado, IFNULL(usuario,'') usuario,"
			+ " DATE_FORMAT(creado_en,'%Y-%m-%d %H:%i') creado_en,"
			+ " TIMESTAMPDIFF(MINUTE, ejecutar_en, NOW()) minutos_vencida"
			+ " FROM crm.campana_programada";

	private static final String SELECT_SIN_PARAMETROS = COLUMNAS
			+ " '' parametros, DATE_FORMAT(ejecutar_en,'%Y-%m-%d %H:%i') ejecutar_en, estado,"
			+ " IFNULL(idenvio,0) idenvio, IFNULL(resultado,'') resultado, IFNULL(usuario,'') usuario,"
			+ " DATE_FORMAT(creado_en,'%Y-%m-%d %H:%i') creado_en,"
			+ " TIMESTAMPDIFF(MINUTE, ejecutar_en, NOW()) minutos_vencida"
			+ " FROM crm.campana_programada";

	private static Programacion leer(final ResultSet rs, final boolean conParametros) throws java.sql.SQLException {
		final Programacion p = new Programacion();
		p.idProgramacion = rs.getLong("idprogramacion");
		p.idCampana = rs.getLong("idcampana");
		p.nombre = rs.getString("nombre");
		p.canal = rs.getString("canal");
		p.idOferta = rs.getInt("idoferta");
		p.esperados = rs.getInt("esperados");
		p.tope = rs.getInt("tope");
		p.resumenFiltros = rs.getString("resumen");
		p.parametros = conParametros ? rs.getString("parametros") : "";
		p.ejecutarEn = rs.getString("ejecutar_en");
		p.estado = rs.getString("estado");
		p.idEnvio = rs.getLong("idenvio");
		p.resultado = rs.getString("resultado");
		p.usuario = rs.getString("usuario");
		p.creadoEn = rs.getString("creado_en");
		p.minutosVencida = rs.getInt("minutos_vencida");
		return (p);
	}

	private static boolean cambiar(final long id, final String desde, final String hasta, final Long idEnvio,
			final String resultado) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana_programada SET estado = ?, actualizado_en = NOW(),"
					+ " idenvio = IFNULL(?, idenvio), resultado = IFNULL(?, resultado)"
					+ " WHERE idprogramacion = ? AND estado = ?");
			ps.setString(1, hasta);
			if (idEnvio != null && idEnvio.longValue() > 0) {
				ps.setLong(2, idEnvio.longValue());
			} else {
				ps.setNull(2, java.sql.Types.BIGINT);
			}
			ps.setString(3, resultado == null ? null : recortar(resultado, 500));
			ps.setLong(4, id);
			ps.setString(5, desde);
			final int filas = ps.executeUpdate();
			ps.close();
			return (filas == 1);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO.cambiar " + id + " " + desde + "->" + hasta
					+ ": " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	private static String recortar(final String v, final int max) {
		if (v == null) {
			return (null);
		}
		return (v.length() <= max ? v : v.substring(0, max));
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaProgramadaDAO: no cerro la conexion, " + e.toString());
		}
	}
}
