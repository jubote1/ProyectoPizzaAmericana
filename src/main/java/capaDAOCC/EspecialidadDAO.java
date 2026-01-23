package capaDAOCC;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import capaModeloCC.Cliente;
import capaModeloCC.Especialidad;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que se encarga de implementar todos aquellos m�todos que tienen una interacci�n directa con la base de datos
 * @author JuanDavid
 *
 */
public class EspecialidadDAO {
	
	
/**
 * M�todo que se encarga de insertar en base de datos la informaci�n de la entidad Especialidad
 * @param Espe recibe como par�metro un objeto Modelo Especialidad con base en el cual se realiza la inserci�n de la
 * especialidad.
 * @return Se retonra valor entero con el id de la especiliadad insertada.
 */
public static int insertarEspecialidad(Especialidad Espe)
{
	Logger logger = Logger.getLogger("log_file");
	int idEspecialidadIns = 0;
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	try
	{
		Statement stm = con1.createStatement();
		String insert = "insert into especialidad (nombre, abreviatura) values ('" + Espe.getNombre() + "', '" +Espe.getAbreviatura() + "')"; 
		logger.info(insert);
		stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.getGeneratedKeys();
		if (rs.next()){
			idEspecialidadIns=rs.getInt(1);
			logger.info("id especialidad insertada en bd " + idEspecialidadIns);
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
	return(idEspecialidadIns);
}

/**
 * M�todo que se encarga de eliminar una especialidad en la base de datos.
 * @param idespecialidad Se recibe como par�metro el id especialidad qeu se desea eliminar.
 */
public static void eliminarEspecialidad(int idespecialidad)
{
	Logger logger = Logger.getLogger("log_file");
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	try
	{
		Statement stm = con1.createStatement();
		String delete = "delete from especialidad  where idespecialidad = " + idespecialidad; 
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
 * M�todo que se encarga de retornar una especialidad dado un idespecialidad
 * @param idespecialidad recibe como par�metro un intero id especialidad y con base en esto, realiza la consulta
 * en base de datos y retorna la informaci�n.
 * @return Se retorna la informaci�n de la especialidad en un objeto Modelo Especialidad.
 */
public static Especialidad retornarEspecialidad(int idespecialidad)
{
	Logger logger = Logger.getLogger("log_file");
	int idEspecialidadEli = 0;
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	Especialidad Espe = new Especialidad(0,"", "","");
	try
	{
		Statement stm = con1.createStatement();
		String consulta = "select idespecialidad,nombre, abreviatura,estado from  especialidad  where idespecialidad = " + idespecialidad; 
		logger.info(consulta);
		ResultSet rs = stm.executeQuery(consulta);
		int idesp = 0;
		String nombr = "";
		String abre = "";
		String estado = "";
		while(rs.next()){
			idesp = rs.getInt("idespecialidad");
			nombr = rs.getString("nombre");
			abre = rs.getString("abreviatura");
			estado = rs.getString("estado");
			break;
		}
		Espe = new Especialidad(idesp, nombr, abre,estado);
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
	return(Espe);
}

/**
 * M�todo que tiene como objetivo modificar una especialidad.
 * @param Espe Recibe como par�metro un objeto Modelo Especiliadad con base en la cual se har� la modificaci�n.
 * @return Se retorna un string indicadno si el proceso fue exitoso o no.
 */
public static String editarEspecialidad(Especialidad Espe)
{
	Logger logger = Logger.getLogger("log_file");
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	String resultado = "";
	try
	{
		Statement stm = con1.createStatement();
		String update = "update especialidad set nombre ='" + Espe.getNombre() + "', abreviatura =  '" +Espe.getAbreviatura() + "' where idespecialidad = " + Espe.getIdespecialidad(); 
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
 * M�todo que se encarga de retornar el precio en base de datos de un producto y especialidad determinada
 * que tengan una excepci�n de precio.
 * @param idespecialidad El idespecialidad que esta asociado a la excepci�n especialidad
 * @param idproducto El idproducto asociado a la excepci�n de especialidad.
 * @return Se retornar� un valor double con el precio asociado a los par�metros de idespecialidad e idproducto enviados.
 */
public static double obtenerPrecioExcepcionEspecialidad(int idespecialidad, int idproducto)
{
	Logger logger = Logger.getLogger("log_file");
	int idEspecialidadEli = 0;
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	double precio= 0;
	try
	{
		Statement stm = con1.createStatement();
		String consulta = "select precio from  especialidad_excepcion  where idespecialidad = " + idespecialidad + " and idproducto = " + idproducto; 
		logger.info(consulta);
		ResultSet rs = stm.executeQuery(consulta);
		while(rs.next()){
			precio = rs.getDouble("precio");
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
	return(precio);
}

/**
 * M�todo que encarga de la homologaci�n del c�digo de especialidad con base en el SKU descompuesto.
 * @param sku
 * @return
 */
public static int homologarEspecialidadTiendaVirtual(String sku)
{
	Logger logger = Logger.getLogger("log_file");
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	int idInterno = 0;
	try
	{
		Statement stm = con1.createStatement();
		String consulta = "select idinterno from homologacion_producto_pedidovirtual where tipo = 'E' and sku = '" + sku + "'";
		logger.info(consulta);
		ResultSet rs = stm.executeQuery(consulta);
		while(rs.next()){
			idInterno = rs.getInt("idinterno");
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
	return(idInterno);
}

/**
 * M�todo que se encarga de retornar un booleano indicando si la promoci�n existe y corresponde a una promoci�n.
 * @param sku
 * @return
 */
public static boolean esPromocionTiendaVirtual(String sku)
{
	Logger logger = Logger.getLogger("log_file");
	ConexionBaseDatos con = new ConexionBaseDatos();
	Connection con1 = con.obtenerConexionBDPrincipal();
	boolean respuesta = false;
	try
	{
		Statement stm = con1.createStatement();
		String consulta = "select idinterno from homologacion_producto_pedidovirtual where tipo = 'PR' and sku = '" + sku + "'";
		logger.info(consulta);
		ResultSet rs = stm.executeQuery(consulta);
		while(rs.next()){
			respuesta = true;
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
	return(respuesta);
}


}
