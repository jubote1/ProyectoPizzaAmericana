package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Procesa y registra las novedades reportadas por el webhook de Brevo:
 * desuscripciones voluntarias, quejas de spam y rebotes (duros y blandos).
 */
public class BrevoWebhookDAO {

	private static final Logger LOGGER = Logger.getLogger("log_file");

	public static class LogBrevo {
		public long idlog;
		public String fechaHora;
		public String evento;
		public String email;
		public String motivo;
		public String resultado;
		public String ipOrigen;
	}

	/**
	 * Procesa un evento individual recibido desde el webhook de Brevo.
	 *
	 * @return resultado textual de la accion aplicada (ej: BAJA_APLICADA, REBOTE_DURO, etc.)
	 */
	public static String procesarEvento(final Connection cn, final String eventoRaw,
			final String emailRaw, final String motivoRaw, final String ipOrigen,
			final String jsonRaw) {

		if (emailRaw == null || emailRaw.trim().isEmpty()) {
			registrarLog(cn, eventoRaw, "", motivoRaw, "IGNORADO_SIN_EMAIL", ipOrigen, jsonRaw);
			return "IGNORADO_SIN_EMAIL";
		}

		final String email = emailRaw.trim().toLowerCase();
		final String evento = eventoRaw == null ? "DESCONOCIDO" : eventoRaw.trim().toLowerCase();
		final String motivo = motivoRaw == null ? "" : motivoRaw.trim();
		String resultado = "IGNORADO";

		try {
			if (esEventoBaja(evento)) {
				// 1. DESUSCRIPCION VOLUNTARIA
				aplicarBajaCliente(cn, email, "[Brevo Webhook] Baja voluntaria (desuscripcion)");
				aplicarBajaPersona(cn, email);
				resultado = "BAJA_APLICADA";

			} else if (esEventoSpam(evento)) {
				// 2. QUEJA DE SPAM / COMPLAINT
				aplicarBajaCliente(cn, email, "[Brevo Webhook] Marco spam / complaint");
				aplicarBajaPersona(cn, email);
				resultado = "SPAM_APLICADO";

			} else if (esEventoReboteDuro(evento)) {
				// 3. REBOTE DURO / DIRECCION INVALIDA
				CorreoRebotadoDAO.anotar(cn, email, "DURO", motivo.isEmpty() ? "Rebote duro informado por Brevo" : motivo, "BREVO_WEBHOOK");
				resultado = "REBOTE_DURO_REGISTRADO";

			} else if (esEventoReboteBlando(evento)) {
				// 4. REBOTE BLANDO / BUZON LLENO
				CorreoRebotadoDAO.anotar(cn, email, "BLANDO", motivo.isEmpty() ? "Rebote blando informado por Brevo" : motivo, "BREVO_WEBHOOK");
				resultado = "REBOTE_BLANDO_REGISTRADO";

			} else if ("blocked".equals(evento)) {
				// 5. BLOQUEADO POR BREVO
				CorreoRebotadoDAO.anotar(cn, email, "DURO", motivo.isEmpty() ? "Bloqueado en Brevo" : motivo, "BREVO_WEBHOOK");
				resultado = "BLOQUEADO_REGISTRADO";

			} else {
				// Evento informativo (delivered, opened, click, etc.)
				resultado = "INFORMATIVO_SIN_ACCION";
			}

		} catch (final Exception e) {
			LOGGER.error("BrevoWebhookDAO.procesarEvento (" + evento + ", " + email + "): " + e.toString());
			resultado = "ERROR: " + e.getMessage();
		}

		// Registrar en la tabla auditable crm.log_webhook_brevo
		registrarLog(cn, evento, email, motivo, resultado, ipOrigen, jsonRaw);
		return resultado;
	}

	private static boolean esEventoBaja(final String ev) {
		return "unsubscribed".equals(ev) || "unsubscribe".equals(ev)
				|| "unsubscription".equals(ev) || "contact_deleted".equals(ev);
	}

	private static boolean esEventoSpam(final String ev) {
		return "spam".equals(ev) || "complaint".equals(ev);
	}

	private static boolean esEventoReboteDuro(final String ev) {
		return "hard_bounce".equals(ev) || "hardbounce".equals(ev)
				|| "invalid_email".equals(ev);
	}

	private static boolean esEventoReboteBlando(final String ev) {
		return "soft_bounce".equals(ev) || "softbounce".equals(ev);
	}

	/**
	 * Marca envio_publicidad='N' y politica_datos='N' en todas las filas de cliente con ese correo.
	 */
	private static int aplicarBajaCliente(final Connection cn, final String email, final String nota)
			throws Exception {
		final String sql = "UPDATE pizzaamericana.cliente\n"
				+ "   SET envio_publicidad = 'N',\n"
				+ "       politica_datos   = 'N',\n"
				+ "       observacion      = CONCAT(IFNULL(observacion,''), ' | ', ?, ' el ', CURRENT_DATE)\n"
				+ " WHERE LOWER(TRIM(email)) = ?\n"
				+ "   AND (IFNULL(envio_publicidad,'S') <> 'N' OR IFNULL(politica_datos,'S') <> 'N')";
		try (PreparedStatement ps = cn.prepareStatement(sql)) {
			ps.setString(1, nota);
			ps.setString(2, email);
			return ps.executeUpdate();
		}
	}

	/**
	 * Sincroniza politica_datos='N' en crm.persona y crm.persona_resumen para las personas de ese correo.
	 */
	private static void aplicarBajaPersona(final Connection cn, final String email) throws Exception {
		final String sql1 = "UPDATE crm.persona p\n"
				+ "   JOIN pizzaamericana.cliente c ON p.idpersona = c.idpersona\n"
				+ "    SET p.politica_datos = 'N'\n"
				+ "  WHERE LOWER(TRIM(c.email)) = ?\n"
				+ "    AND IFNULL(p.politica_datos,'S') <> 'N'";
		try (PreparedStatement ps = cn.prepareStatement(sql1)) {
			ps.setString(1, email);
			ps.executeUpdate();
		}

		final String sql2 = "UPDATE crm.persona_resumen pr\n"
				+ "   JOIN pizzaamericana.cliente c ON pr.idpersona = c.idpersona\n"
				+ "    SET pr.politica_datos = 'N'\n"
				+ "  WHERE LOWER(TRIM(c.email)) = ?\n"
				+ "    AND IFNULL(pr.politica_datos,'S') <> 'N'";
		try (PreparedStatement ps = cn.prepareStatement(sql2)) {
			ps.setString(1, email);
			ps.executeUpdate();
		}
	}

	/**
	 * Registra el evento en la tabla auditable crm.log_webhook_brevo.
	 */
	public static void registrarLog(final Connection cn, final String evento, final String email,
			final String motivo, final String resultado, final String ipOrigen, final String jsonRaw) {
		final String sql = "INSERT INTO crm.log_webhook_brevo\n"
				+ " (evento, email, motivo, resultado, ip_origen, json_evento)\n"
				+ " VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = cn.prepareStatement(sql)) {
			ps.setString(1, recortar(evento, 50));
			ps.setString(2, recortar(email, 150));
			ps.setString(3, recortar(motivo, 300));
			ps.setString(4, recortar(resultado, 100));
			ps.setString(5, recortar(ipOrigen, 60));
			ps.setString(6, jsonRaw);
			ps.executeUpdate();
		} catch (final Exception e) {
			LOGGER.error("BrevoWebhookDAO.registrarLog: " + e.toString());
		}
	}

	/**
	 * Consulta los ultimos registros del log para auditoria y verificacion.
	 */
	public static List<LogBrevo> consultarUltimosLogs(final int limite) {
		final List<LogBrevo> lista = new ArrayList<>();
		final int lim = limite <= 0 ? 20 : Math.min(limite, 100);
		final String sql = "SELECT idlog, fecha_hora, evento, email, motivo, resultado, ip_origen\n"
				+ "  FROM crm.log_webhook_brevo\n"
				+ " ORDER BY idlog DESC LIMIT " + lim;

		try (Connection cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			 Statement s = cn.createStatement();
			 ResultSet rs = s.executeQuery(sql)) {

			while (rs.next()) {
				final LogBrevo log = new LogBrevo();
				log.idlog = rs.getLong("idlog");
				log.fechaHora = rs.getString("fecha_hora");
				log.evento = rs.getString("evento");
				log.email = rs.getString("email");
				log.motivo = rs.getString("motivo");
				log.resultado = rs.getString("resultado");
				log.ipOrigen = rs.getString("ip_origen");
				lista.add(log);
			}
		} catch (final Exception e) {
			LOGGER.error("BrevoWebhookDAO.consultarUltimosLogs: " + e.toString());
		}
		return lista;
	}

	private static String recortar(final String texto, final int max) {
		if (texto == null) return "";
		return texto.length() <= max ? texto : texto.substring(0, max);
	}
}
