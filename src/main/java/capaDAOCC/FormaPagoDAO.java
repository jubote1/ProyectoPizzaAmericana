package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EstadoPedido;
import capaModeloCC.FormaPago;
import conexionCC.ConexionBaseDatos;

/**
 * Clase que se encarga de implementar los métodos que se encargarán de la interacción de base de datos con la entidad
 * FormaPago
 * @author JuanDavid
 *
 */
public class FormaPagoDAO {
	
	/**
	 * Método que se encarga de insertar una forma de pago en la base de datos con base en la información recibida
	 * como parámetro
	 * @param forma Se recibe objeto de Modelo FormaPago que contiene los valores de la entidad a insertar en la base de datos.
	 * 
	 * @return Se retorna el id de forma de pago retornado en la inserción de la entidad en la base de datos.
	 */
	public static int insertarFormaPago(FormaPago forma)
	{
		Logger logger = Logger.getLogger("log_file");
		int idFormaPagoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into forma_pago (nombre, tipoformapago) values ('" + forma.getNombre() + "', '" + forma.getTipoforma() + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idFormaPagoIns =rs.getInt(1);
				logger.info("Id forma pago insertada en bd " + idFormaPagoIns);
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
		return(idFormaPagoIns);
	}

	/**
	 * Método que se encarga de la eliminación de una Forma de pago con base en la información de parámetros enviada.
	 * @param idFormaPago Se recibe como parámetro el idformapago que se desea eliminar.
	 */
	public static void eliminarFormaPago(int idFormaPago)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from forma_pago  where idforma_pago = " + idFormaPago; 
			logger.info(delete);
			stm.executeUpdate(delete);
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
	 * Método que se encarga de retornar la informacion de una forma de pago con base en el parámetro recibido.
	 * @param idFormaPago Se recibe como parámetro un entero con el id forma de pago
	 * @return Se retorna un objeto Modelo Forma de pago con la información de la forma de pago cosultada.
	 */
	public static FormaPago retornarFormaPago(int idFormaPago)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		FormaPago forma = new FormaPago(0,"", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idforma_pago, a.nombre, a.tipoformapago, a.virtual, a.mensaje_texto, a.mensaje_correo from  forma_pago a  where a.idforma_pago = " + idFormaPago; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idforma = 0;
			String nombr = "";
			String tipoforma = "";
			String virtual = "";
			String mensajeTexto  = "";
			String mensajeCorreo = "";
			while(rs.next()){
				idforma = rs.getInt("idforma_pago");
				nombr = rs.getString("nombre");
				tipoforma = rs.getString("tipoformapago");
				forma = new FormaPago(idforma, nombr, tipoforma);
				virtual = rs.getString("virtual");
				forma.setVirtual(virtual);
				mensajeTexto = rs.getString("mensaje_texto");
				forma.setMensajeTexto(mensajeTexto);
				mensajeCorreo = rs.getString("mensaje_correo");
				forma.setMensajeCorreo(mensajeCorreo);
				break;
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
		return(forma);
	}

	/**
	 * Método que se encarga de editar una forma de pago con base en los parámetros recibidos
	 * @param forma Se recibe como parámetro un objeto Modelo Forma Pago con los valores base para la modificación.
	 * @return se retorna un valor tipo String con el resultado del proceso.
	 */
	public static String editarFormaPago(FormaPago forma)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update forma_pago set nombre ='" + forma.getNombre() + "', tipoformapago =  '" + forma.getTipoforma() + "' where idforma_pago = " + forma.getIdformapago(); 
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
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
			resultado = "error";
		}
		return(resultado);
	}
	
	/**
	 * Método que se encarga de retornar todas las formas pago definidas en base de datos
	 * @return Se retorna un ArrayList con todos los objetos Forma Pago definidos en el sistema.
	 */
	public static ArrayList<FormaPago> obtenerFormasPago(String tipoPago)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<FormaPago> formaspago = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "";
			if(tipoPago.equals(new String("T")))
			{
				consulta = "select a.idforma_pago, a.nombre, a.tipoformapago, a.virtual, a.mensaje_texto, a.mensaje_correo from forma_pago a where a.estado = 'A'";
			}else if(tipoPago.equals(new String("PV")))
			{
				consulta = "select a.idforma_pago, a.nombre, a.tipoformapago, a.virtual, a.mensaje_texto, a.mensaje_correo from forma_pago a where a.estado = 'A' and a.virtual = 'S'";
			}
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idformapago;
			String nombre;
			String tipoformapago;
			String virtual;
			String mensajeTexto;
			String mensajeCorreo;
			while(rs.next()){
				idformapago = rs.getInt("idforma_pago");
				nombre = rs.getString("nombre");
				tipoformapago = rs.getString("tipoformapago");
				virtual = rs.getString("virtual");
				try
				{
					mensajeTexto = rs.getString("mensaje_texto");
				}catch(Exception e)
				{
					mensajeTexto = "";
				}
				try
				{
					mensajeCorreo = rs.getString("mensaje_correo");
				}catch(Exception e)
				{
					mensajeCorreo = "";
				}
				FormaPago forma = new FormaPago( idformapago,nombre, tipoformapago);
				forma.setVirtual(virtual);
				forma.setMensajeTexto(mensajeTexto);
				forma.setMensajeCorreo(mensajeCorreo);
				formaspago.add(forma);
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
		return(formaspago);
		
	}
	
	
	/**
	 * Retorna las formas de pago que se deberán ver para un perfil normal de contact center
	 * @return
	 */
	public static ArrayList<FormaPago> obtenerFormasPagoContact(String tipoPago)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<FormaPago> formaspago = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "";
			if(tipoPago.equals(new String("T")))
			{
				consulta = "select a.idforma_pago, a.nombre, a.tipoformapago, a.virtual, a.mensaje_texto, a.mensaje_correo from forma_pago a where a.estado = 'A' and a.contact = 'S'";
			}else if(tipoPago.equals(new String("PV")))
			{
				consulta = "select a.idforma_pago, a.nombre, a.tipoformapago, a.virtual, a.mensaje_texto, a.mensaje_correo from forma_pago a where a.estado = 'A' and a.contact = 'S' and a.virtual = 'S'";
			}
			
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idformapago;
			String nombre;
			String tipoformapago;
			String virtual;
			String mensajeTexto;
			String mensajeCorreo;
			while(rs.next()){
				idformapago = rs.getInt("idforma_pago");
				nombre = rs.getString("nombre");
				tipoformapago = rs.getString("tipoformapago");
				virtual = rs.getString("virtual");
				try
				{
					mensajeTexto = rs.getString("mensaje_texto");
				}catch(Exception e)
				{
					mensajeTexto = "";
				}
				try
				{
					mensajeCorreo = rs.getString("mensaje_correo");
				}catch(Exception e)
				{
					mensajeCorreo = "";
				}
				FormaPago forma = new FormaPago( idformapago,nombre, tipoformapago);
				forma.setVirtual(virtual);
				forma.setMensajeTexto(mensajeTexto);
				forma.setMensajeCorreo(mensajeCorreo);
				formaspago.add(forma);
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
		return(formaspago);
		
	}
	
	public static int realizarHomologacionFormaPagoTiendaVirtual(String codTiendaVirtual)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idFormaPago = 0;
		
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idforma_pago from homologacion_forpag_pedidovirtual where formapago_tienda_virtual = '" + codTiendaVirtual + "'"; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idFormaPago = rs.getInt("idforma_pago");
				break;
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
		return(idFormaPago);
	}

}
