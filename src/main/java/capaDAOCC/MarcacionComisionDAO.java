package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Marcacion;
import capaModeloCC.MarcacionComision;
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
public class MarcacionComisionDAO {
	
	/**
	 * M�todo que se encarga de retornar la informaci�n de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<MarcacionComision> obtenerMarcacionComision(int idMarcacion)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<MarcacionComision> marcaciones = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from marcacion_comision where idmarcacion = " + idMarcacion;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idTienda; 
			int comision;
			int comisionfull;
			while(rs.next()){
				
				idTienda = rs.getInt("idtienda");
				comision = rs.getInt("comision");
				comisionfull = rs.getInt("comisionfull");
				MarcacionComision marcacionComision = new MarcacionComision(idMarcacion, idTienda, comision, comisionfull);
				marcaciones.add(marcacionComision);
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
		return(marcaciones);
		
	}
	
	
}
