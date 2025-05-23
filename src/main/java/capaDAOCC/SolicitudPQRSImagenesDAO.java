package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import capaModeloCC.FormaPago;
import capaModeloCC.Pedido;
import capaModeloCC.SolicitudPQRS;
import capaModeloCC.SolicitudPQRSImagenes;
import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

public class SolicitudPQRSImagenesDAO {
	
	/**
	 * M�todo de la capa DAO que se encarga de implementar la inserci�n de la entidad solicitudPQRS, recibiendo como par�metro
	 * un objeto tipo SolicitudPQRS, retornar� como resultado de la inserci�n el idsolicitudPQRS asignado por la base de datos
	 * en base a un campo configurado como autoincrementable en la misma.
	 * @param solicitud Se recibe como par�metro un objeto de la capaModelo SolicitudPQRS
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
	
	public static boolean eliminarSolicitudPQRSImagenPorId(int idImagen) {
	    Logger logger = Logger.getLogger("log_file");
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    boolean eliminado = false;

	    try {
	        Statement stm = con1.createStatement();
	        String deleteSQL = "DELETE FROM solicitudpqrs_imagenes WHERE idimagen = " + idImagen;
	        logger.info(deleteSQL);

	        int filasAfectadas = stm.executeUpdate(deleteSQL);
	        eliminado = filasAfectadas > 0;

	        if (eliminado) {
	            logger.info("Imagen con ID " + idImagen + " eliminada correctamente.");
	        } else {
	            logger.warn("No se encontró imagen con ID " + idImagen + " para eliminar.");
	        }

	        stm.close();
	        con1.close();
	    } catch (Exception e) {
	        logger.error("Error al eliminar imagen con ID " + idImagen + ": " + e.toString());
	        try {
	            con1.close();
	        } catch (Exception e1) {
	            logger.error("Error al cerrar conexión: " + e1.toString());
	        }
	        return false;
	    }

	    return eliminado;
	}

	public static ArrayList<JSONObject> consultarSolicitudPQRSImagenes(int idSolicitudPRQS)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<JSONObject> imagenes = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from solicitudpqrs_imagenes where idsolicitudPQRS = " + idSolicitudPRQS; 
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while(rs.next())
			{   JSONObject obj = new JSONObject();
				String imagenTmp = rs.getString("rutaimagen");
				int idimagen = rs.getInt("idimagen");
				obj.put("rutaimagen", imagenTmp);
				obj.put("idimagen", idimagen);
				imagenes.add(obj);
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
