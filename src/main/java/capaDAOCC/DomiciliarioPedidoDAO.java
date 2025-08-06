package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	        sql.append("WITH UbicacionesConRow AS ( ")
	           .append("    SELECT ubi.clave_dom, ti.nombre AS tienda, ubi.idtienda, ubi.latitud, ubi.longitud, ubi.fecha, ")
	           .append("           COALESCE(e.nombre_largo, et.nombre) AS nombre_largo, ")
	           .append("           ROW_NUMBER() OVER (PARTITION BY ubi.clave_dom ORDER BY ubi.fecha DESC) AS rn ")
	           .append("    FROM ubicacion_domiciliario ubi ")
	           .append("    LEFT JOIN general.empleado e ON ubi.clave_dom = e.claverapida ")
	           .append("    LEFT JOIN general.empleado_temporal et ON ubi.clave_dom = RIGHT(et.identificacion, 6) ")
	           .append("    LEFT JOIN tienda ti ON ti.idtienda = ubi.idtienda ")
	           .append("    WHERE (e.claverapida IS NOT NULL OR et.identificacion IS NOT NULL) ");

	        if (id != 0) {
	            sql.append("AND ubi.idtienda = ").append(id).append(" ");
	        }

	        sql.append("AND DATE(ubi.fecha) = CURRENT_DATE ")
	           .append(") ")
	           .append("SELECT clave_dom, tienda, idtienda, latitud, longitud, fecha, nombre_largo FROM UbicacionesConRow WHERE rn = 1");

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
	    sql.append("WITH UbicacionesConRow AS ( ")
	       .append("  SELECT ubi.clave_dom, COALESCE(e.nombre_largo, et.nombre) AS nombre_largo, DATE(ubi.fecha) AS fecha, ")
	       .append("         ti.nombre AS tienda, ubi.idtienda, ")
	       .append("         ROW_NUMBER() OVER (PARTITION BY ubi.clave_dom, DATE(ubi.fecha) ORDER BY ubi.fecha DESC) AS rn ")
	       .append("  FROM ubicacion_domiciliario ubi ")
	       .append("  LEFT JOIN general.empleado e ON ubi.clave_dom = e.claverapida ")
	       .append("  LEFT JOIN general.empleado_temporal et ON ubi.clave_dom = RIGHT(et.identificacion, 6) ")
	       .append("  LEFT JOIN tienda ti ON ti.idtienda = ubi.idtienda ")
	       .append("  WHERE (e.claverapida IS NOT NULL OR et.identificacion IS NOT NULL) ");

	    if (idTienda != 0) {
	        sql.append("AND ubi.idtienda = ? ");
	    }
	    if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
	        sql.append("AND DATE(ubi.fecha) BETWEEN ? AND ? ");
	    } else {
	        return lista;
	    }

	    sql.append(") SELECT clave_dom, nombre_largo, fecha, tienda, idtienda FROM UbicacionesConRow WHERE rn = 1 ORDER BY fecha, nombre_largo");

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
	                lista.add(js);
	            }
	        }
	    }
	    return lista;
	}


	public static List<JSONObject> DetalleHistorialUsuariosPorFecha(int idTienda, String fecha, String claveRapida) throws SQLException {
	    List<JSONObject> lista = new ArrayList<>();
	    StringBuilder sql = new StringBuilder();
	    sql.append("SELECT ubi.clave_dom, COALESCE(e.nombre_largo, et.nombre) AS nombre_largo, ")
	       .append("       ubi.fecha, ti.nombre AS tienda, ubi.idtienda, ubi.latitud, ubi.longitud ")
	       .append("FROM ubicacion_domiciliario ubi ")
	       .append("LEFT JOIN general.empleado e ON ubi.clave_dom = e.claverapida ")
	       .append("LEFT JOIN general.empleado_temporal et ON ubi.clave_dom = RIGHT(et.identificacion, 6) ")
	       .append("LEFT JOIN tienda ti ON ti.idtienda = ubi.idtienda ")
	       .append("WHERE (e.claverapida IS NOT NULL OR et.identificacion IS NOT NULL) ")
	       .append("AND ubi.idtienda = ? AND DATE(ubi.fecha) = ? AND ubi.clave_dom = ? ")
	       .append("ORDER BY ubi.fecha DESC, nombre_largo");

	    try (Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
	         PreparedStatement statement = con1.prepareStatement(sql.toString())) {

	        int paramIndex = 1;
	        statement.setInt(paramIndex++, idTienda);
	        statement.setString(paramIndex++, fecha);
	        statement.setString(paramIndex++, claveRapida);

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
	                lista.add(js);
	            }
	        }
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



