package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.Parametro;
import conexionCC.ConexionBaseDatos;
import interfazGraficaPOS.Sesion;


public class ParametrosDAO {
	
	public static int retornarValorNumerico(String variable)
	{
		int valor = 0;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select valornumerico from parametros where valorparametro = '"+ variable +"'";
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				
				try
				{
					valor = Integer.parseInt(rs.getString("valornumerico"));
				}catch(Exception e)
				{
				
					logger.error(e.toString());
					valor = 0;
				}
				
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e)
		{
			
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				
			}
		}
		return(valor);
	}
	
	public static String retornarValorAlfanumerico(String variable)
	{
		String valor = "";
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select valortexto from parametros where valorparametro = '"+ variable +"'";
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				
				try
				{
					valor = rs.getString("valortexto");
				}catch(Exception e)
				{
				
					logger.error(e.toString());
					valor = "";
				}
				
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e)
		{
			
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				
			}
		}
		return(valor);
	}
	
	/**
	 * Método que se encarga de obtener un arrayList con los parámetros ingresados en el sistema. 
	 * @return Se retorna ArrayList genérico con todos los parámetros del sistema.
	 */
	public static ArrayList obtenerParametros()
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		ArrayList parametros = new ArrayList();
		
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from parametros";
			ResultSet rs = stm.executeQuery(consulta);
			ResultSetMetaData rsMd = (ResultSetMetaData) rs.getMetaData();
			int numeroColumnas = rsMd.getColumnCount();
			
			while(rs.next()){
				String [] fila = new String[numeroColumnas];
				for(int y = 0; y < numeroColumnas; y++)
				{
					fila[y] = rs.getString(y+1);
				}
				parametros.add(fila);
				
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
		return(parametros);
		
	}
	
	/**
	 * Metodo que se encarga de retornar parametro de acuerdo al valorParametro recibido.
	 * @param valorParametro de tipo texto como parametro para la consulta
	 * @return Se retorna un objeto de tipo Parametro
	 */
	public static Parametro obtenerParametro(String valorParametro)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		Parametro parametro = new Parametro("", 0, "");
		
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from parametros where valorparametro = '" + valorParametro +"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int valorNumerico = 0;
			String valorTexto = "";
			while(rs.next()){
				
				valorNumerico = rs.getInt("valornumericod");
				valorTexto = rs.getString("valortexto");
				parametro = new Parametro(valorParametro, valorNumerico, valorTexto);
				break;
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
		return(parametro);
		
	}
	
	/**
	 * Método de la capa DAO que se encarga de insertar una entidad parametro
	 * @param Parametro como objeto tipo parametro del cual se extrae la información para la inserción.
	 * @return Se retorna un valor booleano con la respuesta de si fue o no creado el objeto parámetro en la base de datos.
	 */
	public static boolean insertarParametro(Parametro parametro)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		boolean respuesta = true; 
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into parametros (valorparametro, valornumerico, valortexto) values ('" + parametro.getValorParametro() + "', " + parametro.getValorNumerico() + ", '" + parametro.getValorTexto() + "')"; 
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
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
			respuesta = false; 
		}
		return(respuesta);
	}
	
	/**
	 * Método que se encarga de eliminar un determinado parametro, teniendo en cuenta el valorparmaetro pasado como parámetro
	 * @param valorparametro Se recibe como parámetro el valorParametro que se desea eliminar, teniendo en cuenta que es la 
	 * clave primaría de la tabla.
	 * @return Se retorna un valor booleano que indica si el resultado del proceso fue satisfactorio o no.
	 */
	public static boolean eliminarParametro(String valorParametro)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = true;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from parametros where valorparametro = '" + valorParametro+"'"; 
			stm.executeUpdate(delete);
			respuesta = true;
			stm.close();
			con1.close();
		}
		catch (Exception e){
			logger.error(e.toString());
			try
			{
				con1.close();
				respuesta = false;
			}catch(Exception e1)
			{
			}
			
		}
		return(respuesta);
	}
	
	/**
	 * Método de la capa DAO que se encarga de editar un impuesto ya existente.
	 * @param impuesto Recibe como parámetro un objeto de la entidad impuesto con base en el cual se realiza la modificación
	 * @return Se retorna un valor booleano indicando si el proceso fue o no satisfactorio
	 */
	public static boolean EditarParametro(Parametro parametro)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update parametros set valortexto = '" + parametro.getValorTexto() + "' , valornumerico = " + parametro.getValorNumerico() + " where valorparametro = '" + parametro.getValorParametro()+"'" ; 
			stm.executeUpdate(update);
			//Ejecutamos la inserción de log
			String insercionLog = "insert into parametros_log (usuario, nuevovalor,variable) values ('"  + Sesion.getUsuario() + "' , '" + parametro.getValorNumerico()+parametro.getValorTexto() +"' ,'" + parametro.getValorParametro() + "')" ;
			stm.executeUpdate(insercionLog);
			
			
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
			return(false);
		}
		return(true);
	}
	
	
}
