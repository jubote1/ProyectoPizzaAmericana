package capaDAOCC;

import java.sql.Connection;
import org.apache.log4j.Logger;

import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.Municipio;
import capaModeloCC.PermisosGrupo;
import conexionCC.ConexionBaseDatos;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Clase que se encarga de la implementación de toda la interacción con la base de datos para la entidad Municipio.
 * @author JuanDavid
 *
 */
public class PermisosGrupoDAO {
	
	/**
	 * Método que se encarga de retornar la información de todos los municipios definidos en el sistema.
	 * @return Se retorna un ArrayList con todos los municipios definidos en el sistema
	 */
	public static ArrayList<PermisosGrupo> obtenerPermisosGrupo(int idGrupo)
	{
		Logger logger = Logger.getLogger("log_file");
		ArrayList<PermisosGrupo> permisos = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDInventario();
		PermisosGrupo temp;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from permisos_grupo where idgrupo = " + idGrupo;
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				int idPermiso = rs.getInt("idpermiso");
				String nombrePermiso = rs.getString("nombre_permiso");
				temp = new PermisosGrupo(idPermiso, idGrupo, nombrePermiso);
				permisos.add(temp);
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
		return(permisos);
		
	}
}
