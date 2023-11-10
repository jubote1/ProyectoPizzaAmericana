package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaModeloCC.FormaPago;
import capaModeloCC.Pedido;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class SolicitudPQRSImagenesDAO {
	
	/**
	 * Método de la capa DAO que se encarga de implementar la inserción de la entidad solicitudPQRS, recibiendo como parámetro
	 * un objeto tipo SolicitudPQRS, retornará como resultado de la inserción el idsolicitudPQRS asignado por la base de datos
	 * en base a un campo configurado como autoincrementable en la misma.
	 * @param solicitud Se recibe como parámetro un objeto de la capaModelo SolicitudPQRS
	 * @return Se retorna valor intero con el idSolicitudPQRS asignado por la base de datos.
	 */
	public static int insertarSolicitudPQRSImagen(SolicitudPQRSImagenes solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitudPQRSImaIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitudpqrs_imagenes (idsolicitudPQRS,rutaimagen) values ('" + solicitud.getIdSolicitudPQRS() + "', '" + solicitud.getRutaImagen() + "')" ; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idSolicitudPQRSImaIns =rs.getInt(1);
				logger.info("Id SolicitudPQRS insertada en bd " + idSolicitudPQRSImaIns);
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
		return(idSolicitudPQRSImaIns);
	}
	
	public static ArrayList<String> consultarSolicitudPQRSImagenes(int idSolicitudPRQS)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String> imagenes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitudpqrs_imagenes where idsolicitudPQRS = " + idSolicitudPRQS; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{
				String imagenTmp = rs.getString("rutaimagen");
				imagenes.add(imagenTmp);
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
		}
		return(imagenes);
	}

}
