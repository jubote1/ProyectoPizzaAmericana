package capaDAOCC;

import java.sql.Connection;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.SaborLiquido;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que implementa toda la interacción con la base de datos para la entidad Sabor Tipo Liquido.
 * @author JuanDavid
 *
 */
public class SaborTipoLiquidoDAO {
	
	/**
	 * Método que se encarga de retornar todos los sabores tipo liquido definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los sabores Tipo Liquido definidos en el sistema.
	 */
	public static ArrayList<SaborLiquido> obtenerSaborLiquidos()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SaborLiquido> saborliquidos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from sabor_x_tipo_liquido";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idsaborxtipoliquido = rs.getInt("idsabor_x_tipo_liquido");
				String descripcion = rs.getString("descripcion");
				int idtipoliquido = rs.getInt("idtipo_liquido");
				SaborLiquido saborliquido = new SaborLiquido(idsaborxtipoliquido,descripcion,idtipoliquido);
				saborliquidos.add(saborliquido);
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
		return(saborliquidos);
		
	}
	
	/**
	 * Método que se encarga de retornar todos los sabores tipo liquido definidos en el sistema, en el formato básico
	 * para el GRID que implementa el CRUD de la entidad en la capa de presentación.
	 * @return Se retorna un ArrayList con todas las entidades sabor tipo liquido definidas en el sistema.
	 */
	public static ArrayList<SaborLiquido> obtenerSaborLiquidosGrid()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SaborLiquido> saborliquidos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idsabor_x_tipo_liquido, a.descripcion, a.idtipo_liquido, b.nombre   from  sabor_x_tipo_liquido a, tipo_liquido b where a.idtipo_liquido = b.idtipo_liquido";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idsaborxtipoliquido = rs.getInt("idsabor_x_tipo_liquido");
				String descripcion = rs.getString("descripcion");
				int idtipoliquido = rs.getInt("idtipo_liquido");
				String nombreliquido = rs.getString("nombre");
				SaborLiquido saborliquido = new SaborLiquido(idsaborxtipoliquido,descripcion,idtipoliquido,nombreliquido);
				saborliquidos.add(saborliquido);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(saborliquidos);
		
	}
	
	/**
	 * Método que se encarga de insertar un sabor tipo liquido de acuerdo a los valores ingresados como parámetro
	 * @param pro Se recibe como parámetro un objeto de Modelo Sabor Tipo Liquido con base en el cual se realiza
	 * la inserción de la entidad.
	 * @return Se retorna un valor entero con el id sabor tipo liquido creado en la base de datos.
	 */
	public static int insertarSaborTipoLiquido(SaborLiquido pro)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSaborLiquidoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into sabor_x_tipo_liquido (idsabor_x_tipo_liquido,descripcion,idtipo_liquido) values (" + pro.getIdSaborTipoLiquido() + ", '" + pro.getDescripcionSabor() + "' , "  + pro.getIdLiquido()  +  ")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idSaborLiquidoIns = rs.getInt(1);
				
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
		return(idSaborLiquidoIns);
	}

	/**
	 * Método que se encarga de la eliminación de un sabor tipo líquido de la base de datos, con base en la información recibida como parámetro.
	 * @param idsabortipoliquido Se recibe como parámetro el idsabortipoliquido, con base en el cual se realiza la eliminación.
	 */
	public static void eliminarSaborTipoLiquido(int idsabortipoliquido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from sabor_x_tipo_liquido  where idsabor_x_tipo_liquido = " + idsabortipoliquido; 
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
	 * Método mediante el cual se realiza la consulta de un sabor tipo liquido con base el parámetro recibido.
	 * @param idsabortipoliquido Se recibe como parámetro valor entero que indica el sabor tipo liquido que se requiere 
	 * consultar.
	 * @return Se retorna objeto de Modelo SaborTipoLiquido en el cual se retorna la entidad sabor tipo liquido consultada.
	 */
	public static SaborLiquido retornarSaborTipoLiquido(int idsabortipoliquido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		SaborLiquido Pro = new SaborLiquido(0,"",0);
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idsabor_x_tipo_liquido,descripcion,idtipo_liquido from  sabor_x_tipo_liquido  where idsabor_x_tipo_liquido = " + idsabortipoliquido; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idsabor = 0;
			String descripcion = "";
			int idtipo_liquido = 0;
			while(rs.next()){
				idsabor = rs.getInt("idsabor_x_tipo_liquido");
				descripcion = rs.getString("descripcion");
				idtipo_liquido = rs.getInt("idtipo_liquido");
				break;
			}
			Pro = new SaborLiquido(idsabor,descripcion,idtipo_liquido);
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
		return(Pro);
	}

	/**
	 * Método que permite la edición de un sabor tipo liquido con base en la información enviada como parámetro.
	 * @param Pro Se recibe como párametro un objeto Modelo SaborTipoLiquido que contiene la información básica
	 * para la modificación.
	 * @return Se retorna un valor String que indica el resultado del proceso.
	 */
	public static String editarSaborTipoLiquido(SaborLiquido Pro)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update sabor_x_tipo_liquido set  descripcion = '" + Pro.getDescripcionSabor() + "', idtipo_liquido =" + Pro.getIdLiquido() +"  where idsabor_x_tipo_liquido = " + Pro.getIdSaborTipoLiquido(); 
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
	 * Método que se encarga de homologar el tipo de liquido incluido en el producto, con base en lo enviado dentro del JSON.
	 * @param sku
	 * @return
	 */
	public static int homologarLiquidoTiendaVirtual(String sku)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idInterno = 0;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idinterno from homologacion_producto_pedidovirtual where tipo = 'T' and sku = '" + sku + "'";
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

}
