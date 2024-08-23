package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.FidelizacionTransaccion;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class FidelizacionTransaccionDAO {

	
	public static boolean existeFidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from fidelizacion_transaccion where correo = '"+ correo + "' and idtienda = " + idTienda + " and idpedidotienda = " + idPedidoTienda;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				respuesta = true;
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
	
	public static boolean insertarFidelizacionTransaccion(FidelizacionTransaccion transaccion)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into fidelizacion_transaccion (correo,idtienda, idpedidotienda, valor_neto, puntos) values ('" + transaccion.getCorreo() +"' ," + transaccion.getIdTienda()+ " ," +  transaccion.getIdPedidoTienda()+ "," + transaccion.getValorNeto() + ", " + transaccion.getPuntos() +")";
			logger.info(insert);
			stm.executeUpdate(insert);
			respuesta = true;
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
	
}
