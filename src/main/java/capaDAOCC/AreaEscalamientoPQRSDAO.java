package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import capaModeloCC.AreaEscalamiento;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class AreaEscalamientoPQRSDAO {
	

/**
 * Método que se encarga de retornar los correos para notificación de un área
 * @param area
 * @return
 */
	public static ArrayList<String> obtenerCorreoAreaEscalamientoPQRS(String area)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String> correos = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String correoTokenizar = "";
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select correos from area_escalamiento_pqrs where area = '"+ area + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				correoTokenizar = rs.getString(1);
				break;
			}
			rs.close();
			stm.close();
			con1.close();
			//Realizamos al tokenizacion para los varios correos que podrian haber
			StringTokenizer tokenizar = new StringTokenizer(correoTokenizar,";");
			while (tokenizar.hasMoreTokens()) {
			    String token = tokenizar.nextToken();
			    correos.add(token);
			}
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(correos);
	}
	
	public static List<AreaEscalamiento> obtenerAreaEscalamientoPQRS() {
	    Logger logger = Logger.getLogger("log_file");
	    List<AreaEscalamiento> areas = new ArrayList<>();

	    String sql = "SELECT idarea, area ,correos ,inicio_horario, final_horario FROM area_escalamiento_pqrs";

	    try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal();
	         PreparedStatement ps = con.prepareStatement(sql)) {

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	            	int idarea =  rs.getInt("idarea");
	                Time inicio = rs.getTime("inicio_horario");
	                Time fin = rs.getTime("final_horario");
	                String area = rs.getString("area");
	                String correos = rs.getString("correos");

	                AreaEscalamiento area_escalamiento = new AreaEscalamiento();
	                area_escalamiento.setIdarea(idarea); 
	                area_escalamiento.setArea(area);
	                area_escalamiento.setCorreos(correos);
	                area_escalamiento.setInicio_horario( inicio != null ? inicio.toString() : null);
	                area_escalamiento.setFinal_horario(fin != null ? fin.toString() : null);

	                areas.add(area_escalamiento);
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("Error en consulta areas: " + e.getMessage());
	        logger.error("Error en consulta - areas", e);
	    }

	    return areas;
	}

	
	
}
