package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.TiendaFranquiciaWompi;
import conexionCC.ConexionBaseDatos;

public class TiendaFranquiciaWompiDAO {
	
	public static String obtenerTiendaFranquiciaWompiDAO(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String apiLlavePrivada = "";
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select api_llave_privada from tienda_franquicia_wompi where idtienda = "+ idTienda+ "";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				apiLlavePrivada = rs.getString("api_llave_privada");
				break;
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(apiLlavePrivada);
	}
	
	public static TiendaFranquiciaWompi obtenerTiendaFranquiciaWompi(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String apiLlavePrivada = "";
		String apiLlavePublica = "";
		String correo = "";
		TiendaFranquiciaWompi retorno = new TiendaFranquiciaWompi(0,"", "", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from tienda_franquicia_wompi where idtienda = "+ idTienda+ "";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				apiLlavePrivada = rs.getString("api_llave_privada");
				apiLlavePublica = rs.getString("api_llave_publica");
				correo = rs.getString("correo");
				retorno = new TiendaFranquiciaWompi(idTienda,apiLlavePublica, apiLlavePrivada, correo );
				break;
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(retorno);
	}

}
