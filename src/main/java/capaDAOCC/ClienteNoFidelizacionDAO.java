package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.ClienteFidelizacion;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
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
public class ClienteNoFidelizacionDAO {

	
	/**
	 * Método que inserta un correo como cliente que no desea estar en el plan de fidelizacion
	 * @param correo
	 * @return
	 */
	public static boolean insertarClienteNoFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into cliente_no_fidelizacion (correo) values ('" + correo + "')";
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
	
	
	/**
	 * Método que valida si un correo está dentro de los clientes No Fidelizacion
	 * @param correo
	 * @return
	 */
	public static boolean validarExistenciaClienteNoFidelizacion(String correo)
	{
		Logger logger = Logger.getLogger("log_file");
		boolean respuesta = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		PreparedStatement ps = null;
		try
		{
			String select = "select * from  cliente_no_fidelizacion where correo = ?";
			ps = con1.prepareStatement(select);
			ps.setString(1, correo);
			logger.info("Consulta: " + ps.toString());
			ps.executeQuery();
			ResultSet rs = ps.executeQuery();
	        respuesta = rs.next();
	        rs.close();
	        ps.close();
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
