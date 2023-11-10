package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class IntegracionCRMDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static IntegracionCRM obtenerInformacionIntegracion(String crm)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		IntegracionCRM respuesta = new IntegracionCRM("",0,"","","","");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from integracion_crm where crm = '" + crm +"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idTienda = rs.getInt("idtienda");
				String accessToken = rs.getString("access_token");
				String freshToken = rs.getString("fresh_token");
				String clientId = rs.getString("client_id");
				String appShopID = rs.getString("app_shop_id");
				respuesta = new IntegracionCRM(crm,idTienda, accessToken, freshToken, clientId, appShopID);
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
	
	/**
	 * Se recibe un arrayList de las integraciones asociadas a un solo nombre pero que se repite debido a que es una integración
	 * por tienda.
	 * @param crm
	 * @return
	 */
	public static ArrayList<IntegracionCRM> obtenerInformacionIntegracionMultiple(String crm)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<IntegracionCRM> integraciones = new ArrayList();
		IntegracionCRM respuesta = new IntegracionCRM("",0,"","","","");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from integracion_crm where crm = '" + crm +"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idTienda = rs.getInt("idtienda");
				String accessToken = rs.getString("access_token");
				String freshToken = rs.getString("fresh_token");
				String clientId = rs.getString("client_id");
				String appShopID = rs.getString("app_shop_id");
				respuesta = new IntegracionCRM(crm,idTienda, accessToken, freshToken, clientId, appShopID);
				integraciones.add(respuesta);
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
		return(integraciones);
		
	}
	
	/**
	 * Método que se encarga de recuperar la información de la integración por el nombre de la integración y por el id de la tienda.
	 * @param crm
	 * @param idTienda
	 * @return
	 */
	public static IntegracionCRM obtenerInformacionIntegracionXTienda(String crm, int idTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		IntegracionCRM respuesta = new IntegracionCRM("",0,"","","","");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from integracion_crm where crm = '" + crm +"' and idtienda = " + idTienda;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				String accessToken = rs.getString("access_token");
				String freshToken = rs.getString("fresh_token");
				String clientId = rs.getString("client_id");
				String appShopID = rs.getString("app_shop_id");
				respuesta = new IntegracionCRM(crm,idTienda, accessToken, freshToken, clientId, appShopID);
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
	
	/**
	 * Método qeu se encarga de retornar el id de un Municipio dado el nombre de un Munipio
	 * @param municipio Se recibe como parámetro un String con el nombre del Municipio.
	 * @return Se retorna un entero con el id del municipio según el nombre del Municipio enviado como parámetro.
	 */
	public static void actualizarTokenIntegracionCRM(String crm, String accessToken, String freshToken)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update integracion_crm set access_token = '" + accessToken +"', fresh_token = '" + freshToken +"' where crm = '"+ crm + "'";
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
		}catch (Exception e){
			System.out.println(e.toString());
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
	 * Se realiza la actualización de los datos del token de actualización pero recibiendo el nombre de la integración y el idTienda
	 * @param crm
	 * @param idTienda
	 * @param accessToken
	 * @param freshToken
	 */
	public static void actualizarTokenIntegracionCRMMultiple(String crm, int idTienda, String accessToken, String freshToken)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update integracion_crm set access_token = '" + accessToken +"', fresh_token = '" + freshToken +"' where crm = '"+ crm + "' and idtienda =" + idTienda;
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
		}catch (Exception e){
			System.out.println(e.toString());
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
	 * Método que permite recuperar de que tienda es la integración enviando el appShopId
	 * @param appShopId
	 * @return
	 */
	public static int obtenerIdTiendaIntegracion(String appShopId)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idTienda = 0;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idtienda from integracion_crm where app_shop_id = '" + appShopId +"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idTienda = rs.getInt(1);
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
		return(idTienda);
		
	}
}
