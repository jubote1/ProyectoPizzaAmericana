package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Deja constancia de lo que se le envia a cada persona por Brevo.
 *
 * Hasta el 2026-09-22 por Brevo salian correos y WhatsApp todos los dias sin
 * guardar nada: ni a quien, ni que plantilla, ni cuando, ni si Brevo lo acepto.
 * Si un cliente reclama que le escribimos de mas no habia como responderle, y
 * medir una campana era imposible, porque comparar a los que recibieron contra
 * los que no exige saber quienes recibieron.
 *
 * REGISTRAR NUNCA PUEDE TUMBAR UN ENVIO
 *
 * Todo lo de aqui se traga sus errores y deja la linea en el log. Perder la
 * evidencia de un envio es malo; no mandarle la promocion a tres mil personas
 * porque fallo el registro es mucho peor, y ademas Brevo ya la mando: el
 * registro ocurre despues.
 */
public class EnvioBrevoDAO {

	/** Un destinatario de un envio. */
	public static class Destino {
		public String destino = "";
		public String nombre = "";
	}

	/**
	 * Guarda un envio, una fila por destinatario.
	 *
	 * @param canal       'C' correo o 'W' whatsapp
	 * @param destinos    a quienes se les mando
	 * @param resultado   "OK" o "ERROR"
	 * @param detalle     lo que respondio Brevo cuando fallo, o null
	 * @return cuantas filas quedaron
	 */
	public static int registrar(final String canal, final ArrayList<Destino> destinos,
			final int idPlantilla, final String asunto, final String usuario,
			final String resultado, final String detalle) {
		if (destinos == null || destinos.isEmpty()) {
			return (0);
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		int filas = 0;
		try {
			cn = con.obtenerConexionBDPrincipal();

			/*
			 * La persona se resuelve AQUI y no al consultar.
			 *
			 * Por el correo o por el celular, segun el canal. Asi la vista 360
			 * responde de inmediato, y si manana el maestro une dos personas, lo
			 * que se envio hoy siguio yendo a quien fue.
			 *
			 * Va con LIMIT 1 porque un correo puede estar en varias personas
			 * -familias, correos compartidos-. Colgarlo de la primera es
			 * impreciso, pero dejarlo en NULL por esa duda seria peor: se
			 * perderia el envio de vista en el 360 de todas ellas.
			 */
			final String busca = "W".equalsIgnoreCase(canal)
					? "SELECT idpersona FROM crm.persona WHERE celular_norm = ? LIMIT 1"
					: "SELECT idpersona FROM crm.persona WHERE email = ? LIMIT 1";
			final PreparedStatement psBusca = cn.prepareStatement(busca);

			final PreparedStatement psIns = cn.prepareStatement(
					"INSERT INTO crm.envio_brevo"
					+ " (idpersona, canal, destino, nombre, idplantilla, asunto,"
					+ "  resultado, detalle, usuario, enviado_en)"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())");

			for (int i = 0; i < destinos.size(); i++) {
				final Destino d = destinos.get(i);
				if (d == null || d.destino == null || d.destino.trim().length() == 0) {
					continue;
				}
				Long idPersona = null;
				try {
					psBusca.setString(1, d.destino.trim());
					final ResultSet rs = psBusca.executeQuery();
					if (rs.next()) {
						idPersona = Long.valueOf(rs.getLong(1));
					}
					rs.close();
				} catch (final Exception e) {
					//Sin persona igual se guarda: el envio ocurrio.
					idPersona = null;
				}

				if (idPersona == null) {
					psIns.setNull(1, java.sql.Types.BIGINT);
				} else {
					psIns.setLong(1, idPersona.longValue());
				}
				psIns.setString(2, "W".equalsIgnoreCase(canal) ? "W" : "C");
				psIns.setString(3, recortar(d.destino, 100));
				psIns.setString(4, recortar(d.nombre, 120));
				psIns.setInt(5, idPlantilla);
				psIns.setString(6, recortar(asunto, 250));
				psIns.setString(7, "ERROR".equalsIgnoreCase(resultado) ? "ERROR" : "OK");
				psIns.setString(8, recortar(detalle, 500));
				psIns.setString(9, recortar(usuario, 50));
				psIns.addBatch();
				filas++;
			}
			psIns.executeBatch();
			psIns.close();
			psBusca.close();
			Logger.getLogger("log_file").info("Envio Brevo registrado: canal=" + canal
					+ " destinatarios=" + filas + " plantilla=" + idPlantilla
					+ " resultado=" + resultado + " usuario=" + usuario);
		} catch (final Exception e) {
			//Ver el comentario de la clase: esto no puede tumbar un envio.
			Logger.getLogger("log_file").error("EnvioBrevoDAO.registrar: " + e.toString());
		} finally {
			try {
				if (cn != null) {
					cn.close();
				}
			} catch (final Exception e) {
				Logger.getLogger("log_file").error("EnvioBrevoDAO: no cerro, " + e.toString());
			}
		}
		return (filas);
	}

	/** Lo que se le ha enviado a una persona, lo mas reciente primero. */
	public static class Envio {
		public String canal = "";
		public String destino = "";
		public String asunto = "";
		public int idPlantilla;
		public String plantilla = "";
		public String resultado = "";
		public String cuando = "";
	}

	/** Tope de envios que se traen al 360. Mas que esto no se alcanza a mirar. */
	private static final int TOPE = 30;

	public static ArrayList<Envio> deLaPersona(final long idPersona) {
		final ArrayList<Envio> lista = new ArrayList<Envio>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			//El nombre de la plantilla sale de plantilla_brevo, que es el
			//catalogo vivo. Con LEFT JOIN porque una plantilla puede borrarse
			//de ahi y el envio sigue habiendo ocurrido.
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT e.canal, e.destino, IFNULL(e.asunto,'') AS asunto,"
					+ " e.idplantilla, IFNULL(p.nombre,'') AS plantilla,"
					+ " e.resultado, e.enviado_en"
					+ " FROM crm.envio_brevo e"
					+ " LEFT JOIN pizzaamericana.plantilla_brevo p ON p.idplantilla = e.idplantilla"
					+ " WHERE e.idpersona = ? ORDER BY e.enviado_en DESC LIMIT " + TOPE);
			ps.setLong(1, idPersona);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Envio x = new Envio();
				x.canal = texto(rs.getString("canal"));
				x.destino = texto(rs.getString("destino"));
				x.asunto = texto(rs.getString("asunto"));
				x.idPlantilla = rs.getInt("idplantilla");
				x.plantilla = texto(rs.getString("plantilla"));
				x.resultado = texto(rs.getString("resultado"));
				x.cuando = texto(rs.getString("enviado_en"));
				lista.add(x);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnvioBrevoDAO.deLaPersona: " + e.toString());
		} finally {
			try {
				if (cn != null) {
					cn.close();
				}
			} catch (final Exception e) {
			}
		}
		return (lista);
	}

	private static String recortar(final String s, final int largo) {
		if (s == null) {
			return (null);
		}
		final String t = s.trim();
		return (t.length() <= largo ? t : t.substring(0, largo));
	}

	private static String texto(final String s) {
		return (s == null ? "" : s);
	}
}
