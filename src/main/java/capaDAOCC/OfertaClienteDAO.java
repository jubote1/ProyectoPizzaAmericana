package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteZapier;
import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.MensajeTexto;
import capaModeloCC.Oferta;
import capaModeloCC.OfertaCliente;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que implementa todos los métodos de acceso a la base de datos para la administración de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class OfertaClienteDAO {
	
	/**
	 * Método que se encarga de obtener todas la Ofertas de un cliente parametrizadas en base de datos
	 * @return Retorna un ArrayList con objetos de Modelo Oferta.
	 */
	public static ArrayList<OfertaCliente> obtenerOfertasClienteGrid(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<OfertaCliente> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_oferta from oferta_cliente a, oferta b where a.idoferta = b.idoferta and a.idcliente = " + idCliente;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOfertaCliente;
			int idOferta;
			String utilizada;
			String ingresoOferta;
			String usoOferta;
			String nombreOferta = "";
			String observacion = "";
			String usuarioIngreso = "";
			int PQRS = 0;
			OfertaCliente ofertaTemp = new OfertaCliente(0,0,0,"", 0,"","", "", "");
			while(rs.next()){
				idOfertaCliente = rs.getInt("idofertacliente");
				idOferta = rs.getInt("idoferta");
				utilizada = rs.getString("utilizada");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				nombreOferta = rs.getString("nombre_oferta");
				observacion = rs.getString("observacion");
				PQRS = rs.getInt("PQRS");
				usuarioIngreso = rs.getString("usuario_ingreso");
				ofertaTemp = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, PQRS,ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaTemp.setNombreOferta(nombreOferta);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
	/**
	 * Método que contiene las ofertas que TIENE VIGENTE UN CLIENTE DETERMINADO
	 * @param idCliente
	 * @return
	 */
	public static ArrayList<OfertaCliente> obtenerOfertasVigenteCliente(int idCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<OfertaCliente> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_oferta from oferta_cliente a, oferta b where a.idoferta = b.idoferta and utilizada = 'N' and idcliente = " + idCliente;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOfertaCliente;
			int idOferta;
			String utilizada;
			String ingresoOferta;
			String usoOferta;
			String nombreOferta = "";
			String observacion = "";
			String usuarioIngreso = "";
			int PQRS = 0;
			OfertaCliente ofertaTemp = new OfertaCliente(0,0,0,"", PQRS,"","", observacion, "");
			while(rs.next()){
				idOfertaCliente = rs.getInt("idofertacliente");
				idOferta = rs.getInt("idoferta");
				utilizada = rs.getString("utilizada");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				nombreOferta = rs.getString("nombre_oferta");
				observacion = rs.getString("observacion");
				PQRS = rs.getInt("PQRS");
				usuarioIngreso = rs.getString("usuario_ingreso");
				ofertaTemp = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, PQRS, ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaTemp.setNombreOferta(nombreOferta);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
		
	/**
	 * Método que se encarga de realizar la inserción de una ferta Cliente con base en la información recibida como 
	 * parámetro.
	 * @param Exc Recibe como parámetro un objeto de Modelo EOfertaCliente con base en el cual se realiza la inserción
	 * de la información.
	 * @return Se retorna un número entero con el idofertacliente retornado en la inserción a la base de datos.
	 */
	public static int insertarOfertaCliente(OfertaCliente ofer)
	{
		Logger logger = Logger.getLogger("log_file");
		int idOfertaClienteIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String fechaCaducidad = "";
		fechaCaducidad = ofer.getFechaCaducidad();
		if(fechaCaducidad == null)
		{
			fechaCaducidad = "";
		}
		try
		{ 
			Statement stm = con1.createStatement();
			String insert = "";
			if(fechaCaducidad.equals(new String("")))
			{
				insert = "insert into oferta_cliente (idoferta,idcliente, observacion, PQRS, codigo_promocion, usuario_ingreso, saldo) values (" + ofer.getIdOferta() + " ," + ofer.getIdCliente() + " , '" + ofer.getObservacion() + "' ," + ofer.getPQRS() + " , '" + ofer.getCodigoPromocion() + "' , '" + ofer.getUsuarioIngreso() + "' , " + ofer.getSaldo()  +")"; 
				logger.info(insert);
			}else
			{
				insert = "insert into oferta_cliente (idoferta,idcliente, observacion, PQRS, codigo_promocion, usuario_ingreso, fecha_caducidad, saldo) values (" + ofer.getIdOferta() + " ," + ofer.getIdCliente() + " , '" + ofer.getObservacion() + "' ," + ofer.getPQRS() + " , '" + ofer.getCodigoPromocion() + "' , '" + ofer.getUsuarioIngreso() + "' , '" + fechaCaducidad + "' , " + ofer.getSaldo()  +")"; 
				logger.info(insert);
			}
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idOfertaClienteIns = rs.getInt(1);
				logger.info("idOfertaCliente insertado es " + idOfertaClienteIns);
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
			return(0);
		}
		return(idOfertaClienteIns);
	}

	/**
	 * Método qeu se encarga de eliminar una oferta CLiente con base en la información enviadad como parámetro.
	 * @param idofertaCliente Recibe como parámetro el idexcepcion que desea ser eliminado.
	 */
	public static void eliminarOfertaCliente(int idOfertaCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from oferta_cliente  where idofertacliente = " + idOfertaCliente; 
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
	 * Método que se encarga de consultar una ofertaCliente con base en el parámetro recibido.
	 * @param idOferta Se recibe como parámetro el idexcepcion que desea ser consultado.
	 * @return Se retorna un objeto Modelo Oferta que contiene la información el excepcion Precio consultada.
	 */
	public static OfertaCliente retornarOfertaCliente(int idOfertaCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		OfertaCliente ofertaTemp = new OfertaCliente(0,0,0,"",0,"","","", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_oferta, c.idcliente, c.nombre nombre_cliente, b.red_parcial from  oferta_cliente a, oferta b, cliente c  where a.idoferta = b.idoferta and a.idcliente = c.idcliente and a.idofertacliente = " + idOfertaCliente; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idCliente;
			int idOferta;
			String nombreOferta;
			String nombreCliente;
			String codigoPromocion;
			String utilizada;
			String ingresoOferta;
			String usoOferta;
			String observacion;
			String usuarioIngreso;
			double saldo;
			String redParcial = "N";
			int PQRS = 0;
			while(rs.next()){
				idCliente = rs.getInt("idcliente");
				idOferta = rs.getInt("idoferta");
				utilizada = rs.getString("utilizada");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				observacion = rs.getString("observacion");
				PQRS = rs.getInt("PQRS");
				usuarioIngreso = rs.getString("usuario_ingreso");
				nombreOferta = rs.getString("nombre_oferta");
				nombreCliente = rs.getString("nombre_cliente");
				codigoPromocion = rs.getString("codigo_promocion");
				saldo = rs.getDouble("saldo");
				redParcial = rs.getString("red_parcial");
				ofertaTemp = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, PQRS,ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaTemp.setNombreOferta(nombreOferta);
				ofertaTemp.setNombreCliente(nombreCliente);
				ofertaTemp.setCodigoPromocion(codigoPromocion);
				ofertaTemp.setSaldo(saldo);
				ofertaTemp.setRedParcial(redParcial);
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
		return(ofertaTemp);
	}

	/**
	 * Método que permite editar una excepción Precio con base en la información enviada como parámetro.
	 * @param Esc Recibe como parámetro un objeto Modelo ExcepcionPrecio con base en el cual se realiza la edición.
	 * @return Retorna un string con el resultado del proceso de edición.
	 */
	public static String actualizarUsoOferta(int idOfertaCliente, String usuarioUso, double descuentoSobrante)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm"+":00");
		Date fechaActual = new Date();
		String fechaActualizar = formatoFecha.format(fechaActual);
		try
		{
			String update;
			Statement stm = con1.createStatement();
			if(descuentoSobrante == 0)
			{
				update = "update oferta_cliente set utilizada = 'S' , saldo = 0 , uso_oferta = '"+ fechaActualizar +"' , usuario_uso = '"+ usuarioUso +"' where idofertacliente = " + idOfertaCliente; 
			}else
			{
				update = "update oferta_cliente set saldo = " + descuentoSobrante + " , uso_oferta = '"+ fechaActualizar +"' , usuario_uso = '"+ usuarioUso +"' where idofertacliente = " + idOfertaCliente; 
			}
			
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
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
			resultado = "error";
		}
		return(resultado);
	}
	
	
	/**
	 * Método que se encargará de retornar las ofertas ingresadas en la semana de rango enviada como parámetros y recupera
	 * la información como un arrayList de ofertascliente.
	 * @param fechaSuperior
	 * @param fechaInferior
	 * @return
	 */
	public static ArrayList<OfertaCliente> obtenerOfertasNuevasSemana(String fechaSuperior, String fechaInferior)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<OfertaCliente> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_oferta from oferta_cliente a, oferta b where a.idoferta = b.idoferta and a.ingreso_oferta >=  '" + fechaInferior + "'  and a.ingreso_oferta <= '" + fechaSuperior + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOfertaCliente;
			int idOferta;
			int idCliente = 0;
			String utilizada;
			String ingresoOferta;
			String usoOferta;
			String nombreOferta = "";
			String observacion = "";
			int PQRS = 0;
			String usuarioIngreso = "";
			OfertaCliente ofertaTemp = new OfertaCliente(0,0,0,"", 0,"","", "", "");
			while(rs.next()){
				idOfertaCliente = rs.getInt("idofertacliente");
				idOferta = rs.getInt("idoferta");
				utilizada = rs.getString("utilizada");
				idCliente = rs.getInt("idcliente");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				nombreOferta = rs.getString("nombre_oferta");
				observacion = rs.getString("observacion");
				PQRS = rs.getInt(PQRS);
				usuarioIngreso = rs.getString("usuario_ingreso");
				ofertaTemp = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, PQRS,ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaTemp.setNombreOferta(nombreOferta);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
	/**
	 * Método que retornará un ArrayList con objetos de tipo oferta Cliente, con todas las ofertas redimidas dentro del  rango de fechas 
	 * enviadas como parámetro.
	 * @param fechaSuperior
	 * @param fechaInferior
	 * @return
	 */
	public static ArrayList<OfertaCliente> obtenerOfertasRedimidasSemana(String fechaSuperior, String fechaInferior)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<OfertaCliente> ofertas = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_oferta from oferta_cliente a, oferta b where a.idoferta = b.idoferta and a.uso_oferta >=  '" + fechaInferior + "'  and a.uso_oferta <= '" + fechaSuperior + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idOfertaCliente;
			int idOferta;
			int idCliente = 0;
			String utilizada;
			String ingresoOferta;
			String usoOferta;
			String nombreOferta = "";
			String observacion = "";
			int PQRS = 0;
			String usuarioIngreso= "";
			OfertaCliente ofertaTemp = new OfertaCliente(0,0,0,"", 0,"","", "", "");
			while(rs.next()){
				idOfertaCliente = rs.getInt("idofertacliente");
				idOferta = rs.getInt("idoferta");
				utilizada = rs.getString("utilizada");
				idCliente = rs.getInt("idcliente");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				nombreOferta = rs.getString("nombre_oferta");
				observacion = rs.getString("observacion");
				PQRS = rs.getInt(PQRS);
				usuarioIngreso = rs.getString("usuario_ingreso");
				ofertaTemp = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, PQRS,ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaTemp.setNombreOferta(nombreOferta);
				ofertas.add(ofertaTemp);
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
		return(ofertas);
		
	}
	
	
	/**
	 * Método que se encarga de validar si un código promocional determinado existe, retorna un valor booleano con la validación.
	 * @param codigoPromocional
	 * @return
	 */
	public static boolean validarExistenciaOfertaCliente(String codigoPromocional)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean resultado = false;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from oferta_cliente  where codigo_promocion = '" + codigoPromocional + "'"; 
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
	
	
	/**
	 * Método que basado en un codigo promocional pasado como parámetro retorna la oferta asociado al mismo indicando si está existe o no y los datos correspondientes.
	 * @param codigoPromocional
	 * @return
	 */
	public static OfertaCliente retornarOfertaCodigoPromocional(String codigoPromocional)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		OfertaCliente ofertaCliente  = new OfertaCliente(0, 0, 0, "", 0 , "",
				"", "", "");
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from oferta_cliente  where codigo_promocion = '" + codigoPromocional + "'"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			int idOfertaCliente;
			int idOferta;
			int idCliente;
			String utilizada;
			int pqrs;
			String ingresoOferta;
			String usoOferta;
			String observacion;
			String usuarioIngreso = "";
			String fechaCaducidad = "";
			double saldo = 0;
			while(rs.next())
			{
				idOfertaCliente = rs.getInt("idofertacliente");
				idOferta = rs.getInt("idoferta");
				idCliente = rs.getInt("idcliente");
				utilizada = rs.getString("utilizada");
				pqrs = rs.getInt("pqrs");
				ingresoOferta = rs.getString("ingreso_oferta");
				usoOferta = rs.getString("uso_oferta");
				saldo = rs.getDouble("saldo");
				if(usoOferta == null)
				{
					usoOferta = "";
				}
				observacion = rs.getString("observacion");
				usuarioIngreso = rs.getString("usuario_ingreso");
				fechaCaducidad = rs.getString("fecha_caducidad");
				if(fechaCaducidad == null)
				{
					fechaCaducidad = "";
				}
				ofertaCliente = new OfertaCliente(idOfertaCliente, idOferta, idCliente, utilizada, pqrs, ingresoOferta, usoOferta, observacion, usuarioIngreso);
				ofertaCliente.setCodigoPromocion(codigoPromocional);
				ofertaCliente.setFechaCaducidad(fechaCaducidad);
				ofertaCliente.setSaldo(saldo);
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
		return(ofertaCliente);
	}

	/**
	 * Método que se encarga de retornar en un arrayList los mensajes de texto que se deberían de enviar según la oferta 
	 * seleccionada
	 * @param idOferta
	 * @return
	 */
	public static ArrayList<MensajeTexto> obtenerMensajesTextoEnviar(int idOferta)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<MensajeTexto> mensajesTexto = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT a.idofertacliente, c.nombre, c.apellido, c.telefono_celular, c.telefono, b.mensaje1, b.mensaje2, a.codigo_promocion, c.email FROM oferta_cliente a, oferta b, cliente c WHERE a.idcliente = c.idcliente AND a.idoferta = b.idoferta AND b.codigo_promocional = 'S' AND a.fecha_mensaje IS null and a.idoferta = " + idOferta;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			 int idOfertaCliente;
			 String nombreCliente;
			 String apellidoCliente;
			 String numeroCelular;
			 String telefono;
			 String mensaje1;
			 String mensaje2;
			 String email;
			 String codigoPromocion;
			MensajeTexto mensaje = new MensajeTexto (0,0,"", "", "", "","", "", "");
			while(rs.next()){
				idOfertaCliente = rs.getInt("idofertacliente");
				nombreCliente = rs.getString("nombre");
				apellidoCliente = rs.getString("apellido");
				numeroCelular = rs.getString("telefono_celular");
				email = rs.getString("email");
				if(numeroCelular == null)
				{
					numeroCelular = "";
				}
				telefono = rs.getString("telefono");
				mensaje1 = rs.getString("mensaje1");
				if(mensaje1 == null)
				{
					mensaje1 = "";
				}
				mensaje2 = rs.getString("mensaje2");
				if(mensaje2 == null)
				{
					mensaje2 = "";
				}
				codigoPromocion = rs.getString("codigo_promocion");
				if(codigoPromocion == null)
				{
					codigoPromocion = "";
				}
				mensaje = new MensajeTexto(idOferta, idOfertaCliente, nombreCliente, apellidoCliente, numeroCelular, telefono, mensaje1, mensaje2, codigoPromocion);
				mensaje.setEmail(email);
				mensajesTexto.add(mensaje);
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
		return(mensajesTexto);
		
	}
	
	/**
	 * Método que se encarga de marcar el campo de mensaje de texto para una oferta determinada, esta marcación se hace
	 * cuando un mensaje de texto fue correctamente enviado.
	 * @param idOfertaCliente
	 * @return
	 */
	public static String actualizarMensajeOferta(int idOfertaCliente)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm"+":00");
		Date fechaActual = new Date();
		String fechaMensaje = formatoFecha.format(fechaActual);
		try
		{
			Statement stm = con1.createStatement();
			String update = "update oferta_cliente set fecha_mensaje = '"+ fechaMensaje +"' where idofertacliente = " + idOfertaCliente; 
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
	
	
	public static ArrayList<ClienteZapier> obtenerClientesNotificacionZapier(int idOferta, String fechaActual)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ClienteZapier> clientesZapier = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT a.codigo_promocion AS codigo, b.nombre & ' ' & b.apellido AS nombre, b.telefono AS telefono FROM oferta_cliente a, cliente b WHERE a.idcliente = b.idcliente and idoferta = " + idOferta + " AND fecha_caducidad > '" + fechaActual +"' AND utilizada = 'N' AND TIMESTAMPDIFF(DAY, '" + fechaActual + "', fecha_caducidad) <= 3;";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String telefono;
			String nombre;
			String codigo;
			int PQRS = 0;
			ClienteZapier clienteTemp = new ClienteZapier("","", "");
			while(rs.next()){
				telefono = rs.getString("telefono");
				nombre = rs.getString("nombre");
				codigo = rs.getString("codigo");
				clienteTemp = new ClienteZapier(telefono, nombre, codigo);
				clientesZapier.add(clienteTemp);
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
		return(clientesZapier);
		
	}
	
}



