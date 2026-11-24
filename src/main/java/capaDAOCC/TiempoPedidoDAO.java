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
			String consulta = "select a.idtienda, a.tiempoentrega, b.nombre, "
					+ "IFNULL((SELECT 'BLOQUEADA' FROM tienda_bloqueada c WHERE c.idtienda = b.idtienda and c.canal = 'TODOS' limit 1),'DISPONIBLE') AS estado, "
					+ "IFNULL((SELECT GROUP_CONCAT(c.canal SEPARATOR ', ') FROM tienda_bloqueada c WHERE c.idtienda = b.idtienda), '') AS canales_bloqueados "
					+ "from tiempo_pedido_tienda a, tienda b where a.idtienda = b.idtienda";
			logger.info(consulta);
			ResultSet rs;
			try {
				rs = stm.executeQuery(consulta);
			} catch (Exception exCol) {
				consulta = "select a.idtienda, a.tiempoentrega, b.nombre, "
						+ "IFNULL((SELECT 'BLOQUEADA' FROM tienda_bloqueada c WHERE c.idtienda = b.idtienda limit 1),'DISPONIBLE') AS estado, "
						+ "'' AS canales_bloqueados "
						+ "from tiempo_pedido_tienda a, tienda b where a.idtienda = b.idtienda";
				rs = stm.executeQuery(consulta);
			}
			int idtienda;
			int minutosPedido;
			String tienda;
			String estado;
			String canalesBloqueados;
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
				canalesBloqueados = "";
				try {
					canalesBloqueados = rs.getString("canales_bloqueados");
				} catch (Exception eCanal) {
					canalesBloqueados = "";
				}
				tie = new TiempoPedido(idtienda, tienda, minutosPedido, estado, canalesBloqueados);
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
	
	public static int retornarTiempoPedidoTienda(int idtienda, String canal)
	{
		int tiempo = 0;
		int estadoTienda = 0;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String canalCons = (canal == null || canal.trim().isEmpty()) ? "TODOS" : canal.trim().toUpperCase();
			String filtroBloqueo;
			if (canalCons.equals("TODOS")) {
				filtroBloqueo = "b.canal = 'TODOS'";
			} else {
				filtroBloqueo = "(b.canal = 'TODOS' or b.canal = '" + canalCons + "')";
			}
			String consulta = "select a.tiempoentrega, IFNULL((SELECT b.idtienda FROM tienda_bloqueada b WHERE b.idtienda = a.idtienda and " 
					+ filtroBloqueo + " limit 1), 0 ) AS estado_tienda from tiempo_pedido_tienda a where a.idtienda =" + idtienda;
			logger.info(consulta);
			ResultSet rs;
			try {
				rs = stm.executeQuery(consulta);
			} catch (Exception exCol) {
				consulta = "select a.tiempoentrega, IFNULL((SELECT b.idtienda FROM tienda_bloqueada b WHERE b.idtienda = a.idtienda limit 1), 0 ) AS estado_tienda from tiempo_pedido_tienda a where a.idtienda =" + idtienda;
				rs = stm.executeQuery(consulta);
			}
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

	public static int retornarTiempoPedidoTienda(int idtienda)
	{
		return retornarTiempoPedidoTienda(idtienda, "TODOS");
	}

}
