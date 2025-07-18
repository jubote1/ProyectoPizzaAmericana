package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import capaModeloCC.EscalamientoPQRS;
import capaModeloCC.ExcepcionPrecio;
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
public class EscalamientoPQRSDAO {
	
	public static void insertarEscalamientoPQRS(EscalamientoPQRS escalamiento)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		PreparedStatement ps = null;
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into escalamiento_pqrs (idsolicitudPQRS,area_responsable) values(?,?)";
			logger.info(insert);
			ps = con1.prepareStatement(insert);
	        ps.setInt(1, escalamiento.getIdSolicitudPQRS());
	        ps.setString(2, escalamiento.getAreaResponsable());
	        ps.executeUpdate();
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
	}
	

}
