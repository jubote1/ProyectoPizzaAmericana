package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EncuestaLaboralDetalle;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementaci�n de toda la interacci�n con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class EncuestaLaboralDetalleDAO {
	

	public static ArrayList<EncuestaLaboralDetalle> obtenerEncLaboralDetalle(int idEncuesta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		ArrayList<EncuestaLaboralDetalle> encLaboralDetalle = new ArrayList<EncuestaLaboralDetalle>();
		EncuestaLaboralDetalle encLaboralDetTemp = new EncuestaLaboralDetalle(0, 0, "", "", 0, 0, 0, 0,"", "",0);
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from encuesta_laboral_detalle where idencuesta =" + idEncuesta + " order by orden";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idEncuestaDetalle;
			String descripcion;
			String tipoRespuesta;
			double valorInicial, valorFinal, valorEscala, valorDefecto;
			String alertar;
			String valorAlertar;
			String obligatorio;
			int orden;
			while(rs.next()){
				idEncuestaDetalle = rs.getInt("idencuestadetalle");
				descripcion = rs.getString("descripcion");
				tipoRespuesta = rs.getString("tipo_respuesta");
				orden = rs.getInt("orden");
				try
				{
					valorInicial = rs.getDouble("valor_inicial");
				}catch(Exception e)
				{
					valorInicial = 0;
				}
				try
				{
					valorFinal = rs.getDouble("valor_final");
				}catch(Exception e)
				{
					valorFinal = 0;
				}
				try
				{
					valorEscala = rs.getDouble("valor_escala");
				}catch(Exception e)
				{
					valorEscala = 0;
				}
				try
				{
					valorDefecto = rs.getDouble("valor_defecto");
				}catch(Exception e)
				{
					valorDefecto = 0;
				}
				alertar = rs.getString("alertar");
				valorAlertar = rs.getString("valor_alertar");
				//obligatorio = rs.getString("obligatorio");
				encLaboralDetTemp = new EncuestaLaboralDetalle(idEncuestaDetalle, idEncuesta, descripcion, tipoRespuesta, valorInicial, valorFinal, valorEscala, valorDefecto, alertar, valorAlertar,orden);
				encLaboralDetalle.add(encLaboralDetTemp);
			}
			rs.close();
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
		return(encLaboralDetalle);
		
	}
	
	


}
