package capaDAOCC;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import capaModeloCC.Usuario;
import conexionCC.ConexionBaseDatos;
/**
 * Clase que se encarga de implementar toda la interacci�n con la base de datos para la entidad Usuario.
 * @author JuanDavid
 *
 */
public class UsuarioDAO {

	/**
	 * M�todo que se encarga de validar la existencia y de un usuario y su contrase�a en la base de datos.
	 * @param usuario Se recibe como par�metro un objeto MOdelo Usuario, el cual trae la informaci�n base para la validaci�n,
	 * autenticaci�n del usuario.
	 * @return Se retorna un valor booleano que indica si el proceso de autenticaci�n es satifactorio o no.
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
	 * M�todo que se encarga de validar si un usuario existe o no en la base de datos
	 * @param usuario Recibe como par�metro un objeto Modelo Usuario con base en el cual se realiza la consulta.
	 * @return Se retorna un valor booleano con base en el cual se realiza la validaci�n del usuario en base de datos
	 * 
	 */
	public static String validarAutenticacion(Usuario usuario) {
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection con1 = con.obtenerConexionBDPrincipal();
	    String resultado = "";
	    Statement stm = null;
	    ResultSet rs = null;

	    try {
	        stm = con1.createStatement();
	        String consulta = "SELECT administrador, nombre_largo, plataforma FROM usuario WHERE nombre = '" + usuario.getNombreUsuario() + "'";
	        rs = stm.executeQuery(consulta);

	        while (rs.next()) {
	            try {
	                resultado = rs.getString("administrador");
	                usuario.setNombreLargo(rs.getString("nombre_largo"));
	                usuario.setPlataforma(rs.getString("plataforma"));
	            } catch (Exception e) {
	                System.out.println("Error extrayendo datos del ResultSet: " + e.getMessage());
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("Error al ejecutar la consulta: " + e.getMessage());
	    } finally {
	        try {
	            if (rs != null) rs.close();
	            if (stm != null) stm.close();
	            if (con1 != null) con1.close();
	        } catch (Exception e) {
	            System.out.println("Error cerrando recursos: " + e.getMessage());
	        }
	    }

	    return resultado;
	}

	/**
	 * M�todo que se encarga de revisar si el usuario existe y retornar el nombre completo y el idGrupo
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
	
	public static List<Usuario> obtenerUsuarioActivo() {
		List<Usuario> ListaUsuario = new ArrayList<>();
	    Logger logger = Logger.getLogger("log_file");
	    ConexionBaseDatos con = new ConexionBaseDatos();
	    Connection conn = con.obtenerConexionBDPrincipal();

	    String consulta = "SELECT id, nombre, nombre_largo, plataforma ,activo FROM usuario WHERE activo = 1";

	    try (
	        PreparedStatement stmt = conn.prepareStatement(consulta);
	        ResultSet rs = stmt.executeQuery()
	    ) {
	
	    	while (rs.next()) {
	        	Usuario usuario = new Usuario();
	            usuario = new Usuario();
	            usuario.setId(rs.getInt("id"));
	            usuario.setNombreUsuario(rs.getString("nombre"));
	            usuario.setNombreLargo(rs.getString("nombre_largo"));
	            usuario.setPlataforma(rs.getString("plataforma"));
	            usuario.setActivo(rs.getBoolean("activo"));

	            ListaUsuario.add(usuario);
	        }
	    } catch (SQLException e) {
	        logger.info("Error al consultar usuarios activos: " + e.getMessage());
	    } finally {
	        try {
	            conn.close();
	        } catch (SQLException e) {
	            logger.info("Error al cerrar conexión: " + e.getMessage());
	        }
	    }
        
	    return ListaUsuario;
	    
	}

	
}
