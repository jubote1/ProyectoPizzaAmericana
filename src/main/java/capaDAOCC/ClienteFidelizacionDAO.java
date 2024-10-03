package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class ClienteFidelizacionDAO {

	public static ClienteFidelizacion obtenerClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		ClienteFidelizacion cliente = new ClienteFidelizacion();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from cliente_fidelizacion where correo = '"+ correo + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String fechaVinculacion = "";
			String activo = "";
			double puntosVigentes = 0;
			while(rs.next()){
				fechaVinculacion = rs.getString("fecha_vinculacion");
				activo = rs.getString("activo");
				puntosVigentes = rs.getDouble("puntos_vigentes");
				cliente = new ClienteFidelizacion(correo, fechaVinculacion, activo, puntosVigentes);
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
		return(cliente);
	}
	
	
	public static boolean existeClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from cliente_fidelizacion where correo = '"+ correo + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				respuesta = true;
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
		return(respuesta);
	}
	
	public static boolean desactivarClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update cliente_fidelizacion set activo = 'N' where correo = '"+ correo + "'";
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}
	
	
	public static boolean activarClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update cliente_fidelizacion set activo = 'S' where correo = '"+ correo + "'";
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}
	
	public static boolean sumarPuntosClienteFidelizacion(String correo, double puntosSumar)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update cliente_fidelizacion a set a.puntos_vigentes = a.puntos_vigentes + " + puntosSumar +  " where a.correo = '"+ correo + "'";
			logger.info(update);
			stm.executeUpdate(update);
			respuesta = true;
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
		return(respuesta);
	}

	public static boolean insertarClienteFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into cliente_fidelizacion (correo) values ('" + correo +"')";
			logger.info(insert);
			stm.executeUpdate(insert);
			respuesta = true;
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
		return(respuesta);
	}
	
	public static double obtenerCantidadPuntosCliente(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		double puntosVigentes = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select puntos_vigentes from cliente_fidelizacion where correo = '"+ correo + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				puntosVigentes = rs.getDouble("puntos_vigentes");
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
		return(puntosVigentes);
	}
	
}
