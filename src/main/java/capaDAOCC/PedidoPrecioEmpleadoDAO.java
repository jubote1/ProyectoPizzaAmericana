package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.PedidoPrecioEmpleado;
import conexionCC.ConexionBaseDatos;

public class PedidoPrecioEmpleadoDAO {

	public static void insertarPedidoPrecioEmpleado(PedidoPrecioEmpleado pedPrecio)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido_precio_empleado (idempleado, fecha, idtienda, valor, idpedido, autorizado, codigo) values("
			+ pedPrecio.getIdEmpleado() + " ,'" + pedPrecio.getFecha() + "' ," + pedPrecio.getIdTienda() + " ," + pedPrecio.getValor() + " ," + pedPrecio.getIdPedido() + " ,'" + pedPrecio.getAutorizado() + "' , '" + pedPrecio.getCodigo() + "')" ;
			logger.info(insert);
			stm.executeUpdate(insert);
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
	
	public static void marcarAutorizadoPedidoPrecioEmpleado(String codigo)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update pedido_precio_empleado set autorizado = 'S' where codigo = '" + codigo + "'";
			logger.info(update);
			stm.executeUpdate(update);
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
	
	public static boolean validarExistenciaCodigo(String codigoPromocional)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		boolean resultado = false;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from pedido_precio_empleado  where codigo = '" + codigoPromocional + "'"; 
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
	
	public static int validarFrecuenciaPedidoPrecioEmpleado(int idEmpleado, String fechaAnterior, String fechaActual)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		int resultado = 0;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select count(*) from pedido_precio_empleado  where idempleado = " + idEmpleado + " and DATE_FORMAT(fecha_insercion, '%Y-%m-%d') >= '" + fechaAnterior + "' and DATE_FORMAT(fecha_insercion, '%Y-%m-%d') <= '"+ fechaActual + "' and autorizado = 'S'"; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				resultado = rs.getInt(1);
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
	
	public static ArrayList<PedidoPrecioEmpleado> consultarPedidosPrecioEmpleado(String fechaInicial , String fechaFinal)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		ArrayList<PedidoPrecioEmpleado> pedidos = new ArrayList();
		PedidoPrecioEmpleado pedTemp;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.*, b.nombre_largo, c.nombre from pedido_precio_empleado a, empleado b, pizzaamericana.tienda c where a.fecha_insercion >= '"+ fechaInicial +" 00:00:00' and a.fecha_insercion <= '" + fechaFinal + " 23:59:59' AND a.idempleado = b.id AND a.idtienda = c.idtienda  AND a.autorizado = 'S'" ;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idEmpleado;
			String fecha;
			int idTienda;
			double valor;
			int idPedido;
			String autorizado;
			String codigo;
			String fechaInsercion;
			String nombreEmpleado;
			String nombreTienda;
			while(rs.next())
			{
				idEmpleado = rs.getInt("idempleado");
				fecha = rs.getString("fecha");
				idTienda = rs.getInt("idtienda");
				valor = rs.getDouble("valor");
				idPedido = rs.getInt("idpedido");
				autorizado = rs.getString("autorizado");
				codigo = rs.getString("codigo");
				fechaInsercion = rs.getString("fecha_insercion");
				nombreEmpleado = rs.getString("nombre_largo");
				nombreTienda = rs.getString("nombre");
				pedTemp = new PedidoPrecioEmpleado(idEmpleado, fecha, idTienda, valor, idPedido,autorizado,codigo);
				pedTemp.setFechaInsercion(fechaInsercion);
				pedTemp.setNombreEmpleado(nombreEmpleado);
				pedTemp.setTienda(nombreTienda);
				pedidos.add(pedTemp);
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
		return(pedidos);
	}
	
}
