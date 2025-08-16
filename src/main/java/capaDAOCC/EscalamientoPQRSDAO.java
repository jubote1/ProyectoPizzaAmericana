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
	
	/**
	 * Método que retorna los escalamientos de una pqrs determinada
	 * @param idSolicitudPQRS
	 * @return
	 */
	public static ArrayList<EscalamientoPQRS> consultarEscalamientoPQRS(int[] idsSolicitudPQRS) {
	    Logger logger = Logger.getLogger("log_file");
	    ArrayList<EscalamientoPQRS> escalamientos = new ArrayList<>();
	    if (idsSolicitudPQRS == null || idsSolicitudPQRS.length == 0) return escalamientos;

	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    PreparedStatement ps = null;

	    try {
	        // Construir placeholders (?, ?, ...) según cantidad de IDs
	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < idsSolicitudPQRS.length; i++) {
	            sb.append("?");
	            if (i < idsSolicitudPQRS.length - 1) sb.append(",");
	        }

	        String sql = "SELECT * FROM escalamiento_pqrs WHERE idsolicitudPQRS IN (" + sb.toString() + ")";
	        logger.info(sql);

	        ps = con1.prepareStatement(sql);
	        for (int i = 0; i < idsSolicitudPQRS.length; i++) {
	            ps.setInt(i + 1, idsSolicitudPQRS[i]);
	        }

	        ResultSet rs = ps.executeQuery();
	        while (rs.next()) {
	            int idEscalamiento = rs.getInt("idescalamiento");
	            int idSolicitud = rs.getInt("idsolicitudPQRS");
	            String areaResponsable = rs.getString("area_responsable");
	            String fechaEscalamiento = rs.getString("fecha_escalamiento");
	            String fechaResolucion = rs.getString("fecha_resolucion");
	            if (fechaResolucion == null || fechaResolucion.trim().isEmpty() || fechaResolucion.equals("null")) {
	                fechaResolucion = "";
	            }
	            boolean solucionado = "1".equals(rs.getString("solucionado"));

	            EscalamientoPQRS escTemp = new EscalamientoPQRS(
	                idEscalamiento, idSolicitud, areaResponsable, fechaEscalamiento, fechaResolucion, solucionado
	            );
	            escalamientos.add(escTemp);
	        }

	        ps.close();
	        con1.close();
	    } catch (Exception e) {
	        logger.error(e.toString());
	        try { con1.close(); } catch (Exception ignored) {}
	    }

	    return escalamientos;
	}

	

}
