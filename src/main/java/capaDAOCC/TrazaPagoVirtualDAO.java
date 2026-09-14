package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import capaModeloCC.TrazaPagoVirtual;
import conexionCC.ConexionBaseDatos;

/**
 * La historia completa de un pago virtual, en orden.
 *
 * Se arma con tres consultas y no con un UNION a proposito: pedido_gestion_link
 * solo tiene fecha y usuario despues de la migracion
 * 2026_09_12_02_gestion_link_trazabilidad.sql, y si esa parte falla porque la
 * migracion todavia no se ha corrido, lo demas tiene que seguir apareciendo. Un
 * UNION se caeria completo y la pantalla quedaria en blanco justo cuando mas se
 * necesita.
 *
 * Las tres consultas son por indice y devuelven pocas filas, asi que salen mas
 * barato que la lectura de una sola pantalla del listado.
 */
public class TrazaPagoVirtualDAO {

	/**
	 * Las fechas llegan como texto yyyy-MM-dd HH:mm:ss, que ordenado
	 * alfabeticamente queda ordenado cronologicamente. Lo que no tiene fecha -las
	 * gestiones anteriores a la migracion- se va al final en vez de al principio,
	 * que es donde estorbaria.
	 */
	private static final Comparator<TrazaPagoVirtual> POR_FECHA = new Comparator<TrazaPagoVirtual>() {
		public int compare(final TrazaPagoVirtual uno, final TrazaPagoVirtual otro) {
			final String a = (uno.getFechaHora() == null || uno.getFechaHora().trim().length() == 0)
					? "9999" : uno.getFechaHora();
			final String b = (otro.getFechaHora() == null || otro.getFechaHora().trim().length() == 0)
					? "9999" : otro.getFechaHora();
			return (a.compareTo(b));
		}
	};

	/**
	 * @param idPedido pedido del que se quiere la historia
	 * @param idLink   link de pago; si viene vacio no se consulta a Wompi
	 */
	public static ArrayList<TrazaPagoVirtual> obtenerTraza(final int idPedido, final String idLink) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<TrazaPagoVirtual> traza = new ArrayList<TrazaPagoVirtual>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			agregarAvisos(con1, idPedido, traza);
			agregarEventosWompi(con1, idLink, traza);
			agregarGestiones(con1, idPedido, traza);
		} catch (final Exception e) {
			logger.error("TrazaPagoVirtualDAO.obtenerTraza: " + e.toString());
		} finally {
			try {
				con1.close();
			} catch (final Exception e1) {
			}
		}
		Collections.sort(traza, POR_FECHA);
		return (traza);
	}

	/** Lo que se le mando al cliente: el link, el recordatorio y la cancelacion. */
	private static void agregarAvisos(final Connection con1, final int idPedido,
			final ArrayList<TrazaPagoVirtual> traza) {
		final Logger logger = Logger.getLogger("log_file");
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = con1.prepareStatement("SELECT fecha_hora, IFNULL(observacion,'') AS observacion,"
					+ " IFNULL(email,'') AS email, IFNULL(telefono_celular,'') AS celular"
					+ " FROM pedido_pago_virtual WHERE idpedido = ? ORDER BY fecha_hora");
			pst.setInt(1, idPedido);
			rs = pst.executeQuery();
			while (rs.next()) {
				final StringBuilder detalle = new StringBuilder(rs.getString("observacion"));
				final String celular = rs.getString("celular");
				final String email = rs.getString("email");
				//A quien se le mando importa tanto como que se mando: cuando el cliente
				//dice que no le llego, lo primero que hay que mirar es si el correo o el
				//celular que teniamos eran los suyos.
				if (celular.length() > 0 || email.length() > 0) {
					detalle.append(" [").append(celular);
					if (email.length() > 0) {
						detalle.append(celular.length() > 0 ? " / " : "").append(email);
					}
					detalle.append("]");
				}
				traza.add(new TrazaPagoVirtual("AVISO", rs.getString("fecha_hora"), detalle.toString(), ""));
			}
		} catch (final Exception e) {
			logger.error("TrazaPagoVirtualDAO.agregarAvisos: " + e.toString());
		} finally {
			cerrar(rs, pst);
		}
	}

	/** Lo que contesto la pasarela: los intentos de pago y como terminaron. */
	private static void agregarEventosWompi(final Connection con1, final String idLink,
			final ArrayList<TrazaPagoVirtual> traza) {
		final Logger logger = Logger.getLogger("log_file");
		if (idLink == null || idLink.trim().length() == 0) {
			return;
		}
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = con1.prepareStatement("SELECT fecha_hora, evento, estado FROM log_evento_wompi"
					+ " WHERE id_link = ? ORDER BY fecha_hora, idlog_evento_wompi");
			pst.setString(1, idLink);
			rs = pst.executeQuery();
			while (rs.next()) {
				traza.add(new TrazaPagoVirtual("WOMPI", rs.getString("fecha_hora"), rs.getString("evento"),
						rs.getString("estado")));
			}
		} catch (final Exception e) {
			logger.error("TrazaPagoVirtualDAO.agregarEventosWompi: " + e.toString());
		} finally {
			cerrar(rs, pst);
		}
	}

	/** Lo que hizo la persona que lo gestiono. */
	private static void agregarGestiones(final Connection con1, final int idPedido,
			final ArrayList<TrazaPagoVirtual> traza) {
		final Logger logger = Logger.getLogger("log_file");
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			pst = con1.prepareStatement("SELECT IFNULL(fecha_hora,'') AS fecha_hora,"
					+ " IFNULL(usuario,'') AS usuario, IFNULL(observacion,'') AS observacion"
					+ " FROM pedido_gestion_link WHERE idpedido = ? ORDER BY idgestion");
			pst.setInt(1, idPedido);
			rs = pst.executeQuery();
			while (rs.next()) {
				final String usuario = rs.getString("usuario");
				final String detalle = (usuario.length() > 0 ? usuario + ": " : "") + rs.getString("observacion");
				traza.add(new TrazaPagoVirtual("GESTION", rs.getString("fecha_hora"), detalle, ""));
			}
		} catch (final Exception e) {
			//Si la migracion todavia no se ha corrido, la consulta falla por las
			//columnas que faltan. Se anota y la pantalla sigue mostrando el resto.
			logger.error("TrazaPagoVirtualDAO.agregarGestiones: " + e.toString());
		} finally {
			cerrar(rs, pst);
		}
	}

	private static void cerrar(final ResultSet rs, final PreparedStatement pst) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (final Exception e) {
		}
		try {
			if (pst != null) {
				pst.close();
			}
		} catch (final Exception e) {
		}
	}
}
