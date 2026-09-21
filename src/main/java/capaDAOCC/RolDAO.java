package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.Rol;
import conexionCC.ConexionBaseDatos;

/**
 * CRUD del catalogo de roles de seguridad. Mismo patron que
 * capaDAOCC/EspecialidadDAO.java.
 */
public class RolDAO {

	public static int insertarRol(Rol rol) {
		Logger logger = Logger.getLogger("log_file");
		int idRolIns = 0;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String insert = "insert into rol (nombre, descripcion) values ('" + rol.getNombre() + "', '"
					+ rol.getDescripcion() + "')";
			logger.info(insert);
			stm.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.getGeneratedKeys();
			if (rs.next()) {
				idRolIns = rs.getInt(1);
			}
			stm.close();
			con1.close();
		} catch (Exception e) {
			logger.error(e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
			return (0);
		}
		return (idRolIns);
	}

	public static String editarRol(Rol rol) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			Statement stm = con1.createStatement();
			String update = "update rol set nombre = '" + rol.getNombre() + "', descripcion = '"
					+ rol.getDescripcion() + "' where idrol = " + rol.getIdRol();
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
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

	/** Borrado logico: un rol desactivado deja de ofrecerse para asignar, pero no se borran sus permisos historicos. */
	public static String eliminarRol(int idRol) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "";
		try {
			Statement stm = con1.createStatement();
			String update = "update rol set activo = 'N' where idrol = " + idRol;
			logger.info(update);
			stm.executeUpdate(update);
			resultado = "exitoso";
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

	public static Rol retornarRol(int idRol) {
		ArrayList<Rol> todos = listar("where idrol = " + idRol);
		return (todos.isEmpty() ? new Rol() : todos.get(0));
	}

	public static ArrayList<Rol> listarRoles() {
		return (listar(""));
	}

	public static ArrayList<Rol> listarRolesActivos() {
		return (listar("where activo = 'S'"));
	}

	private static ArrayList<Rol> listar(String filtro) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Rol> roles = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select idrol, nombre, descripcion, activo from rol " + filtro + " order by nombre";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				roles.add(new Rol(rs.getInt("idrol"), rs.getString("nombre"), rs.getString("descripcion"),
						rs.getString("activo")));
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

}
