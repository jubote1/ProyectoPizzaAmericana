package capaDAOCC;

import java.sql.Connection;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.CoordenadaPoligono;
import capaModeloCC.Poligono;
import capaModeloCC.Producto;
import capaModeloCC.Tienda;
import capaModeloCC.Usuario;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que se encarga de implementar todo lo relacionado con la base de datos de la entidad tienda.
 * @author JuanDavid
 *
 */
public class CoordenadaPoligonoDAO {
	
/**
 * Método que se encarga de retornar todas las entidades Poligonos definidas en la base de datos
 * @return Se retorna un ArrayList con todas las entidades Poligono definidas en la base de datos.
 */
	public static ArrayList<CoordenadaPoligono> obtenerCoordenadasPoligono(int idPoligono)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<CoordenadaPoligono> coorPoligonos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeolocalizacion();
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from coordenadas_poligono where idpoligono =" + idPoligono;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idCoordenada = rs.getInt("idcoordenada");
				double latitud = rs.getDouble("latitud");
				double longitud = rs.getDouble("longitud");
				CoordenadaPoligono coorPoligono = new CoordenadaPoligono(idCoordenada,idPoligono, latitud, longitud);
				coorPoligonos.add(coorPoligono);
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
		return(coorPoligonos);
		
	}
	

}
