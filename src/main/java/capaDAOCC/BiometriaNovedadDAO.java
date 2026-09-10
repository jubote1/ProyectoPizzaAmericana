package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import conexionCC.ConexionBaseDatos;

/**
 * Novedades de biometria y el rastro de los cambios hechos a empleado_evento.
 *
 * De donde sale la hora que vale: de general.empleado_evento. Esa tabla es la
 * fuente del calculo de horas, asi que cuando el supervisor confirma una
 * correccion hay que reescribirla alli.
 *
 * OJO con como se "modifica" un evento: la llave primaria de empleado_evento es
 * (id, tipo_evento, fecha, fecha_hora_log), o sea que la HORA es parte de la
 * llave. Cambiar la hora no es un UPDATE: es borrar la fila e insertar otra. El
 * valor original solo sobrevive porque queda copiado en
 * general.empleado_evento_log antes de tocar nada, y las dos operaciones van en
 * la misma transaccion.
 *
 * Las consultas van con PreparedStatement porque aqui entran datos escritos por
 * una persona en una pantalla.
 */
public class BiometriaNovedadDAO {

	/** Marca que queda en uso_biometria cuando la hora la puso una persona y no el huellero. */
	private static final String INTERVENCION_MANUAL = "N";

	/**
	 * Novedades reportadas en un rango de fechas.
	 *
	 * @param fechaDesde aaaa-mm-dd inclusive
	 * @param fechaHasta aaaa-mm-dd inclusive
	 * @param estado     REPORTADA, ATENDIDA, RECHAZADA, o vacio para todas
	 */
	public static List<JSONObject> obtenerNovedades(final String fechaDesde, final String fechaHasta,
			final String estado) {
		final List<JSONObject> novedades = new ArrayList<JSONObject>();
		final StringBuilder sql = new StringBuilder();
		sql.append(" SELECT n.idnovedad, n.id, e.nombre_largo, n.fecha, n.idtienda, ");
		sql.append("        n.evento_tipo, n.evento_fecha_hora_log, n.tipo_novedad, ");
		sql.append("        n.hora_reportada, n.observacion_reporte, n.reportado_por, ");
		sql.append("        n.fecha_reporte, n.origen, n.estado, n.observacion_resolucion, ");
		sql.append("        n.resuelto_por, n.fecha_resolucion, ");
		sql.append("        (SELECT COUNT(*) FROM general.empleado_evento_log l ");
		sql.append("          WHERE l.idnovedad = n.idnovedad) AS cambios ");
		sql.append(" FROM general.empleado_evento_novedad n ");
		sql.append(" LEFT JOIN general.empleado e ON e.id = n.id ");
		sql.append(" WHERE n.fecha BETWEEN ? AND ? ");
		if (estado != null && estado.trim().length() > 0) {
			sql.append(" AND n.estado = ? ");
		}
		sql.append(" ORDER BY n.estado = 'REPORTADA' DESC, n.fecha DESC, n.idnovedad DESC ");

		final ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDGeneral();
			 PreparedStatement pstmt = con1.prepareStatement(sql.toString())) {
			pstmt.setString(1, fechaDesde);
			pstmt.setString(2, fechaHasta);
			if (estado != null && estado.trim().length() > 0) {
				pstmt.setString(3, estado.trim());
			}
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					final JSONObject obj = new JSONObject();
					obj.put("idnovedad", rs.getInt("idnovedad"));
					obj.put("id", rs.getInt("id"));
					obj.put("nombre", texto(rs.getString("nombre_largo")));
					obj.put("fecha", texto(rs.getString("fecha")));
					obj.put("idtienda", rs.getInt("idtienda"));
					obj.put("eventotipo", texto(rs.getString("evento_tipo")));
					obj.put("eventohora", texto(rs.getString("evento_fecha_hora_log")));
					obj.put("tiponovedad", texto(rs.getString("tipo_novedad")));
					obj.put("horareportada", texto(rs.getString("hora_reportada")));
					obj.put("observacion", texto(rs.getString("observacion_reporte")));
					obj.put("reportadopor", texto(rs.getString("reportado_por")));
					obj.put("fechareporte", texto(rs.getString("fecha_reporte")));
					obj.put("origen", texto(rs.getString("origen")));
					obj.put("estado", texto(rs.getString("estado")));
					obj.put("observacionresolucion", texto(rs.getString("observacion_resolucion")));
					obj.put("resueltopor", texto(rs.getString("resuelto_por")));
					obj.put("fecharesolucion", texto(rs.getString("fecha_resolucion")));
					obj.put("cambios", rs.getInt("cambios"));
					novedades.add(obj);
				}
			}
		} catch (final Exception e) {
			System.out.println("BiometriaNovedadDAO.obtenerNovedades: " + e);
			e.printStackTrace();
		}
		return (novedades);
	}

	private static String texto(final String valor) {
		return (valor == null) ? "" : valor;
	}

	/**
	 * Todos los registros de biometria de un empleado en un dia.
	 *
	 * Es lo que se le muestra al supervisor: no solo el registro de la novedad,
	 * sino la jornada completa, para que pueda ver el contexto y corregir lo que
	 * haga falta.
	 */
	public static List<JSONObject> obtenerEventosDelDia(final int idEmpleado, final String fecha) {
		final List<JSONObject> eventos = new ArrayList<JSONObject>();
		final String sql = " SELECT v.tipo_evento, v.fecha, v.fecha_hora_log, v.idtienda, "
				+ "        IFNULL(v.uso_biometria, '') AS uso_biometria "
				+ " FROM general.empleado_evento v "
				+ " WHERE v.id = ? AND v.fecha = ? "
				+ " ORDER BY v.fecha_hora_log ASC ";
		final ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDGeneral();
			 PreparedStatement pstmt = con1.prepareStatement(sql)) {
			pstmt.setInt(1, idEmpleado);
			pstmt.setString(2, fecha);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					final JSONObject obj = new JSONObject();
					obj.put("tipo", texto(rs.getString("tipo_evento")));
					obj.put("fecha", texto(rs.getString("fecha")));
					obj.put("fechahora", texto(rs.getString("fecha_hora_log")));
					obj.put("idtienda", rs.getInt("idtienda"));
					obj.put("usobiometria", texto(rs.getString("uso_biometria")));
					eventos.add(obj);
				}
			}
		} catch (final Exception e) {
			System.out.println("BiometriaNovedadDAO.obtenerEventosDelDia: " + e);
			e.printStackTrace();
		}
		return (eventos);
	}

	/**
	 * Registra una novedad. La llama el POS: esa pantalla es abierta, cualquiera
	 * puede reportar, y por eso la novedad NO cambia nada por si sola. Es una
	 * propuesta que queda en estado REPORTADA hasta que el supervisor la revise.
	 *
	 * @return el idnovedad creado, o 0 si fallo
	 */
	public static int insertarNovedad(final int idEmpleado, final String fecha, final int idTienda,
			final String eventoTipo, final String eventoFechaHora, final String tipoNovedad,
			final String horaReportada, final String observacion, final String reportadoPor,
			final String origen) {
		final String sql = " INSERT INTO general.empleado_evento_novedad "
				+ " (id, fecha, idtienda, evento_tipo, evento_fecha, evento_fecha_hora_log, "
				+ "  tipo_novedad, hora_reportada, observacion_reporte, reportado_por, origen) "
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
		int idGenerado = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDGeneral();
			 PreparedStatement pstmt = con1.prepareStatement(sql,
					 java.sql.Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setInt(1, idEmpleado);
			pstmt.setString(2, fecha);
			pstmt.setInt(3, idTienda);
			ponerTextoONulo(pstmt, 4, eventoTipo);
			//evento_fecha es la misma fecha de la jornada cuando hay evento seleccionado.
			ponerTextoONulo(pstmt, 5, (eventoTipo == null || eventoTipo.trim().length() == 0) ? null : fecha);
			ponerTextoONulo(pstmt, 6, eventoFechaHora);
			pstmt.setString(7, tipoNovedad);
			ponerTextoONulo(pstmt, 8, horaReportada);
			pstmt.setString(9, observacion);
			pstmt.setString(10, reportadoPor);
			pstmt.setString(11, origen);
			pstmt.executeUpdate();
			try (ResultSet rs = pstmt.getGeneratedKeys()) {
				if (rs.next()) {
					idGenerado = rs.getInt(1);
				}
			}
		} catch (final Exception e) {
			System.out.println("BiometriaNovedadDAO.insertarNovedad: " + e);
			e.printStackTrace();
		}
		return (idGenerado);
	}

	private static void ponerTextoONulo(final PreparedStatement pstmt, final int posicion,
			final String valor) throws java.sql.SQLException {
		if (valor == null || valor.trim().length() == 0) {
			pstmt.setNull(posicion, java.sql.Types.VARCHAR);
		} else {
			pstmt.setString(posicion, valor.trim());
		}
	}

	/**
	 * Aplica UN cambio a general.empleado_evento y lo deja registrado en el log,
	 * todo en la misma transaccion. Si algo falla no queda ni el cambio ni el log.
	 *
	 * Acciones:
	 *   MODIFICA  borra la fila vieja e inserta la nueva. No es un UPDATE porque la
	 *             hora es parte de la llave primaria.
	 *   AGREGA    inserta un evento que no existia -el caso del que olvido marcar-.
	 *   ELIMINA   borra un evento, por ejemplo una marcacion duplicada.
	 *
	 * El evento que queda toma uso_biometria = 'N' a proposito: esa hora ya no
	 * viene del huellero sino de una persona, y asi el reporte semanal de no uso
	 * del huellero lo ve como lo que es, una intervencion manual.
	 *
	 * @return cadena vacia si todo salio bien, o el motivo del fallo
	 */
	public static String aplicarCambio(final int idNovedad, final int idEmpleado, final String fecha,
			final int idTienda, final String accion, final String antesTipo, final String antesFechaHora,
			final String despuesTipo, final String despuesFechaHora, final String usuario,
			final String observacion) {
		if (!"MODIFICA".equals(accion) && !"AGREGA".equals(accion) && !"ELIMINA".equals(accion)) {
			return ("La accion debe ser MODIFICA, AGREGA o ELIMINA.");
		}
		final boolean tocaBorrar = "MODIFICA".equals(accion) || "ELIMINA".equals(accion);
		final boolean tocaInsertar = "MODIFICA".equals(accion) || "AGREGA".equals(accion);
		if (tocaBorrar && (vacio(antesTipo) || vacio(antesFechaHora))) {
			return ("Para " + accion + " hay que indicar el evento original.");
		}
		if (tocaInsertar && (vacio(despuesTipo) || vacio(despuesFechaHora))) {
			return ("Para " + accion + " hay que indicar el evento nuevo.");
		}

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = null;
		try {
			con1 = con.obtenerConexionBDGeneral();
			con1.setAutoCommit(false);
			String usoBiometriaAntes = null;

			if (tocaBorrar) {
				//Se lee primero para poder guardar en el log como estaba de verdad.
				final String sqlLeer = " SELECT IFNULL(uso_biometria,'') AS uso_biometria "
						+ " FROM general.empleado_evento "
						+ " WHERE id = ? AND tipo_evento = ? AND fecha = ? AND fecha_hora_log = ? ";
				try (PreparedStatement leer = con1.prepareStatement(sqlLeer)) {
					leer.setInt(1, idEmpleado);
					leer.setString(2, antesTipo);
					leer.setString(3, fecha);
					leer.setString(4, antesFechaHora);
					try (ResultSet rs = leer.executeQuery()) {
						if (!rs.next()) {
							con1.rollback();
							return ("El registro original ya no existe. Alguien mas lo pudo cambiar; "
									+ "vuelva a cargar la jornada.");
						}
						usoBiometriaAntes = rs.getString("uso_biometria");
					}
				}
				final String sqlBorrar = " DELETE FROM general.empleado_evento "
						+ " WHERE id = ? AND tipo_evento = ? AND fecha = ? AND fecha_hora_log = ? ";
				try (PreparedStatement borrar = con1.prepareStatement(sqlBorrar)) {
					borrar.setInt(1, idEmpleado);
					borrar.setString(2, antesTipo);
					borrar.setString(3, fecha);
					borrar.setString(4, antesFechaHora);
					borrar.executeUpdate();
				}
			}

			if (tocaInsertar) {
				final String sqlInsertar = " INSERT INTO general.empleado_evento "
						+ " (id, tipo_evento, fecha, fecha_hora_log, idtienda, uso_biometria) "
						+ " VALUES (?, ?, ?, ?, ?, ?) ";
				try (PreparedStatement insertar = con1.prepareStatement(sqlInsertar)) {
					insertar.setInt(1, idEmpleado);
					insertar.setString(2, despuesTipo);
					insertar.setString(3, fecha);
					insertar.setString(4, despuesFechaHora);
					insertar.setInt(5, idTienda);
					insertar.setString(6, BiometriaNovedadDAO.INTERVENCION_MANUAL);
					insertar.executeUpdate();
				}
			}

			final String sqlLog = " INSERT INTO general.empleado_evento_log "
					+ " (idnovedad, id, fecha, idtienda, accion, antes_tipo, antes_fecha_hora_log, "
					+ "  antes_uso_biometria, despues_tipo, despues_fecha_hora_log, "
					+ "  despues_uso_biometria, usuario, observacion) "
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
			try (PreparedStatement log = con1.prepareStatement(sqlLog)) {
				if (idNovedad > 0) {
					log.setInt(1, idNovedad);
				} else {
					log.setNull(1, java.sql.Types.INTEGER);
				}
				log.setInt(2, idEmpleado);
				log.setString(3, fecha);
				log.setInt(4, idTienda);
				log.setString(5, accion);
				ponerTextoONulo(log, 6, tocaBorrar ? antesTipo : null);
				ponerTextoONulo(log, 7, tocaBorrar ? antesFechaHora : null);
				ponerTextoONulo(log, 8, tocaBorrar ? usoBiometriaAntes : null);
				ponerTextoONulo(log, 9, tocaInsertar ? despuesTipo : null);
				ponerTextoONulo(log, 10, tocaInsertar ? despuesFechaHora : null);
				ponerTextoONulo(log, 11, tocaInsertar ? BiometriaNovedadDAO.INTERVENCION_MANUAL : null);
				log.setString(12, usuario);
				ponerTextoONulo(log, 13, observacion);
				log.executeUpdate();
			}

			con1.commit();
			return ("");
		} catch (final Exception e) {
			System.out.println("BiometriaNovedadDAO.aplicarCambio: " + e);
			e.printStackTrace();
			try {
				if (con1 != null) {
					con1.rollback();
				}
			} catch (final Exception e1) {
				System.out.println("Fallo el rollback: " + e1);
			}
			return ("No se pudo aplicar el cambio: " + e.getMessage());
		} finally {
			try {
				if (con1 != null) {
					con1.setAutoCommit(true);
					con1.close();
				}
			} catch (final Exception e1) {
				System.out.println("Fallo cerrando la conexion: " + e1);
			}
		}
	}

	/** Cierra una novedad. estado debe ser ATENDIDA o RECHAZADA. */
	public static String cerrarNovedad(final int idNovedad, final String estado, final String usuario,
			final String observacion) {
		if (!"ATENDIDA".equals(estado) && !"RECHAZADA".equals(estado)) {
			return ("El estado debe ser ATENDIDA o RECHAZADA.");
		}
		final String sql = " UPDATE general.empleado_evento_novedad "
				+ " SET estado = ?, resuelto_por = ?, fecha_resolucion = NOW(), "
				+ "     observacion_resolucion = ? "
				+ " WHERE idnovedad = ? AND estado = 'REPORTADA' ";
		final ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDGeneral();
			 PreparedStatement pstmt = con1.prepareStatement(sql)) {
			pstmt.setString(1, estado);
			pstmt.setString(2, usuario);
			ponerTextoONulo(pstmt, 3, observacion);
			pstmt.setInt(4, idNovedad);
			if (pstmt.executeUpdate() == 0) {
				return ("La novedad no existe o ya habia sido resuelta.");
			}
			return ("");
		} catch (final Exception e) {
			System.out.println("BiometriaNovedadDAO.cerrarNovedad: " + e);
			e.printStackTrace();
			return ("No se pudo cerrar la novedad: " + e.getMessage());
		}
	}

	private static boolean vacio(final String valor) {
		return (valor == null || valor.trim().length() == 0);
	}
}
