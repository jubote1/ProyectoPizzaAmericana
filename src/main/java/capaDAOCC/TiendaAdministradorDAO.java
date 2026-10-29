package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * De que tienda es un administrador de punto de venta.
 *
 * No existe una columna "la tienda de esta persona": los administradores se
 * mueven. Lo que si existe es el registro de horas trabajadas
 * (general.horario_trabajado: empleado, fecha, horas, tienda), asi que la
 * tienda se DEDUCE: la que mas horas le sumo la semana pasada.
 *
 * Quien rota de verdad -los supernumerarios- no tiene una tienda que deducir y
 * se marca a mano con general.empleado.supernumerario = 1. Esa columna la crea
 * sql/2026_10_29_01_administrador_supernumerario.sql, que tambien explica por que
 * no se reutilizo general.empleado.rotacion: el POS ya la usa para otra cosa.
 *
 * Medido el 2026-10-29 sobre seis semanas: los administradores fijos concentran
 * entre el 81 y el 100 por ciento de sus horas en una sola tienda, y los dos
 * supernumerarios andan entre el 33 y el 84. Las bandas se tocan, asi que
 * adivinar por porcentaje se equivocaria con Mayra alguna semana: por eso la
 * marca es manual y el porcentaje no se usa para nada.
 */
public class TiendaAdministradorDAO {

	/** La semana pasada completa, de lunes a domingo, en horas trabajadas por tienda. */
	private static final String SQL_SEMANA_PASADA =
			"SELECT h.idtienda, SUM(h.horas) AS horas "
			+ "FROM general.horario_trabajado h "
			+ "WHERE h.id = ? "
			+ "  AND h.fecha BETWEEN DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY) "
			+ "                  AND DATE_SUB(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 1 DAY) "
			+ "GROUP BY h.idtienda ORDER BY horas DESC LIMIT 1";

	/** Respaldo: el ultimo mes. Un administrador que estuvo incapacitado o de vacaciones la semana pasada. */
	private static final String SQL_ULTIMO_MES =
			"SELECT h.idtienda, SUM(h.horas) AS horas "
			+ "FROM general.horario_trabajado h "
			+ "WHERE h.id = ? AND h.fecha >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) "
			+ "GROUP BY h.idtienda ORDER BY horas DESC LIMIT 1";

	private static final String SQL_EMPLEADO =
			"SELECT u.idempleado, e.supernumerario "
			+ "FROM usuario u LEFT JOIN general.empleado e ON e.id = u.idempleado "
			+ "WHERE u.id = ?";

	/** Lo que se sabe de un usuario: su empleado, si rota, y la tienda deducida. */
	public static class Asignacion {
		/** 0 si el usuario no esta amarrado a ningun empleado. */
		public int idEmpleado = 0;
		/** true si esta marcado como supernumerario: no se le restringe nada. */
		public boolean rota = false;
		/** 0 = no se pudo deducir ninguna tienda. */
		public int idTienda = 0;
		/** "SEMANA_PASADA", "ULTIMO_MES", "ROTA" o "SIN_DATOS": para poder explicar lo que paso. */
		public String origen = "SIN_DATOS";
	}

	/**
	 * Deduce la tienda del usuario. Nunca lanza: si la base no responde devuelve una
	 * asignacion sin tienda, y es quien llama el que decide que hacer con eso.
	 */
	public static Asignacion asignacionDe(final int idUsuario) {
		final Logger logger = Logger.getLogger("log_file");
		final Asignacion a = new Asignacion();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDPrincipal();
			final PreparedStatement pst = con1.prepareStatement(SQL_EMPLEADO);
			pst.setInt(1, idUsuario);
			final ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				a.idEmpleado = rs.getInt("idempleado");
				a.rota = (rs.getInt("supernumerario") == 1);
			}
			rs.close();
			pst.close();

			if (a.idEmpleado <= 0) {
				return (a);
			}
			if (a.rota) {
				a.origen = "ROTA";
				return (a);
			}
			a.idTienda = tiendaCon(con1, SQL_SEMANA_PASADA, a.idEmpleado);
			if (a.idTienda > 0) {
				a.origen = "SEMANA_PASADA";
			} else {
				a.idTienda = tiendaCon(con1, SQL_ULTIMO_MES, a.idEmpleado);
				if (a.idTienda > 0) {
					a.origen = "ULTIMO_MES";
				}
			}
		} catch (final Exception e) {
			logger.error("No se pudo deducir la tienda del usuario " + idUsuario + ": " + e.getMessage());
		} finally {
			try {
				if (con1 != null) {
					con1.close();
				}
			} catch (final Exception e) {
				logger.error("No se pudo cerrar la conexion al deducir la tienda: " + e.getMessage());
			}
		}
		return (a);
	}

	private static int tiendaCon(final Connection con1, final String sql, final int idEmpleado) throws Exception {
		final PreparedStatement pst = con1.prepareStatement(sql);
		pst.setInt(1, idEmpleado);
		final ResultSet rs = pst.executeQuery();
		final int id = (rs.next() ? rs.getInt("idtienda") : 0);
		rs.close();
		pst.close();
		return (id);
	}
}
