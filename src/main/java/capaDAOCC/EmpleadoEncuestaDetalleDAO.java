package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import capaModeloCC.EmpleadoEncuestaDetalle;
import conexionCC.ConexionBaseDatos;


public class EmpleadoEncuestaDetalleDAO {

	public static JSONObject insertarEmpleadoEncuestaDetalle(List<EmpleadoEncuestaDetalle> lista, float porcentaje_total) {
	    JSONObject result = new JSONObject();
	    JSONArray errores = new JSONArray();
	    int failedCount = 0;

	    String sql = "INSERT INTO empleado_encuesta_detalle "
	               + "(idempleadoencuesta, idencuestadetalle, respuesta_si, respuesta_no, observacion, observacion_adi) "
	               + "VALUES (?, ?, ?, ?, ?, ?)";

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    try (Connection conn = con.obtenerConexionBDGeneral();
	         PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

	        conn.setAutoCommit(false);

	        for (EmpleadoEncuestaDetalle det : lista) {
	            try {
	                pstmt.setInt(1, det.getIdEmpleadoEncuesta());
	                pstmt.setInt(2, det.getIdEncuestaDetalle());
	                pstmt.setString(3, det.getRespuestaSi());
	                pstmt.setString(4, det.getRespuestaNo());
	                pstmt.setString(5, det.getObservacion());
	                pstmt.setString(6, det.getObservacionAdicional());
	                pstmt.executeUpdate();

	            } catch (SQLException exItem) {
	                System.out.println("Error insertando detalle con ID " + det.getIdEncuestaDetalle() + ": " + exItem.toString());
	                errores.put(det.getIdEncuestaDetalle());
	                failedCount++;
	            }
	        }

	        conn.commit();

	    } catch (Exception e) {
	    	  System.out.println("Error en bloque de inserción: " + e.toString());
	        failedCount = lista.size();
	        for (EmpleadoEncuestaDetalle det : lista) {
	            errores.put(det.getIdEncuestaDetalle());
	        }
	    }
	    boolean act = true;
	    if(porcentaje_total  != 0) {
	    	 // Actualizar porcentaje
		   act = actualizarPorcentajeTotalEncuesta(lista.get(0).getIdEmpleadoEncuesta(), porcentaje_total);

	    }
	   
	    result.put("errors_insercion", errores);
	    result.put("failed_count", failedCount);
	    result.put("porcentaje_actualizado", act);
	    result.put("success", failedCount == 0);

	    
	 
	    return result;
	}

	public static boolean actualizarPorcentajeTotalEncuesta(int idempleadoencuesta, float porcentaje_total) {
	    String sqlUpdate = "UPDATE empleado_encuesta "
	                     + "SET porcentaje_total = ? "
	                     + "WHERE idempleadoencuesta = ?";

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    try (Connection conn = con.obtenerConexionBDGeneral();
	         PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {

	        pstmt.setFloat(1, porcentaje_total);
	        pstmt.setInt(2, idempleadoencuesta);

	        int affectedRows = pstmt.executeUpdate();
	        return affectedRows > 0;

	    } catch (Exception e) {
	    	  System.out.println("Error actualizando porcentaje_total: " + e.toString());
	        return false;
	    }
	}

}
