package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class MunicipioDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<Municipio> obtenerMunicipios()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Municipio> municipios = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from municipio";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idMunicipio = rs.getInt("idmunicipio");
				String nombre = rs.getString("nombre");
				Municipio municipio = new Municipio(idMunicipio,nombre);
				municipios.add(municipio);
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
		return(municipios);
		
	}
	
	/**
	 * Método qeu se encarga de retornar el id de un Municipio dado el nombre de un Munipio
	 * @param municipio Se recibe como parámetro un String con el nombre del Municipio.
	 * @return Se retorna un entero con el id del municipio según el nombre del Municipio enviado como parámetro.
	 */
	public static int obteneridMunicipio(String municipio)
	{
		Logger logger = Logger.getLogger("log_file");
		int idmunicipio=0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select idmunicipio from municipio where nombre = '"+ municipio + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				idmunicipio = rs.getInt("idmunicipio");
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
		return(idmunicipio);
	}
	
	public static String obtenerMunicipio(int idmunicipio)
	{
		Logger logger = Logger.getLogger("log_file");
		String municipio= "";
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select nombre from municipio where idmunicipio = '"+ idmunicipio + "'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				municipio = rs.getString("nombre");
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
		return(municipio);
	}

}
