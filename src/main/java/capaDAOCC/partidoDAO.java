package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.simple.JSONObject;

import capaModeloCC.Pronostico;
import conexionCC.ConexionBaseDatos;

public class partidoDAO {

	
	public static JSONObject guardarPronostico(Pronostico pronostico) {

	   
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    JSONObject respuesta = new JSONObject();

	    try {

	        String sql = "INSERT INTO pronosticos ("
	                   + "id_partido, "
	                   + "nombre_cliente, "
	                   + "telefono, "
	                   + "idtienda, "
	                   + "numero_factura, "
	                   + "marcador_pais_1, "
	                   + "marcador_pais_2"
	                   + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

	        PreparedStatement ps = con1.prepareStatement(sql);

	        ps.setInt(1, pronostico.getIdPartido());
	        ps.setString(2, pronostico.getNombreCliente());
	        ps.setString(3, pronostico.getTelefono());
	        ps.setInt(4, pronostico.getIdTienda());
	        ps.setString(5, pronostico.getNumeroFactura());
	        ps.setInt(6, pronostico.getMarcadorPais1());
	        ps.setInt(7, pronostico.getMarcadorPais2());

	        int filas = ps.executeUpdate();

	        ps.close();
	        con1.close();

	        if (filas > 0) {

	            respuesta.put("success", true);
	            respuesta.put("message", "Pronóstico registrado correctamente.");

	        } else {

	            respuesta.put("success", false);
	            respuesta.put("message", "No fue posible registrar el pronóstico.");
	        }

	    } catch (Exception e) {

	        System.out.println("Error guardando pronóstico: " + e.toString());

	        try {
	            con1.close();
	        } catch (Exception e1) {}

	        respuesta.put("success", false);
	        respuesta.put("message", "Error interno del servidor.");
	    }

	    return respuesta;
	}
	
	public static JSONObject obtenerPartidoPublicado() {

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    JSONObject respuesta = new JSONObject();

	    try {

	    	String sql = """
	    		    SELECT p.id,
	    		           p.fecha_partido,
	    		           pa1.nombre AS pais1,
	    		           pa1.bandera AS bandera1,
	    		           pa2.nombre AS pais2,
	    		           pa2.bandera AS bandera2
	    		    FROM partidos p
	    		    INNER JOIN paises pa1 ON p.id_pais_1 = pa1.id
	    		    INNER JOIN paises pa2 ON p.id_pais_2 = pa2.id
	    		    WHERE p.fecha_publicacion >= CURDATE()
	    		    ORDER BY p.fecha_publicacion ASC
	    		    LIMIT 1
	    		    """;
	        PreparedStatement ps = con1.prepareStatement(sql);

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {

	            respuesta.put("success", true);
	            respuesta.put("hayPartido", true);
	            respuesta.put("idPartido", rs.getInt("id"));
	            respuesta.put("pais1", rs.getString("pais1"));
	            respuesta.put("bandera1", rs.getString("bandera1"));
	            respuesta.put("pais2", rs.getString("pais2"));
	            respuesta.put("bandera2", rs.getString("bandera2"));
	            respuesta.put("fechaPartido", rs.getString("fecha_partido"));

	        } else {

	            respuesta.put("success", true);
	            respuesta.put("hayPartido", false);
	            respuesta.put("message", "No hay partidos disponibles.");
	        }

	        rs.close();
	        ps.close();
	        con1.close();

	    } catch (Exception e) {

	    	System.out.println("Error obteniendo partido publicado: " + e.toString());

	        try {
	            con1.close();
	        } catch (Exception e1) {}

	        respuesta.put("success", false);
	        respuesta.put("message", "Error interno del servidor.");
	    }

	    return respuesta;
	}
	
	public static boolean existePronosticoFactura(String numeroFactura) {


	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();

	    try {

	        String sql =
	                "SELECT 1 " +
	                "FROM pronosticos " +
	                "WHERE numero_factura = ? " +
	                "LIMIT 1";

	        PreparedStatement ps = con1.prepareStatement(sql);
	        ps.setString(1, numeroFactura);

	        ResultSet rs = ps.executeQuery();

	        boolean existe = rs.next();

	        rs.close();
	        ps.close();
	        con1.close();

	        return existe;

	    } catch (Exception e) {

	        System.out.println("Error validando factura: " + e.toString());

	        try {
	            con1.close();
	        } catch (Exception e1) {}

	        return false;
	    }
	}
}
