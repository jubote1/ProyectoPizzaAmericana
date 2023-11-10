package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.TiempoPedido;
import conexionCC.ConexionBaseDatos;


public class TiempoPedidoDAO {
	
	public static ArrayList<TiempoPedido> retornarTiemposPedidos()
	{
		ArrayList <TiempoPedido> tiemposTienda = new ArrayList();
		int tiempo = 0;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select a.idtienda, a.tiempoentrega, b.nombre, IFNULL((SELECT 'BLOQUEADA' FROM tienda_bloqueada c WHERE c.idtienda = b.idtienda),'DISPONIBLE') AS estado from tiempo_pedido_tienda a, tienda b where a.idtienda = b.idtienda";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idtienda;
			int minutosPedido;
			String tienda;
			String estado;
			TiempoPedido tie;
			while(rs.next()){
				
				try{
					idtienda = Integer.parseInt(rs.getString("idtienda"));
					
				}catch(Exception e){
					logger.error(e.toString());
					idtienda = 0;
				}
				try{
					minutosPedido = Integer.parseInt(rs.getString("tiempoentrega"));
					
				}catch(Exception e){
					logger.error(e.toString());
					minutosPedido = 0;
				}
				tienda = rs.getString("nombre");
				estado = rs.getString("estado");
				tie = new TiempoPedido(idtienda, tienda, minutosPedido,estado);
				tiemposTienda.add(tie);
				
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(tiemposTienda);
	}
	
	public static boolean actualizarTiempoPedido(int nuevotiempo, int idtienda, String user)
	{
		boolean respuesta;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String actualizacion = "update tiempo_pedido_tienda set tiempoentrega = " + nuevotiempo +  " where idtienda =" + idtienda ;
			logger.info(actualizacion);
			stm.executeUpdate(actualizacion);
			String insercionLog = "insert into log_tiempo_tienda (idtienda, usuario, nuevotiempo) values (" + idtienda +" , '" + user + "' , " + nuevotiempo + ")" ;
			logger.info(insercionLog);
			stm.executeUpdate(insercionLog);
			return(true);
			
		}catch (Exception e)
		{
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
				logger.error(e.toString());
			}
			return(false);
		}
		
	}
	
	public static int retornarTiempoPedidoTienda(int idtienda)
	{
		int tiempo = 0;
		int estadoTienda = 0;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select  a.tiempoentrega, IFNULL((SELECT b.idtienda FROM tienda_bloqueada b WHERE b.idtienda = a.idtienda), 0 ) AS estado_tienda from tiempo_pedido_tienda a where a.idtienda =" + idtienda;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			TiempoPedido tie;
			while(rs.next()){
				
				try{
					tiempo = Integer.parseInt(rs.getString("tiempoentrega"));
					estadoTienda = rs.getInt("estado_tienda");
				}catch(Exception e){
					logger.error(e.toString());
					idtienda = 0;
				}
				if(estadoTienda > 0)
				{
					tiempo = 100;
				}
				break;
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(tiempo);
	}

}
