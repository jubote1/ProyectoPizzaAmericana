package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.result.ResultSetMetaData;

import capaModeloCC.EncuestaLaboral;
import capaModeloPOS.EncuestaAplicar;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class EncuestaLaboralDAO {
	

	public static EncuestaLaboral obtenerEncuestaLaboral(int idEncuesta)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		EncuestaLaboral encLaboral = new EncuestaLaboral(0, "", "", "" , "", "", ""); 
		
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from encuesta_laboral where idencuesta =" + idEncuesta;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String descripcion;
			String dependencia;
			String nombreEncuesta;
			String codigo;
			String version;
			String encabezado;
			while(rs.next()){
				descripcion = rs.getString("descripcion");
				dependencia = rs.getString("dependencia");
				nombreEncuesta = rs.getString("nombre_encuesta");
				codigo = rs.getString("codigo");
				version = rs.getString("version");
				encabezado = rs.getString("encabezado");
				encLaboral = new EncuestaLaboral(idEncuesta, descripcion, dependencia, nombreEncuesta, codigo, version, encabezado);
				break;
				
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
		return(encLaboral);
		
	}
	
	
	public static EncuestaAplicar obtenerIdEncuestaEmpExc(int idTipoEmpleado )
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		int idEncuesta = 0;
		String tienda = "";
		String rotacion = "";
		EncuestaAplicar respuesta = new EncuestaAplicar(0,"","");
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "SELECT a.idencuesta , a.tienda, a.rotacion FROM encuesta_laboral a, encuesta_laboral_tipo b , encuesta_laboral_tipo_detalle c\n" + 
					"WHERE a.idtipoencuesta = b.idtipoencuesta AND b.idtipoencuesta = c.idtipoencuesta AND c.idtipoempleado = " + idTipoEmpleado;
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				
				idEncuesta = rs.getInt(1);
				tienda = rs.getString(2);
				rotacion = rs.getString(3);
				respuesta = new EncuestaAplicar(idEncuesta, tienda, rotacion);
			}
			rs.close();
			stm.close();
			con1.close();
		}catch (Exception e){
			logger.info(e.toString());
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
