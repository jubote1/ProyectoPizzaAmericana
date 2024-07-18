package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogBloqueoTienda;
import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class LogBloqueoTiendaDAO {
	
	public static LogBloqueoTienda obtenerUltimoBloqueoTienda(int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		LogBloqueoTienda logBloqueo = new LogBloqueoTienda();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.url FROM log_bloqueo_tienda a, tienda b WHERE a.idtienda = b.idtienda and a.idtienda = " + idTienda + " AND a.accion = 'BLOQUEO' ORDER BY a.fecha_accion DESC LIMIT 1"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idLogBloqueo;
			String accion;
			String fechaAccion;
			String motivo;
			String observacion;
			String debloqueoEn;
			String urlTienda;
			while(rs.next())
			{
				idLogBloqueo = rs.getInt("idlogbloqueo");
				accion = rs.getString("accion");
				fechaAccion = rs.getString("fecha_accion");
				motivo = rs.getString("motivo");
				observacion = rs.getString("observacion");
				debloqueoEn = rs.getString("debloqueo_en");
				urlTienda = rs.getString("url");
				logBloqueo = new LogBloqueoTienda(idLogBloqueo, idTienda, accion, fechaAccion, motivo, observacion, debloqueoEn,urlTienda);
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
		}
		return(logBloqueo);
	}
	
	
	public static ArrayList<LogBloqueoTienda> obtenerHistorialTienda(int idTienda, String fechaAnterior, String fechaPosterior)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<LogBloqueoTienda> logBloqueosTienda = new ArrayList();
		LogBloqueoTienda logBloqueo = new LogBloqueoTienda();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, '' as url FROM log_bloqueo_tienda a WHERE  a.idtienda = " + idTienda + " AND a.fecha_accion >= '"+ fechaAnterior +" 00:00:00' AND a.fecha_accion <= '" + fechaPosterior +" 23:59:00'  ORDER BY a.fecha_accion asc "; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idLogBloqueo;
			String accion;
			String fechaAccion;
			String motivo;
			String observacion;
			String debloqueoEn;
			String urlTienda;
			while(rs.next())
			{
				idLogBloqueo = rs.getInt("idlogbloqueo");
				accion = rs.getString("accion");
				fechaAccion = rs.getString("fecha_accion");
				motivo = rs.getString("motivo");
				observacion = rs.getString("observacion");
				debloqueoEn = rs.getString("debloqueo_en");
				urlTienda = rs.getString("url");
				logBloqueo = new LogBloqueoTienda(idLogBloqueo, idTienda, accion, fechaAccion, motivo, observacion, debloqueoEn,urlTienda);
				logBloqueosTienda.add(logBloqueo);
			}
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(logBloqueosTienda);
	}
	
	public static boolean consultarBloqueosAprobados(int idTienda, String fecha)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT * FROM log_bloqueo_tienda a WHERE a.idtienda = " + idTienda + " and fecha_accion >= '" +fecha + " 00:00:00'" + " and fecha_accion <= '" + fecha +" 23:59:59' and a.aprobado = 0 and a.accion = 'BLOQUEO'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				resultado = true;
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
		}
		return(resultado);
	}
	
	public static ArrayList<LogBloqueoTienda> consultarBloqueosTienda(int idTienda, String fecha)
	{
		
		Logger logger = Logger.getLogger("log_file");
		ArrayList<LogBloqueoTienda> logBloqueosTienda = new ArrayList();
		LogBloqueoTienda logBloqueo = new LogBloqueoTienda();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre FROM log_bloqueo_tienda a, tienda b WHERE a.idtienda = b.idtienda and a.idtienda = " + idTienda + " and fecha_accion >= '" +fecha + " 00:00:00'" + " and fecha_accion <= '" + fecha +" 23:59:59' and a.accion = 'BLOQUEO'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idLogBloqueo;
			String accion;
			String fechaAccion;
			String motivo;
			String observacion;
			String debloqueoEn;
			String tienda;
			String aprobado;
			int binAprobado;
			while(rs.next())
			{
				idLogBloqueo = rs.getInt("idlogbloqueo");
				accion = rs.getString("accion");
				fechaAccion = rs.getString("fecha_accion");
				motivo = rs.getString("motivo");
				observacion = rs.getString("observacion");
				debloqueoEn = rs.getString("debloqueo_en");
				binAprobado = rs.getInt("aprobado");
				if(binAprobado == 1)
				{
					aprobado = "S";
				}else
				{
					aprobado = "N";
				}
				tienda = rs.getString("nombre");
				logBloqueo = new LogBloqueoTienda(idLogBloqueo, idTienda, accion, fechaAccion, motivo, observacion, debloqueoEn,"");
				logBloqueo.setTienda(tienda);
				logBloqueo.setAprobado(aprobado);
				logBloqueosTienda.add(logBloqueo);
			}
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(logBloqueosTienda);
	}
	
	public static boolean aprobarBloqueoTienda(int idLogBloqueo)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update log_bloqueo_tienda set aprobado = 1 WHERE idlogbloqueo = " + idLogBloqueo;
			logger.info(update);
			stm.executeUpdate(update);
			resultado = true;
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
		return(resultado);
	}

}
