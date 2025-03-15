package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class LogAvanzarEstadoAppDAO {

	public static int insertarLogPedidoVirtual(int idTienda, int idPedidoTienda, String claveUsuario, String observacion)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_avanzar_estado_app (idtienda, idpedidotienda,claveusuario,observacion) values (" + idTienda +"," + idPedidoTienda + ", '" + claveUsuario +"' , '" + observacion + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id log  en bd " + idEventoIns);
	        }
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
			return(0);
		}
		return(idEventoIns);
	}
	
	public static void actualizarLogPedidoVirtual(int idLog, String respuesta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update log_avanzar_estado_app set respuesta = '" + respuesta +"' where idlog = " + idLog; 
			logger.info(update);
			stm.executeUpdate(update);
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
