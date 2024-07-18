package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class LogRedencionCodigoDAO {

	public static int insertarLogRedencionCodigo(int idOfertaCliente, String usuarioUso, double descuentoSobrante, double descuento)
	{
		Logger logger = Logger.getLogger("log_file");
		int idLog = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_redencion_codigo (idofertacliente,descuento,saldo,usuario_uso) values (" + idOfertaCliente + "," + descuento + "," + descuentoSobrante + ",'" + usuarioUso + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idLog =rs.getInt(1);
				logger.info("Id log  " + idLog);
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
		return(idLog);
	}
	
}
