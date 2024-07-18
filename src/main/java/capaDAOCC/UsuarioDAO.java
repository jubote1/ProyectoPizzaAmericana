package capaDAOCC;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import org.apache.log4j.Logger;

import capaModeloCC.Usuario;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que se encarga de implementar toda la interacción con la base de datos para la entidad Usuario.
 * @author JuanDavid
 *
 */
public class UsuarioDAO {

	/**
	 * Método que se encarga de validar la existencia y de un usuario y su contraseña en la base de datos.
	 * @param usuario Se recibe como parámetro un objeto MOdelo Usuario, el cual trae la información base para la validación,
	 * autenticación del usuario.
	 * @return Se retorna un valor booleano que indica si el proceso de autenticación es satifactorio o no.
	 */
	public static boolean validarUsuario(Usuario usuario)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from usuario where nombre = '" + usuario.getNombreUsuario() + "' and password = '" + usuario.getContrasena()+"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next())
			{
				respuesta = true;
				break;
			}
				rs.close();
				stm.close();
				con1.close();
		}
		catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(respuesta);
		
	}
	
	/**
	 * Método que se encarga de validar si un usuario existe o no en la base de datos
	 * @param usuario Recibe como parámetro un objeto Modelo Usuario con base en el cual se realiza la consulta.
	 * @return Se retorna un valor booleano con base en el cual se realiza la validación del usuario en base de datos
	 * 
	 */
	public static String validarAutenticacion(Usuario usuario)
	{
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select administrador, nombre_largo, plataforma from usuario where nombre = '" + usuario.getNombreUsuario() + "'";
			ResultSet rs = stm.executeQuery(consulta);
			while(rs.next()){
				
				try{
					resultado = rs.getString("administrador");
					usuario.setNombreLargo(rs.getString("nombre_largo"));
					usuario.setPlataforma(rs.getString("plataforma"));
				}catch(Exception e){
					
					
				}
				rs.close();
				stm.close();
				con1.close();
			}
		}catch (Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(resultado);
	}
	
	/**
	 * Método que se encarga de revisar si el usuario existe y retornar el nombre completo y el idGrupo
	 * @param usuario
	 * @return
	 */
	public static capaModeloPOS.Usuario validarUsuarioInventario(capaModeloPOS.Usuario usuario)
	{
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDGeneral();
		boolean respuesta = false;
		try
		{
			Statement stm = con1.createStatement();
			String consulta = "select * from empleado where nombre = '" + usuario.getNombreUsuario() + "' and claverapida = '" + usuario.getContrasena()+"'";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			String nombreLargo = "";
			int idGrupo = 0;
			while(rs.next())
			{
				nombreLargo = rs.getString("nombre_largo");
				idGrupo = rs.getInt("idgrupo");
				usuario.setNombreLargo(nombreLargo);
				usuario.setIdGrupo(idGrupo);
				respuesta = true;
				break;
			}
				rs.close();
				stm.close();
				con1.close();
		}
		catch(Exception e){
			System.out.println(e.toString());
			try
			{
				con1.close();
			}catch(Exception e1)
			{
			}
		}
		return(usuario);
		
	}
	
}
