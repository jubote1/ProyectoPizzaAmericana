package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.HomologacionTiendaToken;
import conexionCC.ConexionBaseDatos;

public class IntegracionCRMHomologaAsesorDAO {
	
	public static String obtenerHomologacionAsesorCRM(String asesor)
	{
		Logger logger = Logger.getLogger("log_file");
		String nombre = "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select nombre from integracion_crm_homologa_asesor where asesor = '" + asesor + "'"; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				nombre = rs.getString("nombre");
				break;
			}
			rs.close();
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
		return(nombre);
	}
	

}
