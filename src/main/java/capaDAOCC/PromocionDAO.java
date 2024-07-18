	package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.Promocion;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class PromocionDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<Promocion> obtenerPromociones()
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Promocion> promociones = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDDatamartLocal();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from promocion";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			int idPromocion = 0;
			String descripcion = "";
			Promocion promocion;
			while(rs.next()){
				idPromocion = rs.getInt("idpromocion");
				descripcion = rs.getString("descripcion");
				promocion = new Promocion(idPromocion,descripcion);
				promociones.add(promocion);
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
		return(promociones);
		
	}
	
}
