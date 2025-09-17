package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import capaModeloCC.ClienteClub;
import capaModeloCC.ClienteSegmento;
import capaModeloCC.PlantillaBrevo;
import conexionCC.ConexionBaseDatos;

public class SegmentacionClienteDAO {

	public List<PlantillaBrevo> obtenerPlantillas() {
		List<PlantillaBrevo> plantillas = new ArrayList<>();
		String sql = "SELECT idplantilla, nombre ,categoria FROM plantilla_brevo";
		Connection con1 = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDPrincipal(); // Obtener la conexión

			ps = con1.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next()) {
				PlantillaBrevo plantilla = new PlantillaBrevo();
				plantilla.setIdPlantilla(rs.getInt("idplantilla"));
				plantilla.setNombre(rs.getString("nombre"));
				plantilla.setCategoria(rs.getString("categoria"));
				plantillas.add(plantilla);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (con1 != null)
					con1.close(); // Cerrar conexión aquí
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return plantillas;
	}

	public List<ClienteSegmento> obtenerClientesFiltrados(String fechaInicio, String fechaMaxima, int minPedidos,
			List<Integer> excepciones, List<Integer> idTiendas, int diasMinimosSinPublicidad, String canal, String tipoCliente) {

		List<ClienteSegmento> clientes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String sql = "SELECT b.idcliente, " + "       CONCAT(b.nombre, ' ', b.apellido) AS nombre, b.nombrecompania, "
				+ "       b.telefono, c.nombre AS nombretienda, "
				+ "       COUNT(*) AS numeropedidos, MAX(a.fechapedido) AS fechamaxima, " + "       b.email "
				+ "FROM pedido a " + "JOIN cliente b ON a.idcliente = b.idcliente "
				+ "JOIN tienda c ON b.idtienda = c.idtienda " + "JOIN detalle_pedido d ON a.idpedido = d.idpedido "
				+ "WHERE c.funcional != 'N' ";

		// Agregar condici�n para idTiendas
		if (idTiendas != null && !idTiendas.isEmpty()) {
			String placeholders = String.join(",", Collections.nCopies(idTiendas.size(), "?"));
			sql += "AND c.idtienda IN (" + placeholders + ") ";
		}

		// Agregar condici�n para excepciones
		if (excepciones != null && !excepciones.isEmpty()) {
			String placeholders = String.join(",", Collections.nCopies(excepciones.size(), "?"));
			sql += "AND d.idexcepcion IN (" + placeholders + ") ";
		}

		sql += "AND b.email != '' " + "AND b.politica_datos = 'S' " + "AND b.envio_publicidad = 'S' ";

		// Aqu� viene lo importante:
		if (diasMinimosSinPublicidad > 0) {
			sql += "AND (b.ultima_fecha_publicidad IS NULL OR b.ultima_fecha_publicidad <= CURRENT_DATE - INTERVAL ? DAY) ";
		}
		
		if(!canal.equals(new String("")))
		{
			sql += "AND a.origen = '" + canal + "' ";
		}
		
		//Agregamos la parte de filtro de tipoCliente
		
		if(!tipoCliente.equals(new String("")))
		{
			if(tipoCliente.equals(new String("natural")))
			{
				sql += "AND b.idtipopersona = 2 ";
			}else if(tipoCliente.equals(new String("juridica")))
			{
				sql += "AND b.idtipopersona = 1 ";
			}
		}

		sql += "AND a.fechapedido >= ? " + "GROUP BY b.idcliente, b.nombre, b.telefono, c.nombre, b.email "
				+ "HAVING MAX(fechapedido) < ? " + "AND COUNT(*) >= ?";

		try {
			conn = con.obtenerConexionBDPrincipal();
			ps = conn.prepareStatement(sql);

			int index = 1;

			// Setear idTiendas
			if (idTiendas != null && !idTiendas.isEmpty()) {
				for (int idTienda : idTiendas) {
					ps.setInt(index++, idTienda);
				}
			}

			// Setear excepciones
			if (excepciones != null && !excepciones.isEmpty()) {
				for (int excepcion : excepciones) {
					ps.setInt(index++, excepcion);
				}
			}

			// Solo si se us� la condici�n de d�as, se setea ese par�metro
			if (diasMinimosSinPublicidad > 0) {
				ps.setInt(index++, diasMinimosSinPublicidad);
			}

			// Resto de par�metros
			ps.setString(index++, fechaInicio);
			ps.setString(index++, fechaMaxima);
			ps.setInt(index++, minPedidos);

			rs = ps.executeQuery();

			while (rs.next()) {
				ClienteSegmento cliente = new ClienteSegmento();
				cliente.setIdCliente(rs.getInt("idcliente"));
				cliente.setNombre(rs.getString("nombre"));
				cliente.setNombreComp(rs.getString("nombrecompania"));
				cliente.setTelefono(rs.getString("telefono"));
				cliente.setNombreTienda(rs.getString("nombretienda"));
				cliente.setNumeroPedidos(rs.getInt("numeropedidos"));
				cliente.setFechaMaxima(rs.getDate("fechamaxima"));
				cliente.setEmail(rs.getString("email"));
				clientes.add(cliente);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return clientes;
	}

	public List<ClienteClub> obtenerClientesClub(String estadoMiembro, String correoMiembro, int puntosMiembro,
	        String fechaMaxima, String fechaInicio, List<Integer> tiendas) {

	    List<ClienteClub> clientes = new ArrayList<>();
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    String sql = "SELECT \r\n"
	    		+ "    cf.correo, \r\n"
	    		+ "    cf.fecha_vinculacion, \r\n"
	    		+ "    cf.activo, \r\n"
	    		+ "    cf.puntos_vigentes,\r\n"
	    		+ "    c.idcliente, \r\n"
	    		+ "    CONCAT(COALESCE(c.nombre, ''), ' ', COALESCE(c.apellido, '')) AS nombre,\r\n"
	    		+ "    c.nombrecompania, \r\n"
	    		+ "    c.telefono, \r\n"
	    		+ "    t.nombre AS nombre_tienda\r\n"
	    		+ "FROM cliente_fidelizacion cf\r\n"
	    		+ "LEFT JOIN (\r\n"
	    		+ "    SELECT c1.*\r\n"
	    		+ "    FROM cliente c1\r\n"
	    		+ "    INNER JOIN (\r\n"
	    		+ "        SELECT email, MAX(idcliente) AS idcliente\r\n"
	    		+ "        FROM cliente\r\n"
	    		+ "        GROUP BY email\r\n"
	    		+ "    ) c2 ON c1.email = c2.email AND c1.idcliente = c2.idcliente\r\n"
	    		+ ") c ON cf.correo = c.email\r\n"
	    		+ "LEFT JOIN tienda t ON c.idtienda = t.idtienda\r\n"
	    		+ "WHERE 1=1\r\n"
	    		+ " ";

	    List<Object> params = new ArrayList<>();

	    if (estadoMiembro != null && !estadoMiembro.isEmpty()) {
	        sql += " AND cf.activo = ?";
	        params.add(estadoMiembro);
	    }
	    if (correoMiembro != null && !correoMiembro.isEmpty()) {
	        sql += " AND cf.correo = ?";
	        params.add(correoMiembro);
	    }
	    if (puntosMiembro > 0) {
	        sql += " AND cf.puntos_vigentes >= ?";
	        params.add(puntosMiembro);
	    }
	    if (fechaInicio != null && !fechaInicio.isEmpty()) {
	        sql += " AND cf.fecha_vinculacion >= ?";
	        params.add(fechaInicio);
	    }
	    if (fechaMaxima != null && !fechaMaxima.isEmpty()) {
	        sql += " AND cf.fecha_vinculacion <= ?";
	        params.add(fechaMaxima);
	    }
	    if (tiendas != null && !tiendas.isEmpty()) {
	        String placeholders = String.join(",", Collections.nCopies(tiendas.size(), "?"));
	        sql += " AND c.idtienda IN (" + placeholders + ")";
	        params.addAll(tiendas);
	    }

	    try {
	        conn = con.obtenerConexionBDPrincipal();
	        ps = conn.prepareStatement(sql);

	        for (int i = 0; i < params.size(); i++) {
	            if (params.get(i) instanceof Integer) {
	                ps.setInt(i + 1, (Integer) params.get(i));
	            } else {
	                ps.setString(i + 1, (String) params.get(i));
	            }
	        }

	        rs = ps.executeQuery();

	        while (rs.next()) {
	            ClienteClub cliente = new ClienteClub();
	            cliente.setCorreo(rs.getString("correo"));
	            cliente.setFechaVinculado(rs.getDate("fecha_vinculacion"));
	            cliente.setActivo(rs.getString("activo"));
	            cliente.setPuntos(rs.getInt("puntos_vigentes"));

	            cliente.setIdcliente(rs.getInt("idcliente"));

	            String nombre = rs.getString("nombre");
	            cliente.setNombre(nombre != null && !nombre.trim().isEmpty() ? nombre : "No identificado");

	            String compania = rs.getString("nombrecompania");
	            cliente.setNombrecompania(compania != null ? compania : "N/A");

	            String telefono = rs.getString("telefono");
	            cliente.setTelefono(telefono != null ? telefono : "Sin número");

	            String tienda = rs.getString("nombre_tienda");
	            cliente.setNombreTienda(tienda != null ? tienda : "Sin tienda");

	            clientes.add(cliente);
	        }

	  

	    } catch (SQLException e) {
	        System.out.println(e.getMessage());
	        e.printStackTrace();
	    } finally {
	        try {
	            if (rs != null) rs.close();
	            if (ps != null) ps.close();
	            if (conn != null) conn.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }

	    return clientes;
	}


	public boolean actualizarFechaUltimaPublicidad(List<Integer> idsClientes) {
		// Definir la sentencia SQL para actualizar la fecha
		String sql = "UPDATE cliente SET ultima_fecha_publicidad = ? WHERE idcliente = ?";

		// Declaraci�n de objetos para la conexi�n, el PreparedStatement y el manejo de
		// excepciones
		Connection con1 = null;
		PreparedStatement ps = null;

		try {
			// Crear una nueva instancia de la clase ConexionBaseDatos (conectar a la BD)
			ConexionBaseDatos con = new ConexionBaseDatos();
			con1 = con.obtenerConexionBDPrincipal(); // Obtener la conexi�n a la base de datos

			// Obtener la fecha actual
			Date fechaActual = new Date(); // Fecha actual
			java.sql.Date sqlDate = new java.sql.Date(fechaActual.getTime()); // Convertir a java.sql.Date para la base
																				// de datos

			// Preparar la sentencia SQL
			ps = con1.prepareStatement(sql);

			// Iterar sobre la lista de IDs de clientes y actualizar la fecha para cada uno
			for (Integer idCliente : idsClientes) {
				// Establecer los par�metros de la consulta
				ps.setDate(1, sqlDate); // Establecer la fecha actual
				ps.setInt(2, idCliente); // Establecer el ID del cliente

				// Ejecutar la actualizaci�n para el cliente actual
				int filasAfectadas = ps.executeUpdate();

				// Si alguna actualizaci�n no tuvo �xito, retornar false
				if (filasAfectadas == 0) {
					return false;
				}
			}

			// Si todas las actualizaciones fueron exitosas, retornar true
			return true;

		} catch (SQLException e) {
			e.printStackTrace(); // Manejo de errores en caso de excepciones SQL
			return false; // Si ocurre un error, retornar false
		} finally {
			try {
				// Cerrar los recursos en el bloque finally
				if (ps != null)
					ps.close();
				if (con1 != null)
					con1.close();
			} catch (SQLException e) {
				e.printStackTrace(); // Manejo de errores en el cierre de recursos
			}
		}
	}

}
