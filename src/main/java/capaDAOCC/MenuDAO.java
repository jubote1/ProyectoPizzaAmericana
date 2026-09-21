package capaDAOCC;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import capaModeloCC.MenuModulo;
import capaModeloCC.Pantalla;
import conexionCC.ConexionBaseDatos;

/**
 * Arbol de menu (modulo -> pantallas) que un usuario puede ver, segun la
 * union de pantallas de TODOS sus roles (usuario_rol -> rol_pantalla). Esto
 * es lo que reemplaza los 3 archivos Menu.html/MenuAdm.html/MenuPQRS.html
 * estaticos.
 */
public class MenuDAO {

	public static ArrayList<MenuModulo> obtenerMenuParaUsuario(int idUsuario) {
		Logger logger = Logger.getLogger("log_file");
		LinkedHashMap<Integer, MenuModulo> modulos = new LinkedHashMap<>();
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			Statement stm = con1.createStatement();
			String consulta = "select distinct m.idmodulo, m.nombre modulo_nombre, m.orden modulo_orden, "
					+ "p.idpantalla, p.nombre pantalla_nombre, p.url_html, p.orden pantalla_orden "
					+ "from usuario_rol ur "
					+ "join rol r on r.idrol = ur.idrol and r.activo = 'S' "
					+ "join rol_pantalla rp on rp.idrol = ur.idrol "
					+ "join pantalla p on p.idpantalla = rp.idpantalla and p.activo = 'S' "
					+ "join menu_modulo m on m.idmodulo = p.idmodulo and m.activo = 'S' "
					+ "where ur.idusuario = " + idUsuario + " "
					+ "order by m.orden, p.orden";
			logger.info(consulta);
			ResultSet rs = stm.executeQuery(consulta);
			while (rs.next()) {
				int idModulo = rs.getInt("idmodulo");
				MenuModulo modulo = modulos.get(idModulo);
				if (modulo == null) {
					modulo = new MenuModulo(idModulo, rs.getString("modulo_nombre"), rs.getInt("modulo_orden"), null);
					modulos.put(idModulo, modulo);
				}
				Pantalla pantalla = new Pantalla(rs.getInt("idpantalla"), rs.getString("pantalla_nombre"), idModulo,
						rs.getString("url_html"), rs.getInt("pantalla_orden"), "S");
				modulo.getPantallas().add(pantalla);
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

}
