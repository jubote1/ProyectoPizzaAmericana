package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.EstadoPedido;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que implementa todos los métodos de acceso a la base de datos para la administración de la entidad Excepcion de Precio.
 * @author JuanDavid
 *
 */
public class ExcepcionPrecioDAO {
	
	/**
	 * Método que se encarga de obtener todas la excepciones de precio parametrizadas en base de datos
	 * @return Retorna un ArrayList con objetos de Modelo Excepción Precio.
	 */
	public static ArrayList<ExcepcionPrecio> obtenerExcepcionesPrecio()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ExcepcionPrecio> excepciones = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from excepcion_precio where habilitado = 'S' order by descripcion asc";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String lunes, martes, miercoles, jueves, viernes, sabado, domingo;
			String controlaEspecialidades;
			String ofertaAbierta;
			String manejaOferta;
			while(rs.next()){
				int idExcepcion = rs.getInt("idexcepcion");
				int idProducto = rs.getInt("idproducto");
				double precio = rs.getDouble("precio");
				String descripcion = rs.getString("descripcion");
				String incluye_liquido  = rs.getString("incluye_liquido");
				int idtipoliquido = rs.getInt("idtipoliquido");
				String controlaCantidadIngredientes = rs.getString("controla_cantidad_ingredientes");
				int cantidadIngredientes = rs.getInt("cantidad_ingredientes");
				String partiradiciones = rs.getString("partiradiciones");
				String horaInicial = rs.getString("hora_inicial");
				String horaFinal = rs.getString("hora_final");
				lunes = rs.getString("lunes");
				martes = rs.getString("martes");
				miercoles = rs.getString("miercoles");
				jueves = rs.getString("jueves");
				viernes = rs.getString("viernes");
				sabado = rs.getString("sabado");
				domingo = rs.getString("domingo");
				controlaEspecialidades = rs.getString("controla_especialidades");
				ofertaAbierta = rs.getString("oferta_abierta");
				manejaOferta = rs.getString("maneja_oferta");
				System.out.println("controlaEspecialidades " + controlaEspecialidades);
				ExcepcionPrecio excepcion = new ExcepcionPrecio(idExcepcion, idProducto, precio, descripcion,controlaCantidadIngredientes,cantidadIngredientes,"",0,partiradiciones,horaInicial, horaFinal,lunes,martes,miercoles,jueves,viernes, sabado, domingo, controlaEspecialidades);
				excepcion.setOfertaAbierta(ofertaAbierta);
				excepcion.setManejaOferta(manejaOferta);
				excepciones.add(excepcion);
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
		return(excepciones);
		
	}
	
	/**
	 * Método que se encarga de todos las excepciones Precio de la base de datos,en el formato que lo requiere el grid
	 * de la capa de Presentación
	 * @return Retorna un ArrayList con objetos de Modelo ExcepcionPrecio.
	 */
	public static ArrayList<ExcepcionPrecio> obtenerExcepcionesPrecioGrid()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<ExcepcionPrecio> excepciones = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select e.idexcepcion, e.idproducto, p.descripcion producto, e.precio, e.descripcion, e.incluye_liquido, e.idtipoliquido, t.nombre nombreliquido, e.habilitado , e.hora_inicial, e.hora_final, e.lunes, e.martes, e.miercoles, e.jueves, e.viernes, e.sabado, e.domingo  "
					+ "from  producto p, excepcion_precio e LEFT JOIN tipo_liquido t ON (e.idtipoliquido = t.idtipo_liquido) where  e.idproducto = p.idproducto";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String lunes, martes, miercoles, jueves, viernes, sabado, domingo;
			while(rs.next()){
				int idExcepcion = rs.getInt("idexcepcion");
				int idProducto = rs.getInt("idproducto");
				String producto = rs.getString("producto");
				double precio = rs.getDouble("precio");
				String descripcion = rs.getString("descripcion");
				String incluye_liquido  = rs.getString("incluye_liquido");
				int idtipoliquido = rs.getInt("idtipoliquido");
				String nombreliquido = rs.getString("nombreliquido");
				String habilitado = rs.getString("habilitado");
				String horaInicial = rs.getString("hora_inicial");
				String horaFinal = rs.getString("hora_final");
				lunes = rs.getString("lunes");
				martes = rs.getString("martes");
				miercoles = rs.getString("miercoles");
				jueves = rs.getString("jueves");
				viernes = rs.getString("viernes");
				sabado = rs.getString("sabado");
				domingo = rs.getString("domingo");
				ExcepcionPrecio excepcion = new ExcepcionPrecio(idExcepcion, idProducto, producto, precio, descripcion, incluye_liquido, idtipoliquido, nombreliquido,habilitado,horaInicial, horaFinal,lunes,martes,miercoles,jueves,viernes, sabado, domingo);
				excepciones.add(excepcion);
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
		return(excepciones);
		
	}
	
	/**
	 * Método que se encarga de realizar la inserción de una ExcepcionPrecio con base en la información recibida como 
	 * parámetro.
	 * @param Exc Recibe como parámetro un objeto de Modelo ExcepcionPrecio con base en el cual se realiza la inserción
	 * de la información.
	 * @return Se retorna un número entero con el idexcepcion retornado en la inserción a la base de datos.
	 */
	public static int insertarExcepcionPrecio(ExcepcionPrecio Exc)
	{
		Logger logger = Logger.getLogger("log_file");
		int idExcepcionPrecioIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		//traemos los campos horas inicial y final para realizar ciertas validaciones de si son nulos o no
		String horaInicial = Exc.getHoraInicial();
		String horaFinal = Exc.getHoraFinal();
		//Validamos si los valores son vacíos
		if(horaInicial == ""){
			horaInicial = null;
		}
		if(horaFinal == ""){
			horaFinal = null;
		}
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into excepcion_precio (idproducto,precio,descripcion,incluye_liquido,idtipoliquido,habilitado,hora_inicial, hora_final,lunes,martes,miercoles,jueves,viernes, sabado, domingo) values (" + Exc.getIdProducto() + "," + Exc.getPrecio() + ", '" + Exc.getDescripcion() + "' , '" + Exc.getIncluyeliquido() + "' , " 
			+ Exc.getIdtipoliquido()  + " , '"+ Exc.getHabilitado() + "' , '" + horaInicial + "' , '" + horaFinal + "' , '" + Exc.getLunes() + "' , '" + Exc.getMartes() + "' , '" + Exc.getMiercoles() + "' ,'" + Exc.getJueves() + "' , '" + Exc.getViernes() + "' , '" + Exc.getSabado() + "' , '" + Exc.getDomingo() +"')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idExcepcionPrecioIns = rs.getInt(1);
				logger.info("idexcepcionprecio insertado es " + idExcepcionPrecioIns);
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
		return(idExcepcionPrecioIns);
	}

	
	/**
	 * Método qeu se encarga de validar la eliminación de la excepción precio, validando si ha sido o no asignada a un producto
	 * determinado
	 * @param idexcepcion Recibe como parámetro el idexcepcion que desea ser eliminado.
	 * Retorna un valor booleando indicando si tiene o no tiene registros
	 */
	public static boolean validarEliminarExcepcionPrecio(int idexcepcion)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		int cantidadUtilizado = 0;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select count(*) from detalle_pedido  where idexcepcion = " + idexcepcion; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				cantidadUtilizado = rs.getInt(1);
				break;
			}
			if (cantidadUtilizado > 0)
			{
				respuesta = true;
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
		return(respuesta);
		
	}
	
	/**
	 * Método qeu se encarga de eliminar una excepción de precio con base en la información enviadad como parámetro.
	 * @param idexcepcion Recibe como parámetro el idexcepcion que desea ser eliminado.
	 */
	public static void eliminarExcepcionPrecio(int idexcepcion)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from excepcion_precio  where idexcepcion = " + idexcepcion; 
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
	 * Método que se encarga de consultar una excepcion Precio con base en el parámetro recibido.
	 * @param idexcepcion Se recibe como parámetro el idexcepcion que desea ser consultado.
	 * @return Se retorna un objeto Modelo ExcepcionPrecio que contiene la información el excepcion Precio consultada.
	 */
	public static ExcepcionPrecio retornarExcepcionPrecio(int idexcepcion)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ExcepcionPrecio Esc = new ExcepcionPrecio(0,0,0,"","",0,"", "", "", "", "", "", "", "", "", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idexcepcion,idproducto,precio,descripcion,incluye_liquido,idtipoliquido,habilitado, hora_inicial, hora_final, lunes, martes, miercoles, jueves, viernes, sabado, domingo from  excepcion_precio  where idexcepcion = " + idexcepcion; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idexc = 0;
			int idpro = 0;
			double precio = 0;
			String desc = "";
			String inc_liq = "";
			int idtipliq = 0;
			String habilitado = "";
			String horaInicial = "";
			String horaFinal = "";
			String lunes = "", martes = "", miercoles = "", jueves = "", viernes = "", sabado = "", domingo = "";
			while(rs.next()){
				idexc = rs.getInt("idexcepcion");
				idpro = rs.getInt("idproducto");
				precio = rs.getDouble("precio");
				desc = rs.getString("descripcion");
				inc_liq = rs.getString("incluye_liquido");
				idtipliq = rs.getInt("idtipoliquido");
				habilitado = rs.getString("habilitado");
				horaInicial = rs.getString("hora_inicial");
				horaFinal = rs.getString("hora_final");
				lunes = rs.getString("lunes");
				martes = rs.getString("martes");
				miercoles = rs.getString("miercoles");
				jueves = rs.getString("jueves");
				viernes = rs.getString("viernes");
				sabado = rs.getString("sabado");
				domingo = rs.getString("domingo");
				break;
			}
			Esc = new ExcepcionPrecio(idexc, idpro,precio, desc,inc_liq,idtipliq,habilitado,horaInicial, horaFinal, lunes, martes, miercoles, jueves, viernes, sabado, domingo);
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
		return(Esc);
	}

	/**
	 * Método que permite editar una excepción Precio con base en la información enviada como parámetro.
	 * @param Esc Recibe como parámetro un objeto Modelo ExcepcionPrecio con base en el cual se realiza la edición.
	 * @return Retorna un string con el resultado del proceso de edición.
	 */
	public static String editarExcepcionPrecio(ExcepcionPrecio Esc)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		//traemos los campos horas inicial y final para realizar ciertas validaciones de si son nulos o no
				String horaInicial = Esc.getHoraInicial();
				String horaFinal = Esc.getHoraFinal();
				//Validamos si los valores son vacíos
				if(horaInicial == ""){
					horaInicial = null;
				}
				if(horaFinal == ""){
					horaFinal = null;
				}
		try
		{
			Statement stm = con1.createStatement();
			String update = "update excepcion_precio set hora_inicial = '" + horaInicial + "' , hora_final = '" + horaFinal + "' , habilitado = '"+ Esc.getHabilitado() +"' , idproducto =" + Esc.getIdProducto() + ", precio = " + Esc.getPrecio() 
			+ ", descripcion = '" + Esc.getDescripcion() + "' , incluye_liquido = '" + Esc.getIncluyeliquido() + "' , idtipoliquido =" + Esc.getIdtipoliquido() + " , lunes ='" + Esc.getLunes() + "' , martes ='" + Esc.getMartes() + "' , miercoles ='" + Esc.getMiercoles() + "' , jueves ='" + Esc.getJueves() + "' , viernes ='" + Esc.getViernes() + "' , sabado ='" + Esc.getSabado() + "' , domingo = '" + Esc.getDomingo() +"'  where idexcepcion = " + Esc.getIdExcepcion(); 
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
	
	
	public static ExcepcionPrecio homologarExcepcionTiendaVirtual(String sku)
	{
		Logger logger = Logger.getLogger("log_file"); 
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ExcepcionPrecio excepcion = new ExcepcionPrecio(0,0,0,"","",0,"", "", "", "", "", "", "", "", "", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from excepcion_precio where idexcepcion = "
					+ "(select idinterno from homologacion_producto_pedidovirtual where tipo = 'EX' and sku = '" + sku + "')";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idexc = 0;
			int idpro = 0;
			double precio = 0;
			String desc = "";
			String inc_liq = "";
			int idtipliq = 0;
			String habilitado = "";
			String horaInicial = "";
			String horaFinal = "";
			String lunes = "", martes = "", miercoles = "", jueves = "", viernes = "", sabado = "", domingo = "";
			String controlaEspecialidades = "";
			while(rs.next()){
				idexc = rs.getInt("idexcepcion");
				idpro = rs.getInt("idproducto");
				precio = rs.getDouble("precio");
				desc = rs.getString("descripcion");
				inc_liq = rs.getString("incluye_liquido");
				idtipliq = rs.getInt("idtipoliquido");
				habilitado = rs.getString("habilitado");
				horaInicial = rs.getString("hora_inicial");
				horaFinal = rs.getString("hora_final");
				lunes = rs.getString("lunes");
				martes = rs.getString("martes");
				miercoles = rs.getString("miercoles");
				jueves = rs.getString("jueves");
				viernes = rs.getString("viernes");
				sabado = rs.getString("sabado");
				domingo = rs.getString("domingo");
				controlaEspecialidades = rs.getString("controla_especialidades");
				break;
			}
			excepcion = new ExcepcionPrecio(idexc, idpro,precio, desc,inc_liq,idtipliq,habilitado,horaInicial, horaFinal, lunes, martes, miercoles, jueves, viernes, sabado, domingo);
			excepcion.setControlaEspecialidades(controlaEspecialidades);
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
		return(excepcion);
	}
	
	
	/**
	 * Método que permitirá validar si un código es correcto para una excepción de precio determinada
	 * @param codigo
	 * @return
	 */
	public static String obtenerExcepcionCerrada(String codigo , int idExcepcion)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String nombre = "";
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from excepcion_cerrada where idexcepcion = " + idExcepcion + " and codigo = '" + codigo + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				nombre = rs.getString("nombre");
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
		return(nombre);
		
	}
	

}
