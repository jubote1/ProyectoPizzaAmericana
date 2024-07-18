package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.HomologacionTiendaToken;
import conexionCC.ConexionBaseDatos;

public class HomologacionTiendaTokenDAO {
	
	public static HomologacionTiendaToken obtenerHomologacionTiendaToken(int token)
	{
		Logger logger = Logger.getLogger("log_file");
		HomologacionTiendaToken homoResp = new HomologacionTiendaToken(0,0,"");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from homologacion_tienda_token where token = " + token; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idTienda = 0;
			String origen = "";
			while(rs.next()){
				idTienda = rs.getInt("idtienda");
				origen = rs.getString("origen");
				homoResp = new HomologacionTiendaToken(token, idTienda, origen);
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
		return(homoResp);
	}
	

}
