package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Que rol(es) tiene cada usuario. N:M aunque hoy en la practica sea 1:1, para
 * no tener que rehacer el esquema si algun dia un usuario necesita mas de un
 * rol.
 */
public class UsuarioRolDAO {

	/** Nombres de rol de un usuario, para dejarlos en el objeto Usuario de sesion tras el login. */
	public static ArrayList<String> listarNombresRolPorUsuario(int idUsuario) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<String> roles = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select r.nombre from usuario_rol ur join rol r on r.idrol = ur.idrol "
					+ "where ur.idusuario = " + idUsuario + " and r.activo = 'S'";
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				roles.add(rs.getString("nombre"));
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (roles);
	}

	public static ArrayList<Integer> listarIdsRolPorUsuario(int idUsuario) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Integer> ids = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select idrol from usuario_rol where idusuario = " + idUsuario;
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				ids.add(rs.getInt("idrol"));
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (ids);
	}

	public static String guardarRolesUsuario(int idUsuario, ArrayList<Integer> idsRol) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "exitoso";
		try {
			Statement stm = con1.createStatement();
			stm.executeUpdate("delete from usuario_rol where idusuario = " + idUsuario);
			for (Integer idRol : idsRol) {
				stm.executeUpdate(
						"insert into usuario_rol (idusuario, idrol) values (" + idUsuario + ", " + idRol + ")");
			}
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			resultado = "error";
		}
		return (resultado);
	}

}
