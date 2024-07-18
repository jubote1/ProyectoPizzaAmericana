package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.DetallePedidoAdicion;
import conexionCC.ConexionBaseDatos;

public class AdicionDetallePedidoDAO {
	
	public static ArrayList <Integer> ObtenerIdAdicionDetallePedido (int iddetallepedido)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Integer> respuesta = new ArrayList();
		String consulta = "select distinct(a.iddetallepedidoadicion)"
				+ " from adicion_detalle_pedido a where a.iddetallepedidopadre =" + iddetallepedido; 
		logger.info(consulta);
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			ResultSet rs = stm.executeQuery(consulta);
			int iddetallepedidoadicion;
			while(rs.next())
			{
				iddetallepedidoadicion = rs.getInt(1);
				respuesta.add(iddetallepedidoadicion);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch(Exception e)
		{
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
