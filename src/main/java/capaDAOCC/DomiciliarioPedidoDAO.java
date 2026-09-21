package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import capaModeloCC.DomiciliarioPedido;
import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.MensajeTexto;
import capaModeloCC.Oferta;
import capaModeloCC.OfertaCliente;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * Clase que implementa todos los m�todos de acceso a la base de datos para la administraci�n de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class DomiciliarioPedidoDAO {
	
	public static void insertarDomiciliarioPedido(DomiciliarioPedido domPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{ 
			Statement stm = con1.createStatement();
			String insert = "";
			insert = "insert into domiciliario_pedido (idusuario,fecha, idtienda, cantidad) values (" + domPedido.getIdUsuario() + " , '" + domPedido.getFecha() +  "' , " + domPedido.getIdTienda() + " , " + domPedido.getCantidad() + " )";
			logger.info(insert);
			stm.executeUpdate(insert);
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
	}
	
	public ArrayList<JSONObject> obtenerPorId(int id,Boolean bandera) throws SQLException {
		ArrayList<JSONObject> lista = new ArrayList<JSONObject>();
		try {
		
	
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String sql ="SELECT  ubi.clave_dom,ti.nombre,  ubi.latitud, ubi.longitud,t.fecha,t.nombre FROM ubicacion_domiciliario ubi JOIN (SELECT  u.clave_dom AS clave ,MAX(u.fecha)"
				+ " AS fecha ,e.nombre_largo AS nombre,MAX(u.idubicacion) AS ubicacion from ubicacion_domiciliario u  left join general.empleado  e on u.clave_dom =  e.claverapida "
				+ "COLLATE UTF8_UNICODE_CI WHERE e.claverapida IS NOT NULL  GROUP BY u.clave_dom  ,e.nombre_largo) t ON ubi.idubicacion = t.ubicacion JOIN tienda ti ON ti.idtienda = ubi.idtienda";

		if(bandera) {
			sql =sql+" WHERE ubi.idtienda = "+id;
	
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Statement statement  = con1.createStatement();
		ResultSet rs = statement.executeQuery(sql);
		while (rs.next()) {
			String fechaTexto = formatter.format(rs.getTimestamp(5));
			JSONObject js = new JSONObject();
			js.put("clave_dom",rs.getString(1) );
			js.put("idtienda",rs.getString(2));
			js.put("latitud",rs.getString(3) );
			js.put("longitud",rs.getString(4) );
			js.put("fecha",fechaTexto);
			js.put("nombre_largo",rs.getString(6));

			lista.add(js);
		}
		rs.close();
		con1.close();
		} catch (Exception e) {

			System.out.println("" + e.toString());
		}
		return lista;
	}
	
	
	// ✅ VERSIÓN LIMPIA DE LOS 3 MÉTODOS
	// ✅ SIN UNION. CON LEFT JOIN A EMPLEADO Y EMPLEADO_TEMPORAL

	public static List<JSONObject> HistorialActualUbicacion(int id) throws SQLException {
	    List<JSONObject> lista = new ArrayList<>();
	    StringBuilder sql = new StringBuilder();
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    try {
	        // Consulta directa optimizada con tabla derivada para eventos biométricos de hoy (ejecuta en ~300ms)
	        sql.append("SELECT ubi.clave_dom, ti.nombre AS tienda, ubi.idtienda, ubi.latitud, ubi.longitud, ubi.fecha, ")
	           .append("       COALESCE(ubi.estado, 'EN_TIENDA') AS estado, ubi.bateria, ubi.velocidad, ")
	           .append("       COALESCE(ubi.pedidos_activos, 0) AS pedidos_activos, ubi.pedidos_detalle, ")
	           .append("       COALESCE(e.nombre_largo, et.nombre, ubi.nombre_usuario, 'Domiciliario') AS nombre_largo, ")
	           .append("       CASE ")
	           .append("           WHEN et.identificacion IS NOT NULL THEN 'TEMPORAL' ")
	           .append("           WHEN e.claverapida IS NOT NULL THEN 'DIRECTO' ")
	           .append("           ELSE 'OTRO' ")
	           .append("       END AS tipo_repartidor, ")
	           .append("       COALESCE(et.empresa, '') AS empresa_temporal, ")
	           .append("       CASE WHEN ev.id IS NOT NULL THEN 1 ELSE 0 END AS en_turno_biometria ")
	           .append("FROM domiciliario_ubicacion_actual ubi ")
	           .append("LEFT JOIN tienda ti ON ti.idtienda = ubi.idtienda ")
	           .append("LEFT JOIN general.empleado e ON ubi.clave_dom COLLATE utf8mb4_unicode_ci = e.claverapida COLLATE utf8mb4_unicode_ci ")
	           .append("LEFT JOIN general.empleado_temporal et ON (ubi.clave_dom COLLATE utf8mb4_unicode_ci = RIGHT(TRIM(et.identificacion), 6) COLLATE utf8mb4_unicode_ci OR ubi.clave_dom COLLATE utf8mb4_unicode_ci = TRIM(et.identificacion) COLLATE utf8mb4_unicode_ci) ")
	           .append("LEFT JOIN (SELECT DISTINCT id FROM general.empleado_evento WHERE fecha = CURDATE()) ev ON (ev.id = e.id OR ev.id = CAST(RIGHT(TRIM(et.identificacion), 6) AS UNSIGNED) OR ev.id = CAST(TRIM(et.identificacion) AS UNSIGNED)) ")
	           .append("WHERE ubi.fecha >= CURDATE() ");

	        if (id != 0) {
	            sql.append("AND ubi.idtienda = ").append(id).append(" ");
	        }
	        sql.append("ORDER BY ubi.fecha DESC");

	        try (PreparedStatement statement = con1.prepareStatement(sql.toString());
	             ResultSet rs = statement.executeQuery()) {

	            while (rs.next()) {
	                JSONObject js = new JSONObject();
	                js.put("clave_usuario", rs.getString("clave_dom"));
	                js.put("idtienda", rs.getString("idtienda"));
	                js.put("tienda", rs.getString("tienda"));
	                js.put("latitud", rs.getString("latitud"));
	                js.put("longitud", rs.getString("longitud"));
	                js.put("fecha", rs.getString("fecha"));
	                js.put("nombre_usuario", rs.getString("nombre_largo"));
	                js.put("tipo_repartidor", rs.getString("tipo_repartidor"));
	                js.put("empresa_temporal", rs.getString("empresa_temporal"));
	                js.put("en_turno_biometria", rs.getInt("en_turno_biometria") == 1);
	                js.put("estado", rs.getString("estado"));
	                js.put("bateria", rs.getObject("bateria") != null ? rs.getInt("bateria") : null);
	                js.put("velocidad", rs.getInt("velocidad"));
	                js.put("pedidos_activos", rs.getInt("pedidos_activos"));
	                js.put("pedidos_detalle", rs.getString("pedidos_detalle"));
	                lista.add(js);
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("Error HistorialActualUbicacion: " + e);
	    } finally {
	        if (con1 != null) con1.close();
	    }
	    return lista;
	}


	public static List<JSONObject> HistorialUsuariosPorFecha(int idTienda, String fechaInicio, String fechaFin) throws SQLException {
	    List<JSONObject> lista = new ArrayList<>();
	    StringBuilder sql = new StringBuilder();
	    sql.append("SELECT u.clave_dom, ")
	       .append("       COALESCE(e.nombre_largo, et.nombre, act.nombre_usuario, u.clave_dom) AS nombre_largo, ")
	       .append("       u.fecha_dia AS fecha, ")
	       .append("       ti.nombre AS tienda, ")
	       .append("       u.idtienda, ")
	       .append("       CASE ")
	       .append("           WHEN et.identificacion IS NOT NULL THEN 'TEMPORAL' ")
	       .append("           WHEN e.claverapida IS NOT NULL THEN 'DIRECTO' ")
	       .append("           ELSE 'OTRO' ")
	       .append("       END AS tipo_repartidor, ")
	       .append("       COALESCE(et.empresa, '') AS empresa_temporal ")
	       .append("FROM ( ")
	       .append("    SELECT clave_dom, ")
	       .append("           DATE(fecha) AS fecha_dia, ")
	       .append("           SUBSTRING_INDEX(GROUP_CONCAT(idtienda ORDER BY fecha DESC), ',', 1) AS idtienda ")
	       .append("    FROM ubicacion_domiciliario ")
	       .append("    WHERE 1=1 ");

	    if (idTienda != 0) {
	        sql.append("    AND idtienda = ? ");
	    }
	    if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
	        sql.append("    AND fecha >= ? AND fecha < DATE_ADD(?, INTERVAL 1 DAY) ");
	    } else {
	        return lista;
	    }

	    sql.append("    GROUP BY clave_dom, DATE(fecha) ")
	       .append(") u ")
	       .append("LEFT JOIN tienda ti ON ti.idtienda = u.idtienda ")
	       .append("LEFT JOIN domiciliario_ubicacion_actual act ON u.clave_dom COLLATE utf8mb4_unicode_ci = act.clave_dom COLLATE utf8mb4_unicode_ci ")
	       .append("LEFT JOIN general.empleado e ON u.clave_dom COLLATE utf8mb4_unicode_ci = e.claverapida COLLATE utf8mb4_unicode_ci ")
	       .append("LEFT JOIN general.empleado_temporal et ON (u.clave_dom COLLATE utf8mb4_unicode_ci = RIGHT(TRIM(et.identificacion), 6) COLLATE utf8mb4_unicode_ci OR u.clave_dom COLLATE utf8mb4_unicode_ci = TRIM(et.identificacion) COLLATE utf8mb4_unicode_ci) ")
	       .append("ORDER BY u.fecha_dia DESC, nombre_largo");

	    try (Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
	         PreparedStatement statement = con1.prepareStatement(sql.toString())) {

	        int paramIndex = 1;
	        if (idTienda != 0) statement.setInt(paramIndex++, idTienda);
	        statement.setString(paramIndex++, fechaInicio);
	        statement.setString(paramIndex++, fechaFin);

	        try (ResultSet rs = statement.executeQuery()) {
	            while (rs.next()) {
	                JSONObject js = new JSONObject();
	                js.put("clave_usuario", rs.getString("clave_dom"));
	                js.put("nombre_usuario", rs.getString("nombre_largo"));
	                js.put("fecha", rs.getString("fecha"));
	                js.put("tienda", rs.getString("tienda"));
	                js.put("idtienda", rs.getString("idtienda"));
	                js.put("tipo_repartidor", rs.getString("tipo_repartidor"));
	                js.put("empresa_temporal", rs.getString("empresa_temporal"));
	                lista.add(js);
	            }
	        }
	    }
	    return lista;
	}


	public static List<JSONObject> DetalleHistorialUsuariosPorFecha(int idTienda, String fecha, String claveRapida) throws SQLException {
	    List<JSONObject> lista = new ArrayList<>();
	    StringBuilder sql = new StringBuilder();
	    sql.append("SELECT ubi.clave_dom, ")
	       .append("       COALESCE(e.nombre_largo, et.nombre, act.nombre_usuario, ubi.clave_dom) AS nombre_largo, ")
	       .append("       ubi.fecha, ti.nombre AS tienda, ubi.idtienda, ubi.latitud, ubi.longitud, ")
	       .append("       CASE ")
	       .append("           WHEN et.identificacion IS NOT NULL THEN 'TEMPORAL' ")
	       .append("           WHEN e.claverapida IS NOT NULL THEN 'DIRECTO' ")
	       .append("           ELSE 'OTRO' ")
	       .append("       END AS tipo_repartidor, ")
	       .append("       COALESCE(et.empresa, '') AS empresa_temporal ")
	       .append("FROM ubicacion_domiciliario ubi ")
	       .append("LEFT JOIN domiciliario_ubicacion_actual act ON ubi.clave_dom COLLATE utf8mb4_unicode_ci = act.clave_dom COLLATE utf8mb4_unicode_ci ")
	       .append("LEFT JOIN general.empleado e ON ubi.clave_dom COLLATE utf8mb4_unicode_ci = e.claverapida COLLATE utf8mb4_unicode_ci ")
	       .append("LEFT JOIN general.empleado_temporal et ON (ubi.clave_dom COLLATE utf8mb4_unicode_ci = RIGHT(TRIM(et.identificacion), 6) COLLATE utf8mb4_unicode_ci OR ubi.clave_dom COLLATE utf8mb4_unicode_ci = TRIM(et.identificacion) COLLATE utf8mb4_unicode_ci) ")
	       .append("LEFT JOIN tienda ti ON ti.idtienda = ubi.idtienda ")
	       .append("WHERE ubi.clave_dom = ? ");

	    if (idTienda != 0) {
	        sql.append("AND ubi.idtienda = ? ");
	    }
	    sql.append("AND ubi.fecha >= ? AND ubi.fecha < DATE_ADD(?, INTERVAL 1 DAY) ")
	       .append("ORDER BY ubi.fecha ASC");

	    try (Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
	         PreparedStatement statement = con1.prepareStatement(sql.toString())) {

	        int paramIndex = 1;
	        statement.setString(paramIndex++, claveRapida);
	        if (idTienda != 0) statement.setInt(paramIndex++, idTienda);
	        statement.setString(paramIndex++, fecha);
	        statement.setString(paramIndex++, fecha);

	        try (ResultSet rs = statement.executeQuery()) {
	            while (rs.next()) {
	                JSONObject js = new JSONObject();
	                js.put("clave_usuario", rs.getString("clave_dom"));
	                js.put("nombre_usuario", rs.getString("nombre_largo"));
	                js.put("fecha", rs.getString("fecha"));
	                js.put("tienda", rs.getString("tienda"));
	                js.put("idtienda", rs.getString("idtienda"));
	                js.put("latitud", rs.getDouble("latitud"));
	                js.put("longitud", rs.getDouble("longitud"));
	                js.put("tipo_repartidor", rs.getString("tipo_repartidor"));
	                js.put("empresa_temporal", rs.getString("empresa_temporal"));
	                lista.add(js);
	            }
	        }
	    }
	    return lista;
	}

	public static List<JSONObject> ObtenerDespachosHistorial(String fecha, String claveRapida) throws SQLException {
	    List<JSONObject> lista = new ArrayList<>();
	    String sql = "SELECT dr.id AS despacho_id, dr.idtienda, ti.nombre AS nombre_tienda, " +
	                 "       dr.hora_salida, dr.hora_regreso, drd.id_pedido, drd.orden_planificada, drd.hora_entrega " +
	                 "FROM datamart.despacho_real dr " +
	                 "LEFT JOIN datamart.despacho_real_det drd ON dr.id = drd.despacho_real_id AND dr.idtienda = drd.idtienda " +
	                 "LEFT JOIN tienda ti ON dr.idtienda = ti.idtienda " +
	                 "LEFT JOIN general.empleado e ON dr.id_domiciliario = e.id " +
	                 "LEFT JOIN general.empleado_temporal et ON dr.id_domiciliario = et.id " +
	                 "WHERE dr.fecha = ? " +
	                 "  AND (e.claverapida = ? OR et.identificacion LIKE CONCAT('%', ?) OR CAST(dr.id_domiciliario AS CHAR) = ?) " +
	                 "ORDER BY dr.hora_salida ASC, drd.orden_planificada ASC";

	    try (Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
	         PreparedStatement ps = con1.prepareStatement(sql)) {
	        ps.setString(1, fecha);
	        ps.setString(2, claveRapida);
	        ps.setString(3, claveRapida);
	        ps.setString(4, claveRapida);

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                JSONObject js = new JSONObject();
	                js.put("despacho_id", rs.getInt("despacho_id"));
	                js.put("idtienda", rs.getInt("idtienda"));
	                js.put("tienda", rs.getString("nombre_tienda"));
	                js.put("hora_salida", rs.getString("hora_salida"));
	                js.put("hora_regreso", rs.getString("hora_regreso"));
	                js.put("id_pedido", rs.getObject("id_pedido") != null ? rs.getInt("id_pedido") : null);
	                js.put("orden_planificada", rs.getObject("orden_planificada") != null ? rs.getInt("orden_planificada") : null);
	                js.put("hora_entrega", rs.getString("hora_entrega"));
	                lista.add(js);
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("Error ObtenerDespachosHistorial: " + e);
	    }
	    return lista;
	}
	
	
	public ArrayList<JSONObject> ListaTiendas() {

		ArrayList<JSONObject> lista = new ArrayList<JSONObject>();
		try {
			String sql ="select idtienda, nombre from tienda  where nombre != 'Bodega' and nombre != 'Contact Center' and nombre != 'Tienda pruebas'";
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDPrincipal();
			Statement statement  = con1.createStatement();
			ResultSet r = statement.executeQuery(sql);
			while (r.next()) {
				JSONObject jsonobject = new JSONObject();
				jsonobject.put("id",r.getInt(1) );
				jsonobject.put("nombre",r.getString(2) );
			
				lista.add(jsonobject);
			}
			r.close();
			con1.close();
		} catch (Exception e) {

			System.out.println("" + e.toString());
		}

		return lista;
	}

	public static void main(String[] args) {
		
		try {
			System.out.println(HistorialUsuariosPorFecha(0, "2025-01-01", "2025-01-14"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	

}



