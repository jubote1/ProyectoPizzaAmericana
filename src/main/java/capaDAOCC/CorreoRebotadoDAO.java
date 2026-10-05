package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Las direcciones que rebotan.
 *
 * POR QUE EXISTE
 *
 * Una direccion muerta se intentaba en CADA campana, para siempre: nada lo
 * aprendia. Se gasta cupo, se gasta tiempo, y sobre todo se gasta la reputacion
 * del dominio, que es lo primero que miran Gmail y Outlook para mandar a spam.
 * Ahi no se pierde el correo de la campana: se pierde el de las facturas.
 *
 * MARCAR, NO BORRAR
 *
 * El correo NO se le quita al cliente. El saldo de puntos se lleva por la
 * cadena del correo, asi que borrarlo le rompe los puntos a esa persona. Esta
 * lista dice "no le escriba a esta direccion", no "esta persona no existe".
 */
public class CorreoRebotadoDAO {

	/**
	 * La condicion que excluye los rebotes al armar un publico.
	 *
	 * Solo bloquea los DUROS. Un rebote blando -buzon lleno, servidor caido- es
	 * pasajero, y sacar por eso a un cliente bueno seria perderlo por un dia
	 * malo.
	 *
	 * Va como texto y no como metodo con parametros porque no lleva ninguno, y
	 * tenerla en un solo sitio es lo que evita que una pantalla filtre y otra no.
	 */
	public static final String NO_REBOTADO =
			" AND NOT EXISTS (SELECT 1 FROM crm.correo_rebotado r"
			+ "                WHERE r.email = TRIM(crm.persona_resumen.email)"
			+ "                  AND r.tipo = 'DURO')";

	/**
	 * Anota un rebote. Si la direccion ya estaba, le suma una vez.
	 *
	 * Un BLANDO no pisa a un DURO: una vez que se sabe que la direccion no
	 * existe, que despues conteste "buzon lleno" no la revive.
	 */
	public static boolean anotar(final Connection cn, final String email, final String tipo,
			final String motivo, final String origen) {
		if (email == null || email.trim().length() == 0) {
			return (false);
		}
		final String elTipo = "BLANDO".equals(tipo) ? "BLANDO" : "DURO";
		//Los valores se vuelven a pasar como parametros en vez de usar
		//VALUES(col): esa forma quedo obsoleta en MySQL 8 y llena el log de
		//advertencias. En el UPDATE, "tipo" a la derecha es el valor que YA
		//estaba en la fila.
		try (PreparedStatement ps = cn.prepareStatement(
				"INSERT INTO crm.correo_rebotado (email, tipo, motivo, origen, veces,"
				+ " primera_vez, ultima_vez) VALUES (?,?,?,?,1,NOW(),NOW())"
				+ " ON DUPLICATE KEY UPDATE veces = veces + 1, ultima_vez = NOW(),"
				+ "   tipo = IF(tipo = 'DURO', 'DURO', ?), motivo = ?")) {
			ps.setString(1, email.trim().toLowerCase());
			ps.setString(2, elTipo);
			ps.setString(3, recortar(motivo, 300));
			ps.setString(4, origen == null ? "BREVO" : origen);
			ps.setString(5, elTipo);
			ps.setString(6, recortar(motivo, 300));
			ps.executeUpdate();
			return (true);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CorreoRebotadoDAO.anotar " + email + ": " + e.toString());
			return (false);
		}
	}

	/** Igual, abriendo y cerrando su propia conexion. */
	public static boolean anotar(final String email, final String tipo, final String motivo,
			final String origen) {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			return (anotar(cn, email, tipo, motivo, origen));
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CorreoRebotadoDAO.anotar: " + e.toString());
			return (false);
		} finally {
			cerrar(cn);
		}
	}

	public static class Resumen {
		public int duros;
		public int blandos;
		public String ultimo = "";
	}

	public static Resumen resumen() {
		final Resumen r = new Resumen();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT SUM(tipo='DURO') AS duros, SUM(tipo='BLANDO') AS blandos,"
					+ " MAX(ultima_vez) AS ultimo FROM crm.correo_rebotado");
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				r.duros = rs.getInt("duros");
				r.blandos = rs.getInt("blandos");
				r.ultimo = rs.getString("ultimo") == null ? "" : rs.getString("ultimo");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CorreoRebotadoDAO.resumen: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	/** Los ultimos rebotes, para mirarlos en pantalla. */
	public static ArrayList<String[]> ultimos(final int cuantos) {
		final ArrayList<String[]> lista = new ArrayList<String[]>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT email, tipo, IFNULL(motivo,'') AS motivo, origen, veces, ultima_vez"
					+ " FROM crm.correo_rebotado ORDER BY ultima_vez DESC LIMIT ?");
			ps.setInt(1, cuantos);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(new String[] { rs.getString("email"), rs.getString("tipo"),
						rs.getString("motivo"), rs.getString("origen"),
						String.valueOf(rs.getInt("veces")), String.valueOf(rs.getString("ultima_vez")) });
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CorreoRebotadoDAO.ultimos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	private static String recortar(final String v, final int largo) {
		if (v == null) {
			return (null);
		}
		return (v.length() > largo ? v.substring(0, largo) : v);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CorreoRebotadoDAO: no cerro la conexion, " + e.toString());
		}
	}
}
