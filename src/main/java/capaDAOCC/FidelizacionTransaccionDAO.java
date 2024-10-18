package capaDAOCC;

import java.sql.Connection;
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
	
	public static boolean insertarFidelizacionTransaccion(FidelizacionTransaccion transaccion)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into fidelizacion_transaccion (correo,idtienda, idpedidotienda, valor_neto, puntos) values ('" + transaccion.getCorreo() +"' ," + transaccion.getIdTienda()+ " ," +  transaccion.getIdPedidoTienda()+ "," + transaccion.getValorNeto() + ", " + transaccion.getPuntos() +")";
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
			String select = "SELECT a.*, b.nombre AS tienda FROM fidelizacion_transaccion a, tienda b WHERE a.idtienda = b.idtienda AND a.correo = '" + correo + "';";
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
	
}
