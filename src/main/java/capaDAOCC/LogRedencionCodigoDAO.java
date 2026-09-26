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
			//Con parametros, y con fecha: antes el log no guardaba cuando paso ni de donde vino. Este es el camino
			//VIEJO (POS y pantallas sin actualizar); el flujo nuevo escribe con pedido y tienda desde CodigoPromoDAO.
			java.sql.PreparedStatement stm = con1.prepareStatement("insert into log_redencion_codigo (idofertacliente, descuento, saldo, usuario_uso, fecha_real, origen, estado) values (?, ?, ?, ?, now(), 'LEGADO', 'OK')", Statement.RETURN_GENERATED_KEYS);
			stm.setInt(1, idOfertaCliente);
			stm.setDouble(2, descuento);
			stm.setDouble(3, descuentoSobrante);
			stm.setString(4, usuarioUso == null ? "" : (usuarioUso.length() > 20 ? usuarioUso.substring(0, 20) : usuarioUso));
			logger.info("log_redencion_codigo LEGADO oferta_cliente " + idOfertaCliente);
			stm.executeUpdate();
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
