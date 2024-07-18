package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class UbicacionDomiciliarioDAO {

	public static void insertarUbicacionDomiciliario(String claveDomiciliario, int idTienda, float latitud, float longitud)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into ubicacion_domiciliario (clave_dom,idtienda,latitud,longitud) values ('" + claveDomiciliario + "' ," + idTienda + " , " + latitud + " , " + longitud   +")"; 
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

}
