package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * A que tienda pertenece un usuario de Bold (bold_sede_tienda, ver
 * sql/bold_eventos_por_tienda.sql). Cada sede tiene su usuario y a ese usuario
 * esta atado su datafono, asi que el usuario que trae el evento dice la tienda.
 */
public class BoldSedeTiendaDAO {

	/**
	 * @return el idtienda de la sede, buscando primero por el id del usuario de
	 *         Bold (que no cambia) y despues por su correo; 0 si esa sede no
	 *         esta registrada o no se pudo consultar.
	 */
	public static int buscarIdTienda(final String boldUserId, final String sellerEmail) {
		final Logger logger = Logger.getLogger("log_file");
		final Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
		if (con1 == null) {
			return 0;
		}
		try {
			final PreparedStatement ps = con1.prepareStatement(
					"select idtienda from bold_sede_tienda where activo = 1 and"
							+ " ((bold_user_id is not null and bold_user_id = ?)"
							+ " or (seller_email is not null and lower(seller_email) = lower(?)))"
							+ " order by (bold_user_id = ?) desc limit 1");
			ps.setString(1, boldUserId == null ? "" : boldUserId);
			ps.setString(2, sellerEmail == null ? "" : sellerEmail);
			ps.setString(3, boldUserId == null ? "" : boldUserId);
			final ResultSet rs = ps.executeQuery();
			int idTienda = 0;
			if (rs.next()) {
				idTienda = rs.getInt(1);
			}
			rs.close();
			ps.close();
			con1.close();
			return idTienda;
		} catch (final Exception ex) {
			logger.error("BoldSedeTiendaDAO.buscarIdTienda: " + ex.toString());
			try {
				con1.close();
			} catch (final Exception e1) {
			}
			return 0;
		}
	}

}
