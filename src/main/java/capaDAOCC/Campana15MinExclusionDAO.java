package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Nombres de especialidad/producto excluidos de la campana 15 minutos aunque
 * no sumen valor adicional en eleccion_forzada (ej. "The Works", "Con Todo":
 * tardan mas en prepararse, no porque cuesten mas). El POS hace el match por
 * nombre, case-insensitive, contra su propio catalogo local.
 */
public class Campana15MinExclusionDAO {

	public static class Exclusion {
		public int idExclusion = 0;

		public int idCampana = 0;

		public String nombreProducto = "";
	}

	public static ArrayList<Exclusion> obtenerPorCampana(int idCampana) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Exclusion> lista = new ArrayList<>();
		try {
			PreparedStatement pst = con1.prepareStatement(
					"select * from campana_15min_exclusion where idcampana = ? order by nombre_producto");
			pst.setInt(1, idCampana);
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				Exclusion e = new Exclusion();
				e.idExclusion = rs.getInt("idexclusion");
				e.idCampana = rs.getInt("idcampana");
				e.nombreProducto = rs.getString("nombre_producto") == null ? "" : rs.getString("nombre_producto");
				lista.add(e);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerPorCampana: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}

	public static int insertar(int idCampana, String nombreProducto) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		int idExclusion = 0;
		try {
			PreparedStatement pst = con1.prepareStatement(
					"insert into campana_15min_exclusion (idcampana, nombre_producto) values (?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pst.setInt(1, idCampana);
			pst.setString(2, nombreProducto == null ? "" : nombreProducto.trim().toUpperCase());
			pst.executeUpdate();
			ResultSet rs = pst.getGeneratedKeys();
			if (rs.next()) {
				idExclusion = rs.getInt(1);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("insertar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (idExclusion);
	}

	public static void eliminar(int idExclusion) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1
					.prepareStatement("delete from campana_15min_exclusion where idexclusion = ?");
			pst.setInt(1, idExclusion);
			pst.executeUpdate();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("eliminar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
	}
}
