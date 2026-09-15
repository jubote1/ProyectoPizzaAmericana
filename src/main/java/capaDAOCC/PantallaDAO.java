package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;

import capaModeloCC.MenuModulo;
import capaModeloCC.Pantalla;
import conexionCC.ConexionBaseDatos;

/**
 * Lectura del catalogo de modulos de menu y pantallas. No tiene CRUD de
 * pantallas desde la web (se siembran por migracion, ver sql/
 * seguridad_roles_catalogo.sql); lo que sí se administra desde la web es que
 * pantallas ve cada rol (RolPantallaDAO) y que rol tiene cada usuario
 * (UsuarioRolDAO).
 */
public class PantallaDAO {

	public static ArrayList<MenuModulo> listarModulosConPantallas() {
		Logger logger = Logger.getLogger("log_file");
		LinkedHashMap<Integer, MenuModulo> modulos = new LinkedHashMap<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select m.idmodulo, m.nombre modulo_nombre, m.orden modulo_orden, "
					+ "m.idmodulo_padre, p.idpantalla, p.nombre pantalla_nombre, p.url_html, p.orden pantalla_orden, "
					+ "p.activo pantalla_activo "
					+ "from menu_modulo m left join pantalla p on p.idmodulo = m.idmodulo and p.activo = 'S' "
					+ "where m.activo = 'S' order by m.orden, p.orden";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				int idModulo = rs.getInt("idmodulo");
				MenuModulo modulo = modulos.get(idModulo);
				if (modulo == null) {
					int idModuloPadre = rs.getInt("idmodulo_padre");
					modulo = new MenuModulo(idModulo, rs.getString("modulo_nombre"), rs.getInt("modulo_orden"),
							rs.wasNull() ? null : idModuloPadre);
					modulos.put(idModulo, modulo);
				}
				int idPantalla = rs.getInt("idpantalla");
				if (!rs.wasNull()) {
					Pantalla pantalla = new Pantalla(idPantalla, rs.getString("pantalla_nombre"), idModulo,
							rs.getString("url_html"), rs.getInt("pantalla_orden"), rs.getString("pantalla_activo"));
					pantalla.setNombreModulo(modulo.getNombre());
					modulo.getPantallas().add(pantalla);
				}
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
		return (new ArrayList<>(modulos.values()));
	}

	/** Pantallas (con su modulo) que un rol tiene asignadas. */
	public static ArrayList<Integer> listarIdsPantallaPorRol(int idRol) {
		Logger logger = Logger.getLogger("log_file");
		ArrayList<Integer> ids = new ArrayList<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select idpantalla from rol_pantalla where idrol = " + idRol;
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				ids.add(rs.getInt("idpantalla"));
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

	/**
	 * Si una URL de servlet (ej. "/CRUDTienda") esta mapeada a al menos una
	 * pantalla. Usado por SeguridadFilter: una URL sin mapear pasa siempre,
	 * sea cual sea el modo -"sin mapear" no es lo mismo que "sin permiso".
	 */
	public static boolean existeMapeoParaUrl(String patronUrl) {
		Logger logger = Logger.getLogger("log_file");
		boolean existe = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select 1 from pantalla_servlet where patron_url = '" + patronUrl + "' limit 1";
			ResultSet rs = stm.executeQuery(consulta);
			existe = rs.next();
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
		return (existe);
	}

	/**
	 * Si alguno de los roles del usuario (los que ya vienen cacheados en su
	 * sesion) tiene permiso sobre ALGUNA de las pantallas que respaldan esta
	 * URL. Una misma URL puede servir a mas de una pantalla (ej. GetRoles lo
	 * usan Rol.html, AsignarPantallasRol.html y AsignarRolUsuario.html); por
	 * eso se evalua contra todas las que la usan, no contra una sola resuelta
	 * arbitrariamente -de lo contrario un usuario con acceso a una de esas
	 * pantallas pero no a otra podria quedar bloqueado sin motivo real.
	 */
	public static boolean tienePermisoParaUrl(List<String> nombresRol, String patronUrl) {
		if (nombresRol == null || nombresRol.isEmpty()) {
			return (false);
		}
		Logger logger = Logger.getLogger("log_file");
		boolean permitido = false;
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			StringBuilder listaRoles = new StringBuilder();
			for (int i = 0; i < nombresRol.size(); i++) {
				if (i > 0) {
					listaRoles.append(",");
				}
				listaRoles.append("'").append(nombresRol.get(i).replace("'", "''")).append("'");
			}
			Statement stm = con1.createStatement();
			String consulta = "select 1 from pantalla_servlet ps "
					+ "join rol_pantalla rp on rp.idpantalla = ps.idpantalla "
					+ "join rol r on r.idrol = rp.idrol "
					+ "where ps.patron_url = '" + patronUrl + "' and r.nombre in (" + listaRoles + ") limit 1";
			ResultSet rs = stm.executeQuery(consulta);
			permitido = rs.next();
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
		return (permitido);
	}

}
