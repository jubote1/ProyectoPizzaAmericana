package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FidelizacionTransaccion;
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
public class FidelizacionTransaccionDAO {

	
	public static boolean existeFidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from fidelizacion_transaccion where correo = '"+ correo + "' and idtienda = " + idTienda + " and idpedidotienda = " + idPedidoTienda;
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
	
	public static boolean insertarFidelizacionTransaccion(FidelizacionTransaccion transaccion , int diasVigencia)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into fidelizacion_transaccion (correo,idtienda, idpedidotienda, valor_neto, puntos, fecha_vencimiento) values ('" + transaccion.getCorreo() +"' ," + transaccion.getIdTienda()+ " ," +  transaccion.getIdPedidoTienda()+ "," + transaccion.getValorNeto() + ", " + transaccion.getPuntos() + ", " + "DATE_ADD(CURDATE(), INTERVAL " + diasVigencia + " DAY)" +")";
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
	
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransacciones(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND a.correo = '" + correo + "'";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				transacciones.add(tranTemp);
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
		return(transacciones);
	}
	
	
	/**
	 * Método que obtiene la transacciones de fidelización vencidas dados los parámetros.
	 * @return
	 */
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransaccionesVencimiento()
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		double puntosRedimidos;
		String correo;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND a.fecha_vencimiento < CURDATE() AND vencidos = 'N' and puntos > puntos_redimidos";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				correo = rs.getString("correo");
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				puntosRedimidos = rs.getDouble("puntos_redimidos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				tranTemp.setPuntosRedimidos(puntosRedimidos);
				transacciones.add(tranTemp);
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
		return(transacciones);
	}
	
	/**
	 *Marca como vencida una transacción de fidelización 
	 * @param correo
	 * @param idTienda
	 * @param idPedidoTienda
	 * @param puntosVencidos
	 * @return
	 */
	public static boolean vencerFidelizacionTransaccion(String correo,int idTienda, int idPedidoTienda, double puntosVencidos)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "UPDATE fidelizacion_transaccion SET vencidos = 'S' , puntos_vencidos = " + puntosVencidos + " WHERE correo = '" + correo + "' AND idtienda = " + idTienda +  " AND idpedidotienda =" + idPedidoTienda;
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
	
	public static ArrayList<FidelizacionTransaccion> obtenerFidelizacionTransaccionesRedencion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<FidelizacionTransaccion> transacciones = new ArrayList();
		FidelizacionTransaccion tranTemp;
		int idTienda;
		String tienda;
		int idPedidoTienda;
		String fechaTransaccion;
		double valorNeto;
		double puntos;
		double puntosRedimidos;
		double puntosVencidos;
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND vencidos = 'N' and puntos > puntos_redimidos and correo = '" + correo + "' ORDER BY fecha_transaccion desc";
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				correo = rs.getString("correo");
				idTienda = rs.getInt("idtienda");
				tienda = rs.getString("tienda");
				idPedidoTienda = rs.getInt("idpedidotienda");
				fechaTransaccion = rs.getString("fecha_transaccion");
				valorNeto = rs.getDouble("valor_neto");
				puntos = rs.getDouble("puntos");
				puntosRedimidos = rs.getDouble("puntos_redimidos");
				tranTemp = new FidelizacionTransaccion(correo,idTienda,tienda, idPedidoTienda, fechaTransaccion, valorNeto, puntos);
				tranTemp.setPuntosRedimidos(puntosRedimidos);
				transacciones.add(tranTemp);
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
		return(transacciones);
	}
	
	public static boolean actualizarPuntosRedimidosFidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda, double puntosRedimidos)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			PreparedStatement pstmt = con1.prepareStatement(
		             "update fidelizacion_transaccion set puntos_redimidos = ? where correo = ? and idtienda = ? and idpedidotienda = ?");
		    pstmt.setDouble(1, puntosRedimidos);
		    pstmt.setString(2, correo);
		    pstmt.setInt(3, idTienda);
		    pstmt.setInt(4, idPedidoTienda);
		    pstmt.executeUpdate();

			respuesta = true;
			pstmt.close();
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
	
}
