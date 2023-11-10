package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.TipoLiquido;
import conexionCC.ConexionBaseDatos;	

/**
 * Clase que se encarga de implementar toda la interacción con la base de datos para la entidad tipoLiquido
 * @author JuanDavid
 *
 */
public class TipoLiquidoDAO {
	
	/**
	 * Método que se encarga de retornar todos las entidades tipo liquido definidos en la base de datos.
	 * @return Se retorna un ArrayList con todos los tipo Liquido definidos en el sistema.
	 */
	public static ArrayList<TipoLiquido> obtenerTiposLiquido()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<TipoLiquido> tipos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from tipo_liquido";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idtipoliquido = rs.getInt("idtipo_liquido");
				String nombre = rs.getString("nombre");
				String capacidad = rs.getString("capacidad");
				TipoLiquido liquido = new TipoLiquido(idtipoliquido,nombre,capacidad);
				tipos.add(liquido);
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
		return(tipos);
		
	}
	
	/**	
	 * Método que se encarga de insertar un tipo liquido con base en la información recibida como parámetro.
	 * @param pro Se recibe parámetro objeto Modelo TipoLiquido con base en el cual se realizará la inserción de la
	 * nueva entidad en la base de datos.
	 * @return Se retorna un valor entero con el idtipoliquido asignado al objeto nuevo insertado en la base de datos.
	 */
	public static int insertarTipoLiquido(TipoLiquido pro)
	{
		Logger logger = Logger.getLogger("log_file");
		int idTipoLiquidoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into tipo_liquido (nombre,capacidad) values ( '" + pro.getNombre() + "' , '" + pro.getCapacidad() + "' )"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idTipoLiquidoIns = rs.getInt(1);
				
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
		return(idTipoLiquidoIns);
	}

	/**
	 * Método que se encarga de eliminar un tipo Liquido con base en la información enviada como parámetro
	 * @param idtipoliquido Se recibe como parámetro un entero con la información del tipo liquido que se desea
	 * eliminar, el método no tiene ningún retorno.
	 */
	public static void eliminarTipoLiquido(int idtipoliquido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from tipo_liquido  where idtipo_liquido = " + idtipoliquido; 
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
	 * Método que se encarga de retornar una entidad TipoLIquido con base en la información enviada como parámetro.
	 * @param idtipoliquido Se recibe como parámetro un entero idtipoliquido, el cual indicia el id del tipo liquido que se
	 * se desea consultar.
	 * @return Se retorna un objeto Modelo TipoLiquido con la información del tipo liquido que se desea consultar.
	 */
	public static TipoLiquido retornarTipoProducto(int idtipoliquido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		TipoLiquido Pro = new TipoLiquido(0, "","");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idtipo_liquido,nombre,capacidad from  tipo_liquido  where idtipo_liquido = " + idtipoliquido; 
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idtipliq = 0;
			String nombre ="";
			String capacidad = "";
			while(rs.next()){
				idtipliq = rs.getInt("idtipo_liquido");
				nombre = rs.getString("nombre");
				capacidad = rs.getString("capacidad");
				break;
			}
			Pro = new TipoLiquido(idtipliq,nombre,capacidad);
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
		return(Pro);
	}

	/**
	 * Método que se encarga de editar un tipo liquido con base en la información envíada como parámetro.
	 * @param Pro Se recibe como parámetro un objeto Modelo TipoLiquido el cual contiene la base de la información
	 * a modificar.
	 * @return Se retorna una variable String con el resultado del proceso de actualización.
	 */
	public static String editarTipoLiquido(TipoLiquido Pro)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update tipo_liquido set nombre = '" + Pro.getNombre() + "', capacidad = '" + Pro.getCapacidad() + "' where idtipo_liquido = " + Pro.getIdtipo_liquido(); 
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


}
