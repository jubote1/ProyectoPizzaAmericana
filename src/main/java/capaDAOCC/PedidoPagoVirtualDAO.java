package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.PedidoPagoVirtual;
import conexionCC.ConexionBaseDatos;

public class PedidoPagoVirtualDAO {
	
	public static int insertarPedidoPagoVirtual(PedidoPagoVirtual pedPag)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into pedido_pago_virtual (idpedido, email, telefono_celular, observacion) values (" + pedPag.getIdPedido() + ", '" + pedPag.getEmail() + "' , '" + pedPag.getTelefonoCelular() + "' , '" + pedPag.getObservacion() + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id forma evento en bd " + idEventoIns);
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
		return(idEventoIns);
	}

}
