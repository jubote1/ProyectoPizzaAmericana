package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.TercerizadoDomicilioEvento;
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
	
	
	public static int insertarLogCRMBOT(String datosJSON, String authHeader, String operacion) {
	    Logger logger = Logger.getLogger("log_file");  
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    

	    int idEventoIns = 0;

	    String sql = "INSERT INTO log_pedido_crmbot (datos_json, header, tipo) VALUES (?, ?, ?)";

	    try (PreparedStatement ps = con1.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

	        ps.setString(1, datosJSON);
	        ps.setString(2, authHeader);
	        ps.setString(3, operacion);

	        logger.info("Insertando log CRMBOT");

	        ps.executeUpdate();

	        try (ResultSet rs = ps.getGeneratedKeys()) {
	            if (rs.next()) {
	                idEventoIns = rs.getInt(1);
	                logger.info("Id log pedido crmbot en bd: " + idEventoIns);
	            }
	        }

	    } catch (Exception e) {
	        logger.error("Error insertando log CRMBOT: " + e.toString());
	        return 0;
	    }finally {
        try {
            if (con1 != null) con1.close();
        } catch (Exception e) {
            logger.error("Error cerrando conexión: " + e.toString());
        }
    }

	    return idEventoIns;
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
	
	public static void actualizarLogCRMBOT(int idLog, String JSON, String lead, String tipo) {
	    Logger logger = Logger.getLogger("log_file");  
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = null;

	    try {
	        con1 = con.obtenerConexionBDPrincipal();

	        // Convertir lead a entero, con fallback a 0
	        int lead_num = 0;
	        try {
	            lead_num = Integer.parseInt(lead);
	        } catch (Exception e) {
	            lead_num = 0;
	        }

	        System.out.println("idLog: " + idLog);

	        // UPDATE que siempre cuenta como actualizado (usando un truco: SET con mismo valor + condición WHERE idlog)
	        String sql = "UPDATE log_pedido_crmbot SET info_lead = ?, tipo = ?, lead_id = ? WHERE idlog = ?";
	        try (PreparedStatement ps = con1.prepareStatement(sql)) {
	            ps.setString(1, JSON);
	            ps.setString(2, tipo);
	            ps.setInt(3, lead_num);
	            ps.setInt(4, idLog);

	            logger.info("Ejecutando update seguro para idlog: " + idLog);
	            int filas = ps.executeUpdate();
	            System.out.println("FILAS ACTUALIZADAS: " + filas);
	        }

	    } catch (Exception e) {
	        logger.error("Error actualizando log: " + e.toString());
	        e.printStackTrace();
	    } finally {
	        try {
	            if (con1 != null && !con1.isClosed()) {
	                con1.close();
	            }
	        } catch (Exception e) {
	            logger.error("Error cerrando conexión: " + e.toString());
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
	
	public static boolean existePedidoRecienteCRMBOT(String lead, String tipo, int minutosVentana) {
	    Logger logger = Logger.getLogger("log_file");  
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    int lead_num = 0;
	    try {
	        lead_num = Integer.parseInt(lead);
	    } catch (Exception e) {
	        return false;
	    }

	    String sql = "SELECT 1 FROM log_pedido_crmbot " +
	                 "WHERE lead_id = ? " +
	                 "AND tipo = ? " +
	                 "AND fecha_real >= NOW() - INTERVAL ? MINUTE " +
	                 "LIMIT 1";
    try (PreparedStatement ps = con1.prepareStatement(sql)) {

	        ps.setInt(1, lead_num);
	        ps.setString(2, tipo);
	        ps.setInt(3, minutosVentana);

	        ResultSet rs = ps.executeQuery();
	        return rs.next();

	    } catch (Exception e) {
	        logger.error("Error validando duplicado: " + e.toString());
	    } finally {
	        try {
	            if (con1 != null) con1.close();
	        } catch (Exception e) {
	            logger.error("Error cerrando conexión: " + e.toString());
	        }
	    }

	    return false;
	}

	
	public static boolean actualizarLogCRMBOTIdPedido(int idLog, int idPedido) {

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    PreparedStatement ps = null;

	    try {

	        String sql = "UPDATE log_pedido_crmbot " +
	                     "SET idpedido = ? " +
	                     "WHERE idlog = ?";

	        ps = con1.prepareStatement(sql);

	        ps.setInt(1, idPedido);
	        ps.setInt(2, idLog);

	        int filasActualizadas = ps.executeUpdate();

	        return filasActualizadas > 0;

	    } catch (Exception e) {

	        System.out.println(
	            "Error actualizando idpedido en log CRM BOT: " + e.toString()
	        );

	        e.printStackTrace();

	        return false;

	    } finally {

	        try {
	            if (ps != null) {
	                ps.close();
	            }
	        } catch (Exception e) {
	        }

	        try {
	            if (con1 != null) {
	                con1.close();
	            }
	        } catch (Exception e) {
	        }
	    }
	}



}
