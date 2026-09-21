package capaDAOCC;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * La matriz de permisos rol_pantalla. Se administra reemplazando por
 * completo el set de pantallas de un rol (mas simple que calcular el diff, y
 * el volumen -unas 50 pantallas por rol- es minimo).
 */
public class RolPantallaDAO {

	public static String guardarPantallasRol(int idRol, ArrayList<Integer> idsPantalla) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		String resultado = "exitoso";
		try {
			Statement stm = con1.createStatement();
			stm.executeUpdate("delete from rol_pantalla where idrol = " + idRol);
			for (Integer idPantalla : idsPantalla) {
				stm.executeUpdate(
						"insert into rol_pantalla (idrol, idpantalla) values (" + idRol + ", " + idPantalla + ")");
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
