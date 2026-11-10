package capaServicioCC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaDAOCC.BrevoWebhookDAO;
import conexionCC.ConexionBaseDatos;

/**
 * Endpoint para recibir las notificaciones HTTP (Webhooks) de Brevo en tiempo real.
 *
 * Procesa:
 * - Desuscripciones voluntarias (unsubscribed, unsubscribe) -> Marca envio_publicidad='N' y politica_datos='N'.
 * - Quejas de spam (spam, complaint) -> Marca envio_publicidad='N' y politica_datos='N'.
 * - Rebotes duros / correos invalidos (hard_bounce, invalid_email, blocked) -> Registra en crm.correo_rebotado (DURO).
 * - Rebotes blandos (soft_bounce) -> Registra en crm.correo_rebotado (BLANDO).
 *
 * Ruta: /api/brevo/webhook   Metodos: GET (salud/auditoria), POST (recepcion de eventos).
 */
@WebServlet("/api/brevo/webhook")
public class BrevoWebhook extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger("log_file");
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final int MAXIMO_CUERPO = 1024 * 1024; // 1 MB

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final String accion = request.getParameter("accion");
		if ("logs".equalsIgnoreCase(accion)) {
			// Consulta de auditoria de los ultimos 20 eventos procesados
			final List<BrevoWebhookDAO.LogBrevo> logs = BrevoWebhookDAO.consultarUltimosLogs(20);
			response.getWriter().write(MAPPER.writeValueAsString(logs));
			return;
		}

		// Healthcheck basico
		response.getWriter().write("{\"ok\":true,\"servicio\":\"BrevoWebhook\",\"estado\":\"ACTIVO\","
				+ "\"version\":\"1.0\",\"descripcion\":\"Receptor de bajas, quejas y rebotes de Brevo\"}");
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final byte[] cuerpoBytes = leerCuerpo(request.getInputStream());
		if (cuerpoBytes == null || cuerpoBytes.length == 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"ok\":false,\"message\":\"Cuerpo vacio\"}");
			return;
		}

		final String jsonRaw = new String(cuerpoBytes, StandardCharsets.UTF_8);
		final String ipOrigen = obtenerIp(request);

		int procesados = 0;
		Connection cn = null;
		try {
			final JsonNode raiz = MAPPER.readTree(jsonRaw);
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			if (raiz.isArray()) {
				// Brevo puede enviar un lote de eventos en un array JSON
				for (final JsonNode nodo : raiz) {
					procesarNodo(cn, nodo, ipOrigen);
					procesados++;
				}
			} else if (raiz.isObject()) {
				// Evento individual
				procesarNodo(cn, raiz, ipOrigen);
				procesados++;
			} else {
				LOGGER.warn("BrevoWebhook: JSON recibido no es objeto ni array: " + jsonRaw);
			}

			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("{\"ok\":true,\"procesados\":" + procesados + "}");

		} catch (final Exception e) {
			LOGGER.error("BrevoWebhook.doPost error: " + e.toString(), e);
			response.setStatus(HttpServletResponse.SC_OK); // Retornar 200 para evitar reintentos infinitos si el payload era invalido
			response.getWriter().write("{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}");
		} finally {
			if (cn != null) {
				try {
					cn.close();
				} catch (final Exception ignore) {
				}
			}
		}
	}

	private void procesarNodo(final Connection cn, final JsonNode nodo, final String ipOrigen) {
		if (nodo == null || !nodo.isObject()) return;

		final String evento = extraerTexto(nodo, "event");
		final String email = extraerTexto(nodo, "email");

		// El motivo puede venir en reason, description o subject
		String motivo = extraerTexto(nodo, "reason");
		if (motivo.isEmpty()) {
			motivo = extraerTexto(nodo, "description");
		}
		if (motivo.isEmpty()) {
			motivo = extraerTexto(nodo, "subject");
		}

		BrevoWebhookDAO.procesarEvento(cn, evento, email, motivo, ipOrigen, nodo.toString());
	}

	private String extraerTexto(final JsonNode nodo, final String campo) {
		if (nodo.hasNonNull(campo)) {
			return nodo.get(campo).asText("").trim();
		}
		return "";
	}

	private byte[] leerCuerpo(final InputStream is) throws IOException {
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		final byte[] temporal = new byte[4096];
		int leidos;
		int acumulados = 0;
		while ((leidos = is.read(temporal)) != -1) {
			acumulados += leidos;
			if (acumulados > MAXIMO_CUERPO) {
				return null;
			}
			buffer.write(temporal, 0, leidos);
		}
		return buffer.toByteArray();
	}

	private String obtenerIp(final HttpServletRequest req) {
		String ip = req.getHeader("X-Forwarded-For");
		if (ip != null && !ip.trim().isEmpty()) {
			final int coma = ip.indexOf(',');
			return (coma != -1 ? ip.substring(0, coma) : ip).trim();
		}
		ip = req.getHeader("X-Real-IP");
		if (ip != null && !ip.trim().isEmpty()) {
			return ip.trim();
		}
		return req.getRemoteAddr() != null ? req.getRemoteAddr() : "";
	}
}
