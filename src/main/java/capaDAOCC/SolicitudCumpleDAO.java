package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoWompi;
import capaModeloCC.SolicitudCumple;
import capaModeloCC.SolicitudFactura;
import conexionCC.ConexionBaseDatos;

public class SolicitudCumpleDAO {

	public static int insertarSolicitudCumple(SolicitudCumple solCumple)
	{
		Logger logger = Logger.getLogger("log_file");
		int idSolicitud = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String fecha = solCumple.getFecha();
		String fechaTrans = fecha.substring(6, 10)+"-"+fecha.substring(3, 5)+"-"+fecha.substring(0, 2);	
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into solicitud_cumple (fecha, idtienda, idpedido) values ('" + fechaTrans + "'," + solCumple.getIdTienda() + "," + solCumple.getIdPedido() +")"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idSolicitud  =rs.getInt(1);
				logger.info("Id Solicitud  " + idSolicitud );
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
		return(idSolicitud );
	}
	
	public static ArrayList<SolicitudCumple> consultarSolicitudesCumple(String fechaInicial, String fechaFinal)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<SolicitudCumple> solicitudes = new ArrayList();
		SolicitudCumple solTemp;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idSolicitud;
		String fecha = "";
		int idPedido = 0;
		int idTienda = 0;
		String tienda;
		String fechaIni = fechaInicial.substring(6, 10)+"-"+fechaInicial.substring(3, 5)+"-"+fechaInicial.substring(0, 2) + " 00:00:00";	
		String fechaFin = fechaFinal.substring(6, 10)+"-"+fechaFinal.substring(3, 5)+"-"+fechaFinal.substring(0, 2) + " 23:59:59";
		try
		{
			Statement stm = con1.createStatement();
			String select = "";
			select = "select a.*, b.nombre from  solicitud_cumple a, tienda b where a.idtienda = b.idtienda and a.fecha >= '" + fechaIni +"' and a.fecha <= '" + fechaFin + "'";
	
			logger.info(select);
			ResultSet rs = stm.executeQuery(select);
			while (rs.next()){
				idSolicitud = rs.getInt("idsolicitud_cumple");
				fecha = rs.getString("fecha");
				idTienda = rs.getInt("idtienda");
				idPedido = rs.getInt("idpedido");
				tienda = rs.getString("nombre");
				solTemp = new SolicitudCumple(idSolicitud,fecha,idTienda, idPedido );
				solTemp.setTienda(tienda);
				solicitudes.add(solTemp);
	        }
			rs.close();
			stm.close();
			con1.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
			logger.error(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(solicitudes);
	}

}
