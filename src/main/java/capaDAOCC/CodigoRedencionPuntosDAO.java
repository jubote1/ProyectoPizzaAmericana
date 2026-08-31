package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.CodigoRedencionPuntos;
import capaModeloCC.LogEventoWompi;
import capaModeloCC.PedidoPrecioEmpleado;
import conexionCC.ConexionBaseDatos;

public class CodigoRedencionPuntosDAO {

	public static void insertarCodigoRedencionPuntos(CodigoRedencionPuntos codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			//Se agregan idtienda y usuario para saber desde donde y quien pidio el
			//codigo. Se usa PreparedStatement porque el usuario es texto de entrada.
			String insert = "insert into codigo_redencion_puntos (correo,codigo,puntos,fecha_sistema,idtienda,usuario) values(?,?,?,?,?,?)";
			logger.info(insert + " correo=" + codigo.getCorreo() + " tienda=" + codigo.getIdTienda()
					+ " usuario=" + codigo.getUsuario());
			PreparedStatement pst = con1.prepareStatement(insert);
			pst.setString(1, codigo.getCorreo());
			pst.setString(2, codigo.getCodigo());
			pst.setDouble(3, codigo.getPuntos());
			pst.setString(4, codigo.getFechaSistema());
			pst.setInt(5, codigo.getIdTienda());
			pst.setString(6, codigo.getUsuario() == null ? "" : codigo.getUsuario());
			pst.executeUpdate();
			pst.close();
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
	
	public static void marcarValidadoRedencionPuntos(String codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update codigo_redencion_puntos set validado = 'S' where codigo = '" + codigo + "'";
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
	
	
	/**
	 * Método que permite validar la existencia de códigos dentro de los códigos para validar la redención de puntos
	 * @param codigo
	 * @return
	 */
	public static boolean validarExistenciaCodigo(String codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		boolean resultado = false;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from codigo_redencion_puntos  where codigo = '" + codigo + "'"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				resultado = true;
			}
			stm.close();
			rs.close();
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
	
	public static boolean validarExistenciaCodigoRedencion(String codigo, String correo, String fechaSistema)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean resultado = false;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from codigo_redencion_puntos  where codigo = '" + codigo + "' and correo = '" + correo + "' and fecha_sistema = '" + fechaSistema +"' and validado  ='N'"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				resultado = true;
			}
			stm.close();
			rs.close();
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
