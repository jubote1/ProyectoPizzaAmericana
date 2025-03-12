package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.SolicitudConciliacion;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class SolicitudConciliacionDAO {

	/**
	 * Método que se encarga de insertar la solicitud de conciliación de QR o Datáfono
	 * @param solicitud
	 * @return
	 */
	public static boolean insertarSolicitudConciliacion(SolicitudConciliacion solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_conciliacion (fecha,origen,descripcion,idtienda,categoria,valor_analizar,telefono,idpedidotienda) values ('"+ solicitud.getFecha() + "','" + solicitud.getOrigen() + "' , '" + solicitud.getDescripcion() + "' , " + solicitud.getIdTienda() + " , '" + solicitud.getCategoria() + "' , " + solicitud.getValorAnalizar() + " , '" + solicitud.getTelefono() +"' , " + solicitud.getIdPedidoTienda() + ")";
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
