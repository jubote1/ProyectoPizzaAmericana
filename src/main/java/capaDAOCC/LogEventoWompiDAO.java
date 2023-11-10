package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import capaModeloCC.LogEventoWompi;
import conexionCC.ConexionBaseDatos;

public class LogEventoWompiDAO {
	
	public static int insertarLogEventoWompi(LogEventoWompi evento)
	{
		Logger logger = Logger.getLogger("log_file");
		int idEventoIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String insert = "insert into log_evento_wompi (id_link, evento, estado, datos) values ('" + evento.getIndLink() + "', '" + evento.getEvento() + "' , '" + evento.getEstado() + "' , '" + evento.getDatosJSON() + "')"; 
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()){
				idEventoIns =rs.getInt(1);
				logger.info("Id forma evento en bd " + idEventoIns);
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
		return(idEventoIns);
	}

	public static ArrayList<LogEventoWompi> consultarLogEventoWompi(String idLink)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<LogEventoWompi> eventos = new ArrayList();
		try
		{
			Statement stm = con1.createStatement();
			String select = "SELECT * FROM log_evento_wompi WHERE id_link = '" + idLink +"'"; 
			ResultSet rs = stm.executeQuery(select);
			LogEventoWompi eventoTemp;
			String fechaHora;
			String evento;
			String estado;
			while(rs.next())
			{
				fechaHora = rs.getString("fecha_hora");
				evento = rs.getString("evento");
				estado = rs.getString("estado");
				eventoTemp = new LogEventoWompi(idLink,evento,estado,"");
				eventoTemp.setFechaHora(fechaHora);
				eventos.add(eventoTemp);
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
		return(eventos);
	}
}
