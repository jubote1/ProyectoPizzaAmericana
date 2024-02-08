package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.DescuentoGeneral;
import capaModeloCC.DetallePedido;
import conexionCC.ConexionBaseDatos;

public class DetallePedidoDAO {
	
	public static ArrayList<DetallePedido> obtenerDetallesPedidoResumido(int idPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<DetallePedido> detallesPedido = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from detalle_pedido where idpedido = " + idPedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idDetallePedido;
			int idProducto;
			double cantidad;
			int idEspecialidad1;
			int idEspecialidad2;
			double valorUnitario;
			double valorTotal;
			int idSaborTipoLiquido;
			int idExcepcion;
			
			while(rs.next()){
				idDetallePedido = rs.getInt("iddetalle_pedido");
				idProducto = rs.getInt("idProducto");
				cantidad = rs.getDouble("cantidad");
				idEspecialidad1 = rs.getInt("idespecialidad1");
				idEspecialidad2 = rs.getInt("idespecialidad2");
				valorUnitario = rs.getDouble("valorUnitario");
				valorTotal = rs.getDouble("valorTotal");
				idSaborTipoLiquido = rs.getInt("idsabortipoliquido");
				idExcepcion = rs.getInt("idexcepcion");
				DetallePedido detTemp = new DetallePedido(idDetallePedido,idProducto, idPedido, cantidad, idEspecialidad1,
						idEspecialidad2, valorUnitario, valorTotal, idSaborTipoLiquido, idExcepcion);
				detallesPedido.add(detTemp);
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
		return(detallesPedido);
		
	}
	
	
	/**
	 * M�todo que se encarga de calcular el total con un detalle pedido Padre que podra tener adiciones o modificadores
	 * @param idPedido
	 * @param idDetallePedido
	 * @return
	 */
	public static double obtenerTotalDetallePedidoPadre(int idPedido, int idDetallePedido)
	{
		Logger logger = Logger.getLogger("log_file");
		double valorTotal = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select sum(valorTotal) from detalle_pedido where idpedido = " + idPedido + " and (iddetalle_pedido = " + idDetallePedido + " or iddetalle_pedido in (select iddetallepedidoadicion from adicion_detalle_pedido where iddetallepedidopadre = " + idDetallePedido +"))";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			
			while(rs.next()){
				try
				{
					valorTotal = rs.getInt(1);
				}catch(Exception e)
				{
					valorTotal = 0;
				}
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
		return(valorTotal);
		
	}
	
	public static DetallePedido obtenerDetallePedido(int idDetallePedido)
	{
		Logger logger = Logger.getLogger("log_file");
		DetallePedido detallePedido = new DetallePedido(0,0,0,0,0,0,0,0,0,0);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from detalle_pedido where iddetalle_pedido = " + idDetallePedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			double cantidad;
			int idEspecialidad1;
			int idEspecialidad2;
			double valorUnitario;
			double valorTotal;
			int idSaborTipoLiquido;
			int idExcepcion;
			int idPedido;
			while(rs.next()){
				idPedido = rs.getInt("idpedido");
				idDetallePedido = rs.getInt("iddetalle_pedido");
				idProducto = rs.getInt("idProducto");
				cantidad = rs.getDouble("cantidad");
				idEspecialidad1 = rs.getInt("idespecialidad1");
				idEspecialidad2 = rs.getInt("idespecialidad2");
				valorUnitario = rs.getDouble("valorUnitario");
				valorTotal = rs.getDouble("valorTotal");
				idSaborTipoLiquido = rs.getInt("idsabortipoliquido");
				idExcepcion = rs.getInt("idexcepcion");
				detallePedido = new DetallePedido(idDetallePedido,idProducto, idPedido, cantidad, idEspecialidad1,
						idEspecialidad2, valorUnitario, valorTotal, idSaborTipoLiquido, idExcepcion);
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
		return(detallePedido);
		
	}
	
	public static String retornarIdProductosSalesManago(int idPedido)
	{
		Logger logger = Logger.getLogger("log_file");
		String respuesta = "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idproducto,idespecialidad1, idespecialidad2 from detalle_pedido where idpedido = " + idPedido;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idProducto;
			int idEspecialidad1;
			int idEspecialidad2;
			boolean esPrimero = true;
			while(rs.next()){
				idProducto = rs.getInt("idProducto");
				if(esPrimero)
				{
					respuesta = Integer.toString(idProducto);
					esPrimero = false;
				}else
				{
					respuesta = respuesta +" , " +  Integer.toString(idProducto);
				}

				idEspecialidad1 = rs.getInt("idespecialidad1");
				idEspecialidad2 = rs.getInt("idespecialidad2");
				if(idEspecialidad1 != 0)
				{
					respuesta = respuesta +" , I" +  Integer.toString(idEspecialidad1);
				}
				if(idEspecialidad2 != 0)
				{
					respuesta = respuesta +" , I" +  Integer.toString(idEspecialidad2);
				}
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


}
