package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogBloqueoTienda;
import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class PedidoGestionLinkDAO {
	
	public static boolean ingresarObsGestionLink(int idPedido, String observacion)
	{
		boolean resultado = false;
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido_gestion_link (idpedido, observacion) values ( " + idPedido + " , '" + observacion + "')";
			logger.info(insert);
			stm.executeUpdate(insert);
			resultado = true;
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
		return(resultado);
	}

}
