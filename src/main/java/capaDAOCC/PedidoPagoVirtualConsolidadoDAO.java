package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaModeloCC.Municipio;
import capaModeloCC.PedidoPagoVirtualConsolidado;
import conexionCC.ConexionBaseDatos;


public class PedidoPagoVirtualConsolidadoDAO {
	
	public static int insertarPedidoPagoVirtual(int idPedidoTienda, int idTienda,  double valorPagoVirtual, double valorPedido, String idLink)
	{
		int idPedidoVirtual = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido_pago_virtual_consolidado (idpedidotienda, idtienda, valor_pago_virtual, valor_pedido, idlink, estado) values (" + idPedidoTienda + " , " + idTienda + " , " + valorPagoVirtual + " , " + valorPedido +" , '" + idLink + "' , 'PENDIENTE')"; 
			System.out.println(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoVirtual=rs.getInt(1);
				
	        }
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idPedidoVirtual);
	}
	
	public static int insertarPedidoPagoVirtualPagado(int idPedidoTienda, int idTienda,  double valorPagoVirtual, double valorPedido, String idLink, String tipoPago)
	{
		int idPedidoVirtual = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido_pago_virtual_consolidado (idpedidotienda, idtienda, valor_pago_virtual, valor_pedido, idlink, estado, notificado, tipopago, fechapagovirtual) values (" + idPedidoTienda + " , " + idTienda + " , " + valorPagoVirtual + " , " + valorPedido +" , '" + idLink + "' , 'PAGADO'"+ ",'S'" + ",'"+ tipoPago + "'," +"NOW()" +")"; 
			System.out.println(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idPedidoVirtual=rs.getInt(1);
				
	        }
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
			return(0);
		}
		return(idPedidoVirtual);
	}
	
	/**
	 * Método para validar si el idLink pagado en WOMPI proviene de un Pago de Tienda
	 * @param idLink
	 * @param tipoPago
	 * @return
	 */
	public static boolean validarPagoWompiTienda(String idLink, String tipoPago)
	{
		boolean respuesta = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido_pago_virtual_consolidado set fechapagovirtual = CURRENT_TIMESTAMP(), tipopago = '" + tipoPago + "' , estado = 'PAGADO' where idlink = '"+ idLink + "'";
			System.out.println(update);
			logger.info(update);
			int idPedidoAct = 0;
			int actualizados = stm.executeUpdate(update);
			if(actualizados > 0)
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
	 * Método qeu se encarga de retonar un ArrayList con los pedidos que se deberán notificar como pagados a las tiendas
	 * @return
	 */
	public static ArrayList<PedidoPagoVirtualConsolidado> obtenerPagosNotificarTienda()
	{
		ArrayList<PedidoPagoVirtualConsolidado> pagosNoti = new ArrayList();
		//Obtenemos la fecha actual
		Date fechaActual = new Date();
		DateFormat formatoFinal = new SimpleDateFormat("yyyy-MM-dd");
		String strFechaActual = formatoFinal.format(fechaActual);
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from pedido_pago_virtual_consolidado where estado = 'PAGADO' and notificado = 'N' and fechapagovirtual >= '"+ strFechaActual + " 00:00:00'";
			System.out.println(consulta);
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idPedidoVirtual;
			int idTienda;
			int idPedidoTienda;
			double valorPagoVirtual;
			double valorPedido;
			String idLink;
			String estado;
			String tipoPago;
			String notificado;
			String fechaPagoVirtual;
			while(rs.next()){
				idPedidoVirtual = Integer.parseInt(rs.getString("idpedidovirtual"));
				idTienda = Integer.parseInt(rs.getString("idtienda"));
				idPedidoTienda = Integer.parseInt(rs.getString("idpedidotienda"));
				valorPagoVirtual = Double.parseDouble(rs.getString("valor_pago_virtual"));
				valorPedido = Double.parseDouble(rs.getString("valor_pedido"));
				idLink = rs.getString("idlink");
				estado = rs.getString("estado");
				tipoPago = rs.getString("tipopago");
				notificado = rs.getString("notificado");
				fechaPagoVirtual = rs.getString("fechapagovirtual");
				PedidoPagoVirtualConsolidado pagoTemp = new PedidoPagoVirtualConsolidado(idPedidoVirtual, idTienda, idPedidoTienda, valorPagoVirtual, valorPedido, idLink, estado, tipoPago,notificado, fechaPagoVirtual);
				pagosNoti.add(pagoTemp);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			System.out.println(e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(pagosNoti);
		
	}
	
	
	/**
	 * Método que encarga de actualizar el estado de notificado de un pago que ya fue notificado a una tienda
	 * @param idLink
	 * @return
	 */
	public static boolean actualizarNotificacionPago(String idLink)
	{
		boolean respuesta = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			// Actualizamos la tabla pedido con el numero pedido pixel y le ponemos estado al pedido = 1, indicando que ya fue enviado a la tienda.
			String update = "update pedido_pago_virtual_consolidado set notificado = 'S' where idlink = '"+ idLink + "'";
			logger.info(update);
			int idPedidoAct = 0;
			int actualizados = stm.executeUpdate(update);
			if(actualizados > 0)
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

}
