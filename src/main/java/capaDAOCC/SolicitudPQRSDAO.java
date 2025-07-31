package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaModeloCC.ComentarioPqrs;
import capaModeloCC.EncuestaPqrs;
import capaModeloCC.EstadoPqrs;
import capaModeloCC.FormaPago;
import capaModeloCC.MotivoPqrs;
import capaModeloCC.Pedido;
import capaModeloCC.PendientePqrs;
import capaModeloCC.PrioridadPqrs;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.Tienda;
import capaModeloCC.Usuario;
import capaModeloCC.EncuestaPqrs.RespuestaEncuestaPqrs;
import conexionCC.ConexionBaseDatos;

public class SolicitudPQRSDAO {

	/**
	 * M�todo de la capa DAO que se encarga de implementar la inserci�n de la
	 * entidad solicitudPQRS, recibiendo como par�metro un objeto tipo
	 * SolicitudPQRS, retornar� como resultado de la inserci�n el idsolicitudPQRS
	 * asignado por la base de datos en base a un campo configurado como
	 * autoincrementable en la misma.
	 * 
	 * @param solicitud Se recibe como par�metro un objeto de la capaModelo
	 *                  SolicitudPQRS
	 * @return Se retorna valor intero con el idSolicitudPQRS asignado por la base
	 *         de datos.
	 */
	public static int insertarSolicitudPQRS(SolicitudPQRS solicitud) {
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal;
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaSolicitudFinal;
		String correo = solicitud.getCorreo().isEmpty() ? null : solicitud.getCorreo();
		try {
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(solicitud.getFechaSolicitud());
			fechaSolicitudFinal = formatoFinal.format(fechaTemporal);
		} catch (Exception e) {
			logger.error(e.toString());
			return 0;
		}

		String insert = "INSERT INTO solicitudPQRS (fechasolicitud, tiposolicitud, idcliente, idtienda, nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idorigen, idfoco, tipo, area_responsable, idpedidotienda, valor_pedido, valor_descuento, porcentaje_descuento, descuento_redimido, idpedidoredencion, id_usuario_registro, id_usuario_redencion, idestado, idprioridad, idmotivo, cc_vinculado, correo) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement pstmt = con1.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, fechaSolicitudFinal);
			pstmt.setString(2, solicitud.getTipoSolicitud());
			pstmt.setInt(3, solicitud.getIdcliente());
			pstmt.setInt(4, solicitud.getIdtienda());
			pstmt.setString(5, solicitud.getNombres());
			pstmt.setString(6, solicitud.getApellidos());
			pstmt.setString(7, solicitud.getTelefono());
			pstmt.setString(8, solicitud.getDireccion());
			pstmt.setString(9, solicitud.getZona());
			pstmt.setInt(10, solicitud.getIdmunicipio());
			pstmt.setString(11, solicitud.getComentario());
			pstmt.setInt(12, solicitud.getIdOrigen());
			pstmt.setInt(13, solicitud.getIdFoco());
			pstmt.setString(14, solicitud.getTipo());
			pstmt.setString(15, solicitud.getAreaResponsable());
			pstmt.setInt(16, solicitud.getIdpedidotienda());
			pstmt.setDouble(17, solicitud.getValorPedido());
			pstmt.setDouble(18, solicitud.getValorDescuento());
			pstmt.setDouble(19, solicitud.getPorcentajeDescuento());
			pstmt.setInt(20, solicitud.isDescuentoRedimido() ? 1 : 0);
			pstmt.setInt(21, solicitud.getIdpedidoredencion());
			pstmt.setInt(22, solicitud.getIdusuarioRegistro());
			pstmt.setInt(23, solicitud.getIdusuarioRedencion());
			pstmt.setInt(24, solicitud.getIdestado());
			pstmt.setInt(25, solicitud.getIdprioridad());
			pstmt.setInt(26, solicitud.getIdmotivo());
			pstmt.setInt(27, solicitud.isCcVinculado() ? 1 : 0);
			pstmt.setString(28, correo);

			logger.info("Ejecutando: " + pstmt.toString());
			pstmt.executeUpdate();

			try (ResultSet rs = pstmt.getGeneratedKeys()) {
				if (rs.next()) {
					idSolicitudPQRSIns = rs.getInt(1);
					logger.info("Id SolicitudPQRS insertada en bd " + idSolicitudPQRSIns);
				}
			}
		} catch (Exception e) {
			logger.error("Error al insertar PQRS: " + e.toString());
			return 0;
		} finally {
			try {
				if (con1 != null && !con1.isClosed()) {
					con1.close();
				}
			} catch (Exception e) {
				logger.error("Error al cerrar la conexión: " + e.toString());
			}
		}

		return idSolicitudPQRSIns;
	}

	/**
	 * M�todo que se encarga de la actualizaci�n de una PQRS
	 * 
	 * @param solicitud
	 * @return
	 */
	public static int actualizarSolicitudPQRS(SolicitudPQRS solicitud) {
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSIns = solicitud.getIdsolicitud();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal;
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String fechaSolicitudFinal;

		int descuentoRedimido = solicitud.isDescuentoRedimido() ? 1 : 0;
		int ccVinculado = solicitud.isCcVinculado() ? 1 : 0;
		String correo = solicitud.getCorreo().isEmpty() ? null : solicitud.getCorreo();

		try {
			fechaTemporal = new SimpleDateFormat("dd/MM/yyyy").parse(solicitud.getFechaSolicitud());
			fechaSolicitudFinal = formatoFinal.format(fechaTemporal);
		} catch (Exception e) {
			logger.error(e.toString());
			return 0;
		}

		String update = "UPDATE solicitudPQRS SET "
				+ "fechasolicitud = ?, tiposolicitud = ?, idcliente = ?, idtienda = ?, nombres = ?, apellidos = ?, telefono = ?, direccion = ?, zona = ?, idmunicipio = ?, comentario = ?, "
				+ "idorigen = ?, idfoco = ?, tipo = ?, area_responsable = ?, idpedidotienda = ?, valor_pedido = ?, valor_descuento = ?, porcentaje_descuento = ?, descuento_redimido = ?, "
				+ "idpedidoredencion = ?, id_usuario_registro = ?, id_usuario_redencion = ?, idestado = ?, idprioridad = ?, idmotivo = ? ,cc_vinculado = ? , correo = ? "
				+ "WHERE idsolicitudpqrs = ?";

		try (PreparedStatement pstmt = con1.prepareStatement(update)) {
			pstmt.setString(1, fechaSolicitudFinal);
			pstmt.setString(2, solicitud.getTipoSolicitud());
			pstmt.setInt(3, solicitud.getIdcliente());
			pstmt.setInt(4, solicitud.getIdtienda());
			pstmt.setString(5, solicitud.getNombres());
			pstmt.setString(6, solicitud.getApellidos());
			pstmt.setString(7, solicitud.getTelefono());
			pstmt.setString(8, solicitud.getDireccion());
			pstmt.setString(9, solicitud.getZona());
			pstmt.setInt(10, solicitud.getIdmunicipio());
			pstmt.setString(11, solicitud.getComentario());
			pstmt.setInt(12, solicitud.getIdOrigen());
			pstmt.setInt(13, solicitud.getIdFoco());
			pstmt.setString(14, solicitud.getTipo());
			pstmt.setString(15, solicitud.getAreaResponsable());
			pstmt.setInt(16, solicitud.getIdpedidotienda());
			pstmt.setDouble(17, solicitud.getValorPedido());
			pstmt.setDouble(18, solicitud.getValorDescuento());
			pstmt.setDouble(19, solicitud.getPorcentajeDescuento());
			pstmt.setInt(20, descuentoRedimido);
			pstmt.setInt(21, solicitud.getIdpedidoredencion());
			pstmt.setInt(22, solicitud.getIdusuarioRegistro());
			pstmt.setInt(23, solicitud.getIdusuarioRedencion());
			pstmt.setInt(24, solicitud.getIdestado());
			pstmt.setInt(25, solicitud.getIdprioridad());
			pstmt.setInt(26, solicitud.getIdmotivo());
			pstmt.setInt(27, ccVinculado);
			pstmt.setString(28, correo);
			pstmt.setInt(29, idSolicitudPQRSIns);

			logger.info("Ejecutando: " + pstmt.toString());
			pstmt.executeUpdate();
		} catch (Exception e) {
			logger.error("Error al actualizar PQRS: " + e.toString());
			return 0;
		} finally {
			try {
				if (con1 != null && !con1.isClosed()) {
					con1.close();
				}
			} catch (Exception e) {
				logger.error("Error al cerrar la conexión: " + e.toString());
			}
		}

		return idSolicitudPQRSIns;
	}

	/**
	 * M�todo que se encarga desde la base de datos del descarte de la solicitu de
	 * PQRS.
	 * 
	 * @param idSolicitudPQRS
	 * @return
	 */
	public static int descartarSolicitudPQRS(int idSolicitudPQRS) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String delete = "";
		try {
			Statement stm = con1.createStatement();
			delete = "delete from solicitudPQRS  where idsolicitudpqrs =" + idSolicitudPQRS;
			logger.info(delete);
			stm.executeUpdate(delete);
			stm.close();
			con1.close();
		} catch (Exception e) {
			System.out.println(delete + " " + e.toString());
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (0);
		}
		return (idSolicitudPQRS);
	}

	public static boolean adicionarComentarioPQRS(int idSolicitudPQRS, String comentario) {
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		Date fechaTemporal = new Date();
		comentario = " ///" + fechaTemporal.toString() + " " + comentario;
		try {
			Statement stm = con1.createStatement();
			String update = "update solicitudPQRS set comentario = concat( comentario , ' " + comentario
					+ "') where idsolicitudPQRS = " + idSolicitudPQRS;
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
			respuesta = true;
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (respuesta);
		}
		return (respuesta);
	}

	public static ArrayList<SolicitudPQRS> ConsultaIntegradaSolicitudesPQRS(String fechainicial, String fechafinal,
			String tienda, String tipoSolicitud, boolean filtrodescuentoRed) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SolicitudPQRS> consultaSolicitudes = new ArrayList<>();

		String fechaini = fechainicial.substring(6, 10) + "-" + fechainicial.substring(3, 5) + "-"
				+ fechainicial.substring(0, 2);
		String fechafin = fechafinal.substring(6, 10) + "-" + fechafinal.substring(3, 5) + "-"
				+ fechafinal.substring(0, 2);

		StringBuilder consulta = new StringBuilder(
				"SELECT a.idsolicitudPQRS, a.fechasolicitud, a.tiposolicitud, a.nombres, a.apellidos, a.direccion,a.zona, a.idfoco, "
						+ "a.telefono, a.comentario, a.idorigen, b.nombre_origen, a.idmunicipio, a.idtienda, c.nombre_foco, a.idcliente,a.cc_vinculado, a.correo, "
						+ "a.tipo, a.area_responsable, "
						+ "(SELECT COUNT(*) FROM solicitudpqrs_imagenes d WHERE d.idsolicitudPQRS = a.idsolicitudPQRS) AS imagenes, "
						+ "a.idpedidotienda, a.valor_pedido, a.valor_descuento, a.porcentaje_descuento, a.descuento_redimido, a.idpedidoredencion , a.id_usuario_registro , a.id_usuario_redencion ,a.idestado , a.idprioridad ,a.idmotivo, e.descripcion as nombreEstado "
						+ "FROM solicitudPQRS a " + "JOIN origen_pqrs b ON a.idorigen = b.idorigen "
						+ "JOIN foco_pqrs c ON a.idfoco = c.idfoco "
						+ "LEFT JOIN  estado_pqrs e ON a.idestado = e.idestado "
						+ "WHERE a.fechasolicitud BETWEEN ? AND ? ");

		// Filtros dinámicos
		if (!"TODAS".equalsIgnoreCase(tienda)) {
			consulta.append("AND a.idtienda = ? ");
		}
		if (!tipoSolicitud.trim().isEmpty()) {
			consulta.append("AND a.tiposolicitud = ? ");
		}
		if (filtrodescuentoRed) {
			consulta.append("AND a.descuento_redimido = 1 ");
		}

		logger.info("Consulta SQL: " + consulta.toString());

		ConexionBaseDatos con = new ConexionBaseDatos();
		try (Connection con1 = con.obtenerConexionBDPrincipal();
				PreparedStatement ps = con1.prepareStatement(consulta.toString())) {

			int paramIndex = 1;
			ps.setString(paramIndex++, fechaini);
			ps.setString(paramIndex++, fechafin);

			if (!"TODAS".equalsIgnoreCase(tienda)) {
				ps.setInt(paramIndex++, Integer.parseInt(tienda));
			}
			if (!tipoSolicitud.trim().isEmpty()) {
				ps.setString(paramIndex++, tipoSolicitud);
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SolicitudPQRS cadaSolicitud = new SolicitudPQRS(rs.getInt("idsolicitudPQRS"),
							rs.getString("fechasolicitud"), rs.getString("tiposolicitud"), rs.getInt("idcliente"),
							rs.getInt("idtienda"), rs.getString("nombres"), rs.getString("apellidos"),
							rs.getString("telefono"), rs.getString("direccion"), rs.getString("zona"),
							rs.getInt("idmunicipio"), rs.getString("comentario"), rs.getInt("idorigen"),
							rs.getInt("idfoco"), rs.getString("tipo"), rs.getString("area_responsable"),
							rs.getInt("idpedidotienda"), rs.getDouble("valor_pedido"), rs.getDouble("valor_descuento"),
							rs.getInt("porcentaje_descuento"), rs.getBoolean("descuento_redimido"),
							rs.getInt("idpedidoredencion"), rs.getInt("id_usuario_registro"),
							rs.getInt("id_usuario_redencion"), rs.getInt("idestado"), rs.getInt("idprioridad"),
							rs.getInt("idmotivo"), rs.getBoolean("cc_vinculado"), rs.getString("correo")

					);
					cadaSolicitud.setOrigen(rs.getString("nombre_origen"));
					cadaSolicitud.setIdmunicipio(rs.getInt("idmunicipio"));
					cadaSolicitud.setIdtienda(rs.getInt("idtienda"));
					cadaSolicitud.setFoco(rs.getString("nombre_foco"));
					cadaSolicitud.setImagenes(rs.getInt("imagenes"));
					cadaSolicitud.setNombreEstado(rs.getString("nombreEstado"));
					consultaSolicitudes.add(cadaSolicitud);

				}
			}

		} catch (Exception e) {
			logger.error("Error al consultar PQRS: " + e.toString(), e);
			System.out.println("Error al consultar PQRS: " + e.toString());
		}

		return consultaSolicitudes;
	}


	/**
	 * Método construido especificamente para retornar las PQRS para el reporte diario de las PQRS construido para llegar automaticamente
	 * todas las noches
	 * @param fecha
	 * @return
	 */
	public static ArrayList<SolicitudPQRS> ConsultaSolicitudesReportePQRS(String fecha) {
	    Logger logger = Logger.getLogger("log_file");
	    ArrayList<SolicitudPQRS> consultaSolicitudes = new ArrayList<>();

	    StringBuilder consulta = new StringBuilder(
	        "SELECT a.idsolicitudPQRS, a.fechasolicitud, a.tiposolicitud, a.nombres, a.apellidos, a.direccion,a.zona, a.idfoco, " +
	        "a.telefono, a.comentario, a.idorigen, b.nombre_origen, a.idmunicipio, a.idtienda, c.nombre_foco, a.idcliente,a.cc_vinculado," +
	        "a.tipo, a.area_responsable, a.correo , " +
	        "(SELECT COUNT(*) FROM solicitudpqrs_imagenes d WHERE d.idsolicitudPQRS = a.idsolicitudPQRS) AS imagenes, " +
	        "a.idpedidotienda, a.valor_pedido, a.valor_descuento, a.porcentaje_descuento, a.descuento_redimido, a.idpedidoredencion , a.id_usuario_registro , a.id_usuario_redencion ,a.idestado , a.idprioridad ,a.idmotivo, e.descripcion as nombreEstado, f.nombre as nombretienda, g.descripcion as prioridad " +
	        "FROM solicitudPQRS a " +
	        "JOIN origen_pqrs b ON a.idorigen = b.idorigen " +
	        "JOIN foco_pqrs c ON a.idfoco = c.idfoco " +
	        "JOIN tienda f ON a.idtienda = f.idtienda " +
	        "JOIN prioridad_pqrs g ON a.idprioridad = g.idprioridad " +
	        "LEFT JOIN  estado_pqrs e ON a.idestado = e.idestado " +
	        "WHERE a.fechasolicitud = ? and a.idestado != 1"
	    );


	    logger.info("Consulta SQL: " + consulta.toString());

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    try (Connection con1 = con.obtenerConexionBDPrincipal();
	         PreparedStatement ps = con1.prepareStatement(consulta.toString())) {

	        int paramIndex = 1;
	        ps.setString(1, fecha);


	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                SolicitudPQRS cadaSolicitud = new SolicitudPQRS(
	                    rs.getInt("idsolicitudPQRS"),
	                    rs.getString("fechasolicitud"),
	                    rs.getString("tiposolicitud"),
	                    rs.getInt("idcliente"),
	                    rs.getInt("idtienda"),
	                    rs.getString("nombres"),
	                    rs.getString("apellidos"),
	                    rs.getString("telefono"),
	                    rs.getString("direccion"),
	                    rs.getString("zona"),
	                    rs.getInt("idmunicipio"),
	                    rs.getString("comentario"),
	                    rs.getInt("idorigen"),
	                    rs.getInt("idfoco"),
	                    rs.getString("tipo"),
	                    rs.getString("area_responsable"),
	                    rs.getInt("idpedidotienda"),
	                    rs.getDouble("valor_pedido"),
	                    rs.getDouble("valor_descuento"),
	                    rs.getInt("porcentaje_descuento"),
	                    rs.getBoolean("descuento_redimido"),
	                    rs.getInt("idpedidoredencion"),
	                    rs.getInt("id_usuario_registro"),
	                    rs.getInt("id_usuario_redencion"),
	                    rs.getInt("idestado"),
	                    rs.getInt("idprioridad"),
	                    rs.getInt("idmotivo"),
	                    rs.getBoolean("cc_vinculado"),
	                    rs.getString("correo")
	                );
	                cadaSolicitud.setOrigen(rs.getString("nombre_origen"));
	                cadaSolicitud.setIdmunicipio(rs.getInt("idmunicipio"));
	                cadaSolicitud.setIdtienda(rs.getInt("idtienda"));
	                cadaSolicitud.setFoco(rs.getString("nombre_foco"));
	                cadaSolicitud.setImagenes(rs.getInt("imagenes"));
	                cadaSolicitud.setNombreEstado(rs.getString("nombreEstado"));
	                cadaSolicitud.setTienda(rs.getString("nombretienda"));
	                cadaSolicitud.setPrioridad(rs.getString("prioridad"));
	                consultaSolicitudes.add(cadaSolicitud);

	                
	            }
	        }

	    } catch (Exception e) {
	        logger.error("Error al consultar PQRS: " + e.toString(), e);
	        System.out.println("Error al consultar PQRS: " + e.toString());
	    }

	    return consultaSolicitudes;
	}
	
	/**
	 * M�todo que se encarga de validar si una PQRS existe o no, retornando esto
	 * como un valor booleano
	 * 
	 * @param PQRS
	 * @param idCliente
	 * @return
	 */
	public static boolean validarPQRS(int PQRS, int idCliente) {
		Logger logger = Logger.getLogger("log_file");
		String consulta = "select * from solicitudPQRS where idCliente = " + idCliente + " and idsolicitudPQRS = "
				+ PQRS;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);

			while (rs.next()) {
				respuesta = true;
				break;
			}
			rs.close();
			stm.close();
			con1.close();

		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}

		}
		return (respuesta);
	}

	public static ArrayList<Integer> obtenerPQRSCliente(int idCliente) {
		Logger logger = Logger.getLogger("log_file");
		String consulta = "select idsolicitudPQRS from solicitudPQRS where idCliente = " + idCliente;
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Integer> pqrsCliente = new ArrayList();
		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int pqrs = 0;
			while (rs.next()) {
				pqrs = rs.getInt("idsolicitudPQRS");
				pqrsCliente.add(Integer.valueOf(pqrs));
			}
			rs.close();
			stm.close();
			con1.close();

		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}

		}
		return (pqrsCliente);
	}

	/**
	 * Obtener la cantidad de PQRS asociadas a un cliente en específico
	 * 
	 * @param telefono
	 * @return
	 */
	public static int obtenerCantidadPQRS(String telefono) {
		Logger logger = Logger.getLogger("log_file");
		int cantidadPQRS = 0;
		String consulta = "";
		consulta = "select  count(*) from solicitudPQRS a, cliente b WHERE a.idcliente = b.idcliente and (b.telefono = '"
				+ telefono + "')";
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();

		try {
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				cantidadPQRS = rs.getInt(1);
				break;
			}
			rs.close();
			stm.close();
			con1.close();

		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}

		}
		return (cantidadPQRS);
	}

	public static boolean modificarComentariosPqrs(int idSolicitudPqrs, List<ComentarioPqrs> listaComentarios) {
		if (idSolicitudPqrs == 0 || listaComentarios == null) {
			return false;
		}

		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();

		String sqlInsert = "INSERT INTO pqrs_comentario (idsolicitud_pqrs, comentario, fecha_comentario) VALUES (?, ?, ?)";
		String sqlUpdate = "UPDATE pqrs_comentario SET comentario = ?, fecha_comentario = ? WHERE idpqrs_comentario = ?";
		String sqlDelete = "DELETE FROM pqrs_comentario WHERE idpqrs_comentario = ?";

		try (Connection conn = con1) {
			conn.setAutoCommit(false); // Transacción

			try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert);
					PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate);
					PreparedStatement stmtDelete = conn.prepareStatement(sqlDelete)) {

				for (ComentarioPqrs comentario : listaComentarios) {
					// Convertir fecha String a java.sql.Date
					java.sql.Date fechaSql = null;
					try {
						fechaSql = java.sql.Date.valueOf(comentario.getFecha()); // Espera "yyyy-MM-dd"
					} catch (IllegalArgumentException e) {
						System.err.println("Fecha inválida: " + comentario.getFecha());
						conn.rollback();
						return false;
					}

					if (comentario.isEstado()) {
						if (comentario.getId() == 0) {
							// Insertar
							stmtInsert.setInt(1, idSolicitudPqrs);
							stmtInsert.setString(2, comentario.getComentario());
							stmtInsert.setDate(3, fechaSql);
							stmtInsert.addBatch();
						} else {
							// Actualizar
							stmtUpdate.setString(1, comentario.getComentario());
							stmtUpdate.setDate(2, fechaSql);
							stmtUpdate.setInt(3, comentario.getId());
							stmtUpdate.addBatch();
						}
					} else {
						if (comentario.getId() > 0) {
							stmtDelete.setInt(1, comentario.getId());
							stmtDelete.addBatch();
						}

					}

				}

				int[] insertResults = stmtInsert.executeBatch();
				int[] updateResults = stmtUpdate.executeBatch();
				int[] deleteResults = stmtDelete.executeBatch(); // <-- FALTA ESTO

				for (int res : insertResults) {
					if (res == PreparedStatement.EXECUTE_FAILED) {
						conn.rollback();
						return false;
					}
				}
				for (int res : updateResults) {
					if (res == PreparedStatement.EXECUTE_FAILED) {
						conn.rollback();
						return false;
					}
				}
				for (int res : deleteResults) {
					if (res == PreparedStatement.EXECUTE_FAILED) {
						conn.rollback();
						return false;
					}
				}

				conn.commit();

				return true;

			} catch (SQLException e) {
				conn.rollback();
				System.err.println("Error en inserción/actualización comentarios: " + e.getMessage());
				return false;
			} finally {
				conn.setAutoCommit(true);
			}

		} catch (SQLException e) {
			System.err.println("Error en conexión BD: " + e.getMessage());
			return false;
		}
	}

	public static Map<String, List<ComentarioPqrs>> obtenerComentariosPqrs(int idSolicitudPqrs) {
		Map<String, List<ComentarioPqrs>> comentariosPorFecha = new LinkedHashMap<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();

		String sql = "SELECT idpqrs_comentario, idsolicitud_pqrs, comentario, fecha_comentario "
				+ "FROM pqrs_comentario WHERE idsolicitud_pqrs = ? ORDER BY fecha_comentario ASC, idpqrs_comentario ASC";

		try (Connection conn = con1; PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, idSolicitudPqrs);
			ResultSet rs = stmt.executeQuery();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			while (rs.next()) {
				String fechaStr = sdf.format(rs.getDate("fecha_comentario"));

				ComentarioPqrs comentario = new ComentarioPqrs(rs.getInt("idpqrs_comentario"),
						rs.getInt("idsolicitud_pqrs"), rs.getString("comentario"), fechaStr);

				comentariosPorFecha.computeIfAbsent(fechaStr, k -> new ArrayList<>()).add(comentario);
			}

		} catch (SQLException e) {
			System.err.println("Error al obtener comentarios: " + e.getMessage());
		}

		return comentariosPorFecha;
	}

	/**
	 * Método que se encarga de traer un string con los comentarios de la pqrs
	 * @param idSolicitudPqrs
	 * @return
	 */
	public static String obtenerComentariosStrPqrs(int idSolicitudPqrs) {
	    String respuesta = "";
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    String sql = "SELECT comentario, fecha_comentario " +
	                 "FROM pqrs_comentario WHERE idsolicitud_pqrs = ? ORDER BY fecha_comentario ASC, idpqrs_comentario ASC";

	    try (Connection conn = con1; PreparedStatement stmt = conn.prepareStatement(sql)) {
	        stmt.setInt(1, idSolicitudPqrs);
	        ResultSet rs = stmt.executeQuery();

	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	        String fechaStr = "";
	        String comentario = "";
	        while (rs.next()) {
	            fechaStr = sdf.format(rs.getDate("fecha_comentario"));
	            comentario = rs.getString("comentario");
	            respuesta = respuesta + "\\n" + fechaStr + "-" + comentario;
	        }

	    } catch (SQLException e) {
	        System.err.println("Error al obtener comentarios: " + e.getMessage());
	    }

	    return respuesta;
	}
	
	public static List<EstadoPqrs> obtenerEstadoPqrs() {
		List<EstadoPqrs> ListaEstados = new ArrayList<>();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = con.obtenerConexionBDPrincipal();

		String consulta = "SELECT idestado,descripcion  FROM estado_pqrs";

		try (PreparedStatement stmt = conn.prepareStatement(consulta); ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				EstadoPqrs estado = new EstadoPqrs();
				estado.setIdestado(rs.getInt("idestado"));
				estado.setDescripcion(rs.getString("descripcion"));
				ListaEstados.add(estado);
			}
		} catch (SQLException e) {
			logger.info("Error al consultar estados pqrs: " + e.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.info("Error al cerrar conexión: " + e.getMessage());
			}
		}

		return ListaEstados;

	}

	public static List<MotivoPqrs> obtenerMotivosPqrs() {
		List<MotivoPqrs> ListaMotivos = new ArrayList<>();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = con.obtenerConexionBDPrincipal();

		String consulta = "SELECT * FROM motivo_pqrs";

		try (PreparedStatement stmt = conn.prepareStatement(consulta); ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				MotivoPqrs motivo = new MotivoPqrs();
				motivo.setIdmotivo(rs.getInt("idmotivo"));
				motivo.setDescripcion(rs.getString("descripcion"));
				motivo.setIdprioridad(rs.getInt("idprioridad"));
				motivo.setTiposolicitud(rs.getString("tiposolicitud"));

				ListaMotivos.add(motivo);
			}
		} catch (SQLException e) {
			logger.info("Error al consultar motivos pqrs: " + e.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.info("Error al cerrar conexión: " + e.getMessage());
			}
		}

		return ListaMotivos;
	}

	public static List<PrioridadPqrs> obtenerPrioridadPqrs() {
		List<PrioridadPqrs> ListaPrioridad = new ArrayList<>();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = con.obtenerConexionBDPrincipal();

		String consulta = "SELECT * FROM prioridad_pqrs";

		try (PreparedStatement stmt = conn.prepareStatement(consulta); ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				PrioridadPqrs prioridad = new PrioridadPqrs();
				prioridad.setIdprioridad(rs.getInt("idprioridad"));
				prioridad.setDescripcion(rs.getString("descripcion"));
				prioridad.setT_resp_min(rs.getInt("t_resp_min"));
				prioridad.setT_resp_max(rs.getInt("t_resp_max"));
				ListaPrioridad.add(prioridad);
			}
		} catch (SQLException e) {
			logger.info("Error al consultar prioridad pqrs: " + e.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.info("Error al cerrar conexión: " + e.getMessage());
			}
		}

		return ListaPrioridad;

	}

	public static List<PendientePqrs> obtenerPendientePqrs(String fecha) {
		List<PendientePqrs> pqrspendientes = new ArrayList<>();
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = con.obtenerConexionBDPrincipal();

		// Si la fecha es nula o vacía, se usa la fecha actual
		if (fecha == null || fecha.trim().isEmpty()) {
			fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		}
		String consultaSolicitudes = "SELECT s.idsolicitudPQRS, s.tiposolicitud, s.nombres , o.nombre_origen FROM solicitudPQRS s INNER JOIN origen_pqrs o ON s.idorigen = o.idorigen WHERE s.idestado = 1 AND s.fechasolicitud = ?";
		String consultaComentarios = "SELECT comentario, fecha_comentario FROM pqrs_comentario WHERE idsolicitud_pqrs = ?";

		try (PreparedStatement stmtSolicitudes = conn.prepareStatement(consultaSolicitudes);) {
			stmtSolicitudes.setString(1, fecha);
			try (ResultSet rsSolicitudes = stmtSolicitudes.executeQuery()) {
				while (rsSolicitudes.next()) {
					PendientePqrs solicitud = new PendientePqrs();
					solicitud.setIdsolicitud(rsSolicitudes.getInt("idsolicitudPQRS"));
					solicitud.setTiposolicitud(rsSolicitudes.getString("tiposolicitud"));
					solicitud.setNombres(rsSolicitudes.getString("nombres"));
					solicitud.setNombre_origen(rsSolicitudes.getString("nombre_origen"));

					List<ComentarioPqrs> listaComentarios = new ArrayList<>();
					try (PreparedStatement stmtComentarios = conn.prepareStatement(consultaComentarios)) {
						stmtComentarios.setInt(1, solicitud.getIdsolicitud());
						try (ResultSet rsComentarios = stmtComentarios.executeQuery()) {
							while (rsComentarios.next()) {
								ComentarioPqrs c = new ComentarioPqrs();
								c.setComentario(rsComentarios.getString("comentario"));
								c.setFecha(rsComentarios.getString("fecha_comentario"));
								listaComentarios.add(c);
							}
						}
					}

					solicitud.setComentarios(listaComentarios);
					pqrspendientes.add(solicitud);
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			logger.info("Error al consultar prioridad pqrs: " + e.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.info("Error al cerrar conexión: " + e.getMessage());
			}
		}

		return pqrspendientes;
	}
	
	// Método para insertar registros en la tabla encuesta_pqrs
    public static void insertarEncuestaPqrs(EncuestaPqrs encuestapqrs) {
        String sql = "INSERT INTO encuesta_pqrs (idpregunta, respuesta, idsolicitud_pqrs) VALUES (?, ?, ?)";
        ConexionBaseDatos con = new ConexionBaseDatos();
        Connection con1 = null;

        try {
            con1 = con.obtenerConexionBDPrincipal();  // Obtener la conexión una vez
            
            Integer idpqrs = encuestapqrs.getIdpqrs();
            for (RespuestaEncuestaPqrs value : encuestapqrs.getRespuesta()) {
                Integer idPregunta = obtenerIdPorTitulo(value.getDescripcion(), con1);
                
                if (idPregunta == null) {
                    System.err.println("No se encontró la pregunta con el título: " + value.getDescripcion());
                    continue;  // Saltamos este registro y continuamos con los demás
                }

                // Usamos el PreparedStatement dentro del bloque try-with-resources
                try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
                    pstmt.setInt(1, idPregunta);  
                    pstmt.setString(2, value.getRespuesta());
                    pstmt.setInt(3, idpqrs);

                    pstmt.executeUpdate();
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Error al insertar el registo en encuestapqrs: " + value.getDescripcion());
                    // Aquí puedes decidir si detener el proceso o continuar con los demás registros
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Aquí manejas el error de conexión general si es necesario

        } finally {
            if (con1 != null) {
                try {
                    con1.close(); // Cerramos la conexión
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // Método para obtener el ID de una pregunta por su título
    public static Integer obtenerIdPorTitulo(String titulo, Connection con1) throws SQLException {
        String sql = "SELECT idpregunta FROM pregunta_pqrs WHERE titulo = ?";
        Integer idPregunta = null;

        try (PreparedStatement pstmt = con1.prepareStatement(sql)) {
            pstmt.setString(1, titulo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    idPregunta = rs.getInt("idpregunta");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Relanzamos la excepción para que sea manejada adecuadamente
        }

        return idPregunta;
    }

}
