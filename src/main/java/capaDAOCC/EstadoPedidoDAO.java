package capaDAOCC;
import org.apache.log4j.Logger;

import capaModeloCC.Especialidad;
import capaModeloCC.EstadoPedido;
import conexionCC.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.sql.ResultSet;
/**
 * Clase que se encarga de hacer la comunicaic�n con la base de datos, en todo lo relacionado con la entidad Estado Pedio
 * @author JuanDavid
 *
 */
public class EstadoPedidoDAO {
	
	/**
	 * M�todo que se encarga de la inserci�n de la entidad Estado Pedido.
	 * @param Est recibe como par�metro un objeto Modelo Estado Pedido con base en el cual realiza la inserci�n en base de datos.
	 * @return Retorna un Entero con el idestadopedido creado por el m�todo.
	 */
	public static int insertarEstadoPedido(EstadoPedido Est)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEstadoPedidoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into estado_pedido (descripcion) values ('" + Est.getDescripcion() +  "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEstadoPedidoIns = rs.getInt(1);
				logger.info("id estado pedido insertado " + idEstadoPedidoIns);
				
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
		return(idEstadoPedidoIns);
	}

	/**
	 * M�todo que se encarga de la eliminaci�n de un estado pedido
	 * @param idestadopedido Se recibe como par�metro el idestadopedido que se sea eliminar.
	 */
	public static void eliminarEstadoPedido(int idestadopedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String delete = "delete from estado_pedido  where idestadopedido = " + idestadopedido; 
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
	 * M�todo que se encarga de consultar un estado pedido con base en el par�metro recibido.
	 * @param idestadopedido Se recibe un entero con el idestadopedido que se desea consultar
	 * @return Se retorna un objeto Modelo estadoPedido con la informaci�n del estado pedido que se desea consultar.
	 */
	public static EstadoPedido retornarEstadoPedido(int idestadopedido)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEstadoPedidoEli = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		EstadoPedido Est = new EstadoPedido(0,"");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idestadopedido,descripcion from  estado_pedido  where idestadopedido = " + idestadopedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idesta = 0;
			String descripcion = "";
			while(rs.next()){
				idesta = rs.getInt("idestadopedido");
				descripcion = rs.getString("descripcion");
				break;
			}
			Est = new EstadoPedido(idesta,descripcion);
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
		return(Est);
	}

	/**
	 * M�todo que se encarga de editar un estado pedido con base en la informaci�n enviada como par�metro.
	 * @param Est Recibe como par�metro un objeto Modleo Estado pedido con base en el cual se realiza la edici�n
	 * @return Se retorna un string con el resultado del proceso de edici�n.
	 */
	public static String editarEstadoPedido(EstadoPedido Est)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String update = "update estado_pedido set descripcion ='" + Est.getDescripcion() +  "'  where idestadopedido = " + Est.getIdestadopedido(); 
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
	 * M�todo que se encarga de retornar un ArrayList la informaci�n de los estados pedidos creados en el sistema.
	 * @return ArrayList con los objetos de Modelo EstadoPedido creados en el sistema.
	 */
	public static ArrayList<EstadoPedido> obtenerEstadosPedido()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<EstadoPedido> estadosPedidos = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select e.idestadopedido, e.descripcion from estado_pedido e";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idestadopedido;
			String descripcion;
			while(rs.next()){
				idestadopedido = rs.getInt("idestadopedido");
				descripcion = rs.getString("descripcion");
				EstadoPedido est = new EstadoPedido( idestadopedido, descripcion);
				estadosPedidos.add(est);
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
		return(estadosPedidos);
		
	}


}
