package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class PedidoTiendaVirtualDAO {

	/**
	 * En este método trabajaremos en la inserción del log de la llegada de la orden inicialmente.
	 * @param id
	 * @return
	 */
	public static int insertarPedidoTiendaVirtual(int id)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		boolean existeOrden = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			//Realizamos la validacion de que el id no esté insertado ya
			String select = "select count(*) from pedido_tienda_virtual where idordencomercio = " + id;
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				int cantidad = rs.getInt(1);
				if(cantidad > 0)
				{
					existeOrden = true;
				}
				break;
			}
			if(!existeOrden)
			{
				String insert = "insert into pedido_tienda_virtual (idordencomercio,estado) values (" + id + ", 'PEN')"; 
				logger.info(insert);
				stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
				rs = stm.getGeneratedKeys();
				if (rs.next()){
					idEventoIns =rs.getInt(1);
					logger.info("Id interno en bd " + idEventoIns);
		        }
			}
			rs.close();
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
		return(idEventoIns);
	}
	
	
	/**
	 * Método que se encargará de actualizar el estado de un pedido virtual con base en el idInterno
	 * @param idInterno
	 * @param estado
	 * @return
	 */
	public static boolean actualizarPedidoTiendaVirtual(int idInterno , String estado)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean resultado = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String update = "update pedido_tienda_virtual set estado = '" + estado + "' where id_interno =" + idInterno; 
			logger.info(update);
			stm.executeUpdate(update);
			stm.close();
			con1.close();
			resultado = true;
		}
		catch (Exception e){
			resultado = false;
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
	
}
