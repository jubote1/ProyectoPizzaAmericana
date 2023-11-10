package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.PedidoLinkWompi;
import capaModeloCC.TiendaFranquiciaWompi;
import conexionCC.ConexionBaseDatos;

public class PedidoLinkWompiDAO {
	
	public static PedidoLinkWompi obtenerPedidoLinkWompi(String link)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		PedidoLinkWompi respuesta = new PedidoLinkWompi("", 0, "", "");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from pedido_link_wompi where link = '"+ link+ "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String tienda;
			int idTienda;
			String pedido;
			while(rs.next()){
				tienda = rs.getString("tienda");
				idTienda = rs.getInt("idtienda");
				pedido = rs.getString("pedido");
				respuesta = new PedidoLinkWompi(tienda, idTienda, pedido, link);
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
		return(respuesta);
	}
	
	public static void insertarPedidoLinkWompi(PedidoLinkWompi pedidoLink)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String apiLlavePrivada = "";
		String apiLlavePublica = "";
		String correo = "";
		try
		{
			Statement stm = con1.createStatement();
			String insertar = "insert into pedido_link_wompi (tienda, idtienda, pedido, link) values ('" + pedidoLink.getTienda()+ "' , " + pedidoLink.getIdTienda() + " , '" + pedidoLink.getPedido() + "' , '" + pedidoLink.getLink() + "')";
			logger.info(insertar);
			stm.executeUpdate(insertar);
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
	}

}
