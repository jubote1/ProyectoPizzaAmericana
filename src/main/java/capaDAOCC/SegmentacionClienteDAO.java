package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import capaModeloCC.ClienteClub;
import capaModeloCC.FiltroSegmentacion;
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

	/**
	 * Clientes que cumplen los filtros de segmentacion, listos para un envio.
	 *
	 * QUE SE CORRIGIO AQUI Y POR QUE, porque cambia a quien le llega la publicidad:
	 *
	 * 1. Antes se unia detalle_pedido en la consulta principal. Eso multiplica la
	 *    fila por cada renglon del pedido, y el COUNT(*) contaba RENGLONES y no
	 *    pedidos. Medido sobre 86.280 clientes del ultimo ano: el conteo venia
	 *    inflado 4,82 veces en promedio, y con el filtro de "3 pedidos o mas"
	 *    pasaban 77.531 clientes cuando debian pasar 18.353. Ahora el detalle solo
	 *    se consulta con EXISTS, que filtra sin multiplicar, y el conteo es
	 *    COUNT(DISTINCT a.idpedido).
	 *
	 * 2. Antes se agrupaba por b.idcliente. La tabla cliente tiene idtienda, asi
	 *    que la misma persona esta repetida una vez por tienda: le llegaba el correo
	 *    tantas veces como tiendas tuviera, y sus pedidos quedaban partidos entre
	 *    esos registros. Ahora se agrupa por CORREO NORMALIZADO, que es la persona.
	 *
	 * 3. El canal entraba al SQL concatenado ("AND a.origen = '" + canal + "'").
	 *    Ahora va con placeholders, como ya iban las tiendas.
	 *
	 * 4. No se descontaban los pedidos cancelados: un pedido anulado no es una
	 *    compra y no deberia contar para segmentar.
	 *
	 * Los datos que se muestran -nombre, telefono, tienda- salen del pedido MAS
	 * RECIENTE de la persona: son los mas vigentes que tenemos de ella.
	 *
	 * OJO con el significado del conteo: los filtros de promocion, producto y
	 * especialidad son a nivel de PEDIDO, asi que "numeropedidos" es la cantidad de
	 * pedidos que cumplen los filtros, no el total historico del cliente. Se dejo
	 * asi a proposito para no cambiar el comportamiento que ya tenia el filtro de
	 * promociones.
	 */
	public List<ClienteSegmento> obtenerClientesFiltrados(FiltroSegmentacion filtro) {

		List<ClienteSegmento> clientes = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		//Los parametros se van juntando en el mismo orden en que se agregan a la
		//consulta. Antes se llevaban con un index++ a mano y cada filtro nuevo era
		//una oportunidad de desalinearlos.
		List<Object> parametros = new ArrayList<>();

		//El pedido mas reciente de la persona manda para los datos de contacto.
		String masReciente = "ORDER BY a.fechapedido DESC, a.idpedido DESC SEPARATOR '|'";

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT CAST(SUBSTRING_INDEX(GROUP_CONCAT(b.idcliente ").append(masReciente)
				.append("), '|', 1) AS UNSIGNED) AS idcliente, ");
		sql.append("SUBSTRING_INDEX(GROUP_CONCAT(TRIM(CONCAT(IFNULL(b.nombre,''), ' ', ")
				.append("IFNULL(b.apellido,''))) ").append(masReciente).append("), '|', 1) AS nombre, ");
		sql.append("SUBSTRING_INDEX(GROUP_CONCAT(IFNULL(b.nombrecompania,'') ").append(masReciente)
				.append("), '|', 1) AS nombrecompania, ");
		sql.append("SUBSTRING_INDEX(GROUP_CONCAT(IFNULL(NULLIF(TRIM(b.telefono_celular),''), ")
				.append("IFNULL(b.telefono,'')) ").append(masReciente).append("), '|', 1) AS telefono, ");
		sql.append("SUBSTRING_INDEX(GROUP_CONCAT(c.nombre ").append(masReciente)
				.append("), '|', 1) AS nombretienda, ");
		sql.append("COUNT(DISTINCT a.idpedido) AS numeropedidos, ");
		sql.append("MAX(a.fechapedido) AS fechamaxima, ");
		sql.append("LOWER(TRIM(b.email)) AS email ");
		sql.append("FROM pedido a ");
		sql.append("JOIN cliente b ON a.idcliente = b.idcliente ");
		sql.append("JOIN tienda  c ON b.idtienda = c.idtienda ");
		sql.append("WHERE c.funcional <> 'N' ");
		sql.append("AND IFNULL(a.cancelado,'0') <> '1' ");
		//Consentimiento y calidad del correo. Esto ya estaba y se mantiene, pero
		//ahora tambien se exige que el correo tenga forma de correo.
		sql.append("AND b.email LIKE '%_@_%._%' ");
		sql.append("AND b.politica_datos = 'S' AND b.envio_publicidad = 'S' ");

		//Clientes de plataforma: sus datos de contacto no son nuestros. Son los que
		//encabezan cualquier conteo -"privacy protection", "CLIENTE RAPPI"- y no
		//sirven para una campana.
		if (filtro.isExcluirPlataformas()) {
			sql.append("AND SUBSTRING_INDEX(LOWER(TRIM(b.email)), '@', -1) <> 'rappi.com' ");
			sql.append("AND LOWER(TRIM(b.email)) NOT IN ('notengo@gmail.com', 'notiene@gmail.com') ");
		}

		if (!filtro.getTiendas().isEmpty()) {
			sql.append("AND c.idtienda IN (").append(marcadores(filtro.getTiendas().size())).append(") ");
			parametros.addAll(filtro.getTiendas());
		}

		if (!filtro.getCanales().isEmpty()) {
			sql.append("AND a.origen IN (").append(marcadores(filtro.getCanales().size())).append(") ");
			parametros.addAll(filtro.getCanales());
		}

		//Tipo de cliente: la pantalla habla de natural y juridica, la tabla guarda
		//idtipopersona. La correspondencia estaba quemada en dos if y ahora se
		//traduce en un solo lugar.
		List<Integer> tiposPersona = new ArrayList<>();
		for (String tipo : filtro.getTiposCliente()) {
			if ("natural".equalsIgnoreCase(tipo)) {
				tiposPersona.add(2);
			} else if ("juridica".equalsIgnoreCase(tipo)) {
				tiposPersona.add(1);
			}
		}
		if (!tiposPersona.isEmpty()) {
			sql.append("AND b.idtipopersona IN (").append(marcadores(tiposPersona.size())).append(") ");
			parametros.addAll(tiposPersona);
		}

		//Los tres filtros de detalle van con EXISTS y no con JOIN: filtran el pedido
		//sin multiplicar la fila, que es lo que inflaba el conteo.
		if (!filtro.getExcepciones().isEmpty()) {
			sql.append("AND EXISTS (SELECT 1 FROM detalle_pedido d WHERE d.idpedido = a.idpedido ")
					.append("AND d.idexcepcion IN (").append(marcadores(filtro.getExcepciones().size()))
					.append(")) ");
			parametros.addAll(filtro.getExcepciones());
		}

		if (!filtro.getProductos().isEmpty()) {
			sql.append("AND EXISTS (SELECT 1 FROM detalle_pedido d WHERE d.idpedido = a.idpedido ")
					.append("AND d.idproducto IN (").append(marcadores(filtro.getProductos().size()))
					.append(")) ");
			parametros.addAll(filtro.getProductos());
		}

		//La especialidad puede venir en cualquiera de los dos lados de la pizza,
		//asi que hay que mirar las dos columnas.
		if (!filtro.getEspecialidades().isEmpty()) {
			String marcas = marcadores(filtro.getEspecialidades().size());
			sql.append("AND EXISTS (SELECT 1 FROM detalle_pedido d WHERE d.idpedido = a.idpedido ")
					.append("AND (d.idespecialidad1 IN (").append(marcas)
					.append(") OR d.idespecialidad2 IN (").append(marcas).append("))) ");
			parametros.addAll(filtro.getEspecialidades());
			parametros.addAll(filtro.getEspecialidades());
		}

		if (filtro.getDiasMinimosSinPublicidad() > 0) {
			sql.append("AND (b.ultima_fecha_publicidad IS NULL ")
					.append("OR b.ultima_fecha_publicidad <= CURRENT_DATE - INTERVAL ? DAY) ");
			parametros.add(filtro.getDiasMinimosSinPublicidad());
		}

		if (filtro.getFechaInicio().length() > 0) {
			sql.append("AND a.fechapedido >= ? ");
			parametros.add(filtro.getFechaInicio());
		}

		//Se agrupa por CORREO, que es la persona, y no por idcliente.
		sql.append("GROUP BY LOWER(TRIM(b.email)) ");
		sql.append("HAVING COUNT(DISTINCT a.idpedido) >= ? ");
		parametros.add(filtro.getMinPedidos());

		if (filtro.getMaxPedidos() > 0) {
			sql.append("AND COUNT(DISTINCT a.idpedido) <= ? ");
			parametros.add(filtro.getMaxPedidos());
		}

		//Se conserva el significado que ya tenia: la ultima compra por debajo de la
		//fecha maxima del rango.
		if (filtro.getFechaMaxima().length() > 0) {
			sql.append("AND MAX(a.fechapedido) < ? ");
			parametros.add(filtro.getFechaMaxima());
		}

		//Recencia: la ultima compra hace ENTRE tantos y tantos dias. Sirve tanto
		//para buscar a los activos -hasta 25 dias- como para reactivar a los que
		//llevan mucho sin comprar.
		if (filtro.getDiasUltimaCompraHasta() > 0) {
			sql.append("AND MAX(a.fechapedido) >= CURRENT_DATE - INTERVAL ? DAY ");
			parametros.add(filtro.getDiasUltimaCompraHasta());
		}
		if (filtro.getDiasUltimaCompraDesde() > 0) {
			sql.append("AND MAX(a.fechapedido) <= CURRENT_DATE - INTERVAL ? DAY ");
			parametros.add(filtro.getDiasUltimaCompraDesde());
		}

		sql.append("ORDER BY numeropedidos DESC, fechamaxima DESC");

		try {
			conn = con.obtenerConexionBDPrincipal();
			ps = conn.prepareStatement(sql.toString());
			for (int i = 0; i < parametros.size(); i++) {
				ps.setObject(i + 1, parametros.get(i));
			}
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
			System.out.println("SegmentacionClienteDAO.obtenerClientesFiltrados: " + e);
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

	/** Devuelve "?, ?, ?" con tantos marcadores como haga falta. */
	private String marcadores(final int cantidad) {
		return(String.join(",", Collections.nCopies(cantidad, "?")));
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


	public static Map<String, Object> actualizarFechaUltimaPublicidad(List<Integer> idsClientes) {

	    Map<String, Object> resultado = new HashMap<>();
	    List<Integer> clientesFallidos = new ArrayList<>();

	    String sql = "UPDATE cliente SET ultima_fecha_publicidad = ? WHERE idcliente = ?";

	    Connection con1 = null;
	    PreparedStatement ps = null;

	    try {

	        ConexionBaseDatos con = new ConexionBaseDatos();
	        con1 = con.obtenerConexionBDPrincipal();

	        java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());

	        ps = con1.prepareStatement(sql);

	        for (Integer idCliente : idsClientes) {

	            try {

	                ps.setDate(1, sqlDate);
	                ps.setInt(2, idCliente);

	                int filasAfectadas = ps.executeUpdate();

	                if (filasAfectadas == 0) {
	                    clientesFallidos.add(idCliente);
	                }

	            } catch (SQLException e) {
	                clientesFallidos.add(idCliente);
	                e.printStackTrace();
	            }

	        }

	        if (clientesFallidos.isEmpty()) {
	            resultado.put("success", true);
	            resultado.put("message", "La fecha de publicacion de todos los clientes seleccionados fueron actualizados correctamente.");
	        } else {
	            resultado.put("success", false);
	            resultado.put("message", "Algunos clientes no pudieron actualizarse.");
	            resultado.put("clientesFallidos", clientesFallidos);
	        }

	    } catch (SQLException e) {

	        e.printStackTrace();

	        resultado.put("success", false);
	        resultado.put("message", "Ocurrió un error general al conectar o ejecutar la actualización.");
	        resultado.put("clientesFallidos", clientesFallidos);

	    } finally {

	        try {
	            if (ps != null)
	                ps.close();

	            if (con1 != null)
	                con1.close();

	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }

	    return resultado;
	}


	/**
	 * Catalogo para los filtros de la pantalla de segmentacion: productos y
	 * especialidades, cada uno con su id y su nombre.
	 *
	 * Van juntos en un solo metodo -y en un solo servicio- porque la pantalla los
	 * necesita al mismo tiempo, al cargar: dos llamadas para llenar dos listas es
	 * una ida y vuelta de mas sin ninguna ganancia.
	 *
	 * @param cual "productos" o "especialidades"
	 */
	public List<Map<String, Object>> obtenerCatalogo(final String cual) {
		List<Map<String, Object>> lista = new ArrayList<>();
		String sql;
		if ("especialidades".equals(cual)) {
			sql = "SELECT idespecialidad AS id, nombre FROM especialidad ORDER BY nombre";
		} else {
			//El nombre del producto sale de nombre, y si viene vacio se cae a descripcion:
			//hay productos viejos que solo tienen la descripcion diligenciada.
			sql = "SELECT idproducto AS id, "
					+ "IFNULL(NULLIF(TRIM(nombre),''), IFNULL(descripcion,'')) AS nombre "
					+ "FROM producto "
					+ "WHERE IFNULL(NULLIF(TRIM(nombre),''), IFNULL(descripcion,'')) <> '' "
					+ "ORDER BY nombre";
		}
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = con.obtenerConexionBDPrincipal();
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Object> fila = new HashMap<>();
				fila.put("id", rs.getInt("id"));
				fila.put("nombre", rs.getString("nombre"));
				lista.add(fila);
			}
		} catch (SQLException e) {
			System.out.println("SegmentacionClienteDAO.obtenerCatalogo(" + cual + "): " + e);
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
		return lista;
	}
}
