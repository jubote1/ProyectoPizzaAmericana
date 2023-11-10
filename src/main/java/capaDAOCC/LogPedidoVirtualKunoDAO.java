package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class LogPedidoVirtualKunoDAO {

	public static int insertarLogPedidoVirtualKuno(String datosJSON, String authHeader)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_pedido_virtual_kuno (datos_json, header) values ('" + datosJSON + "' , '" + authHeader + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id log pedido virtual en bd " + idEventoIns);
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
	
	
	public static int insertarLogCRMBOT(String datosJSON, String authHeader)
	{
		Logger logger = Logger.getLogger("log_file");  
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_pedido_crmbot (datos_json, header) values ('" + datosJSON + "' , '" + authHeader + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id log pedido crmbot en bd " + idEventoIns);
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
	
	public static int insertarLogRAPPI(String datosJSON, String authHeader)
	{
		Logger logger = Logger.getLogger("log_file");  
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_pedido_rappi (datos_json, header) values ('" + datosJSON + "' , '" + authHeader + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id log pedido crmbot en bd " + idEventoIns);
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
	
	public static int insertarLogDIDI(String datosJSON, String authHeader)
	{
		Logger logger = Logger.getLogger("log_file");  
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_pedido_didi (datos_json, header) values ('" + datosJSON + "' , '" + authHeader + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id log pedido crmbot en bd " + idEventoIns);
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
	
	public static void actualizarLogCRMBOT(int idLog, String JSON, String tipo)
	{
		Logger logger = Logger.getLogger("log_file");  
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update log_pedido_crmbot set info_lead = '" + JSON + "' , tipo = '" + tipo + "' where idlog =" + idLog; 
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
	
	public static void actualizarLogCRMBOTInfLog(int idLog, String log)
	{
		Logger logger = Logger.getLogger("log_file");  
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update log_pedido_crmbot set log = '" + log + "' where idlog =" + idLog; 
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
