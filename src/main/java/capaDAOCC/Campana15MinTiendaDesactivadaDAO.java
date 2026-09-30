package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Desactivacion de la campana 15 minutos por tienda, por dia (horno danado,
 * falta de personal, etc.). El POS es quien decide -- este registro es solo
 * para visibilidad central, best-effort desde el POS. Al ser por fecha, se
 * reactiva sola al dia siguiente: no hace falta ningun proceso que la
 * "vuelva a prender".
 */
public class Campana15MinTiendaDesactivadaDAO {

	public static void registrar(int idTienda, String fecha, String motivo, String usuarioAutoriza,
			String usuarioDesactiva) {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			PreparedStatement pst = con1.prepareStatement("insert into campana_15min_tienda_desactivada_dia"
					+ " (idtienda, fecha, motivo, usuario_autoriza, usuario_desactiva) values (?,?,?,?,?)"
					+ " on duplicate key update motivo = values(motivo), usuario_autoriza = values(usuario_autoriza),"
					+ " usuario_desactiva = values(usuario_desactiva)");
			pst.setInt(1, idTienda);
			pst.setString(2, fecha);
			pst.setString(3, motivo == null ? "" : motivo);
			pst.setString(4, usuarioAutoriza == null ? "" : usuarioAutoriza);
			pst.setString(5, usuarioDesactiva == null ? "" : usuarioDesactiva);
			pst.executeUpdate();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("registrar: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
	}

	public static class Desactivacion {
		public int idTienda = 0;

		public String nombreTienda = "";

		public String fecha = "";

		public String motivo = "";

		public String usuarioAutoriza = "";

		public String usuarioDesactiva = "";

		public String fechaHora = "";
	}

	/** Para la pantalla central de visibilidad: que tiendas estan desactivadas hoy. */
	public static ArrayList<Desactivacion> obtenerDeHoy() {
		Logger logger = Logger.getLogger("log_file");
		ConexionBaseDatos con = new ConexionBaseDatos();
		Connection con1 = con.obtenerConexionBDPrincipal();
		ArrayList<Desactivacion> lista = new ArrayList<>();
		try {
			PreparedStatement pst = con1.prepareStatement("select d.*, t.nombre as nombretienda"
					+ " from campana_15min_tienda_desactivada_dia d left join tienda t on t.idtienda = d.idtienda"
					+ " where d.fecha = curdate() order by t.nombre");
			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				Desactivacion d = new Desactivacion();
				d.idTienda = rs.getInt("idtienda");
				d.nombreTienda = rs.getString("nombretienda") == null ? "" : rs.getString("nombretienda");
				d.fecha = rs.getString("fecha");
				d.motivo = rs.getString("motivo") == null ? "" : rs.getString("motivo");
				d.usuarioAutoriza = rs.getString("usuario_autoriza") == null ? "" : rs.getString("usuario_autoriza");
				d.usuarioDesactiva = rs.getString("usuario_desactiva") == null ? ""
						: rs.getString("usuario_desactiva");
				d.fechaHora = rs.getString("fecha_hora") == null ? "" : rs.getString("fecha_hora");
				lista.add(d);
			}
			rs.close();
			pst.close();
			con1.close();
		} catch (Exception e) {
			logger.error("obtenerDeHoy: " + e.toString());
			try {
				con1.close();
			} catch (Exception e1) {
			}
		}
		return (lista);
	}
}
