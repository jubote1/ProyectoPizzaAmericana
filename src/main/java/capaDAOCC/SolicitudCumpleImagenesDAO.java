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
import capaModeloCC.SolicitudImagenes;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class SolicitudCumpleImagenesDAO {
	
	/**
	 * M�todo de la capa DAO que se encarga de implementar la inserci�n de la entidad solicitudPQRS, recibiendo como par�metro
	 * un objeto tipo SolicitudPQRS, retornar� como resultado de la inserci�n el idsolicitudPQRS asignado por la base de datos
	 * en base a un campo configurado como autoincrementable en la misma.
	 * @param solicitud Se recibe como par�metro un objeto de la capaModelo SolicitudPQRS
	 * @return Se retorna valor intero con el idSolicitudPQRS asignado por la base de datos.
	 */
	public static int insertarSolicitudCumpleImagen(SolicitudImagenes solicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		int idImagen = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_cumple_imagenes (idsolicitud_cumple,ruta) values (" + solicitud.getIdSolicitud() + ", '" + solicitud.getRutaImagen() + "')" ; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idImagen =rs.getInt(1);
				logger.info("Id imagen insertada en bd " + idImagen);
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
		return(idImagen);
	}
	
	public static ArrayList<String> consultarSolicitudCumpleImagenes(int idSolicitud)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String> imagenes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitud_cumple_imagenes where idsolicitud_cumple = " + idSolicitud; 
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
