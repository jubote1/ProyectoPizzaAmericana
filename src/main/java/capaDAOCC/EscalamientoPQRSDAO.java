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
	public static ArrayList<EscalamientoPQRS> consultarEscalamientoPQRS(int idSolicitudPQRS)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<EscalamientoPQRS> escalamientos = new ArrayList();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		PreparedStatement ps = null;
		try
		{
			Statement stm = con1.createStatement();
			String select = "select * from  escalamiento_pqrs where idsolicitudPQRS = ?" ;
			logger.info(select);
			ps = con1.prepareStatement(select);
	        ps.setInt(1, idSolicitudPQRS);
	        ResultSet rs = ps.executeQuery();
	        EscalamientoPQRS escTemp;
	        int idEscalamiento;
	        String areaResponsable;
	        String fechaEscalamiento;
	        String fechaResolucion;
	        String solucionado;
	        boolean boolSol = false;
	        while(rs.next())
	        {
	        	idEscalamiento = rs.getInt("idescalamiento");
	        	areaResponsable = rs.getString("area_responsable");
	        	fechaEscalamiento = rs.getString("fecha_escalamiento");
	        	fechaResolucion = rs.getString("fecha_resolucion");
	        	if(fechaResolucion == null || fechaResolucion.equals(new String("")) ||fechaResolucion.equals(new String("null")))
	        	{
	        		fechaResolucion = "";
	        	}
	        	solucionado = rs.getString("solucionado");
	        	if(solucionado.equals(new String("1")))
	        	{
	        		boolSol = true;
	        	}else
	        	{
	        		boolSol = false;
	        	}
	        	escTemp = new EscalamientoPQRS(idEscalamiento, idSolicitudPQRS, areaResponsable, fechaEscalamiento,fechaResolucion, boolSol);
	        	escalamientos.add(escTemp);
	        }
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
		return(escalamientos);
	}
	

}
