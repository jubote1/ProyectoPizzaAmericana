package capaServicioCC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaControladorCC.BoldEntregaTiendaCtrl;
import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.LogEventoBoldDAO;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.LogEventoBold;
import capaSeguridad.FirmaBold;

/**
 * Webhook de Bold (SONO QR y datafonos): recibe los eventos de pago y, por
 * ahora, solo los guarda en log_evento_bold. No actua sobre ellos.
 *
 * La llave secreta para validar la firma esta en integracion_crm
 * (crm = 'SONOQR', columna fresh_token). Un evento con firma no valida se
 * guarda igual, marcado: en esta fase lo que importa es ver que llega, y si
 * ademas se le respondiera 401 a Bold, reintentaria 5 veces un evento que
 * tampoco vamos a usar.
 *
 * Bold exige un 200 en 2 segundos, asi que aqui no hay nada mas que validar,
 * guardar y responder. Si no se pudo guardar se responde 500 para que Bold
 * reintente (15 min, 1 h, 4 h, 8 h, 24 h).
 *
 * Es publico a proposito, como el de Wompi: Bold no tiene sesion. No es
 * necesario registrarlo en web.xml (SeguridadFilter solo actua sobre las URLs
 * mapeadas en pantalla_servlet).
 */
@WebServlet("/api/bold/webhook")
public class BoldWebhook extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String CRM_SONOQR = "SONOQR";
	/** Un evento de Bold pesa unos cientos de bytes; esto solo evita que el log se llene de basura. */
	private static final int MAXIMO_CUERPO = 256 * 1024;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final Logger logger = Logger.getLogger("log_file");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		final byte[] cuerpo = leerCuerpo(request.getInputStream());
		if (cuerpo == null) {
			response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			response.getWriter().write("{\"ok\":false,\"message\":\"Cuerpo demasiado grande\"}");
			return;
		}
		if (cuerpo.length == 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"ok\":false,\"message\":\"Cuerpo vacio\"}");
			return;
		}

		final IntegracionCRM integracion = IntegracionCRMDAO.obtenerInformacionIntegracion(CRM_SONOQR);
		final String motivoFirma = FirmaBold.validar(cuerpo, request.getHeader("x-bold-signature"),
				integracion.getFreshToken(), integracion.getAccessToken());

		final LogEventoBold evento = new LogEventoBold();
		evento.setHashCuerpo(FirmaBold.sha256Hex(cuerpo));
		evento.setJsonEvento(new String(cuerpo, StandardCharsets.UTF_8));
		evento.setFirmaValida(FirmaBold.esValida(motivoFirma));
		evento.setMotivoFirma(motivoFirma);
		evento.setIpOrigen(origen(request));
		// Lo que Bold mando, tal cual: la firma y el tipo de contenido. Sirven para analizar por que una firma no coincide.
		evento.setFirmaRecibida(request.getHeader("x-bold-signature"));
		evento.setContentType(request.getContentType());
		extraerCampos(evento);

		final int resultado = LogEventoBoldDAO.insertar(evento);
		if (resultado == LogEventoBoldDAO.ERROR) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"ok\":false,\"message\":\"No se pudo guardar\"}");
			return;
		}
		logger.info("BoldWebhook: " + evento.getTipoEvento() + " payment_id=" + evento.getPaymentId() + " firma="
				+ motivoFirma + (resultado == LogEventoBoldDAO.DUPLICADO ? " (reintento, ya estaba)" : ""));
		// A la tienda se le entrega en otro hilo: Bold espera el 200 en 2 segundos y la tienda puede tardar o estar apagada.
		// Si no se logra, queda pendiente y lo recoge el reintento de Servicios.
		if (resultado == LogEventoBoldDAO.GUARDADO && evento.isFirmaValida() && evento.getIdLog() > 0) {
			BoldEntregaTiendaCtrl.entregarAsync(evento.getIdLog());
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write("{\"ok\":true}");
	}

	/** Para verificar que la URL esta arriba (por ejemplo desde el navegador). No expone nada. */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.getWriter().write("{\"ok\":true,\"servicio\":\"bold-webhook\"}");
	}

	/** Lo que se pueda sacar del JSON; si no se puede leer, el evento se guarda igual solo con el cuerpo. */
	private void extraerCampos(final LogEventoBold evento) {
		try {
			final JsonNode raiz = MAPPER.readTree(evento.getJsonEvento());
			final JsonNode data = raiz.path("data");
			evento.setIdNotificacion(texto(raiz.path("id")));
			evento.setTipoEvento(texto(raiz.path("type")));
			evento.setPaymentId(texto(data.path("payment_id")));
			evento.setMerchantId(texto(data.path("merchant_id")));
			evento.setPaymentMethod(texto(data.path("payment_method")));
			evento.setMoneda(texto(data.path("amount").path("currency")));
			evento.setMontoTotal(numero(data.path("amount").path("total")));
			evento.setReferencia(texto(data.path("metadata").path("reference")));
			evento.setTerminalId(texto(data.path("card").path("terminal_id")));
			evento.setFechaEvento(texto(data.path("created_at")));
			// El usuario de Bold de la sede (y su datafono): con esto se sabe la tienda.
			evento.setSellerEmail(texto(data.path("seller").path("email")));
			evento.setBoldUserId(texto(data.path("user_id")));
		} catch (final Exception e) {
			Logger.getLogger("log_file").warn("BoldWebhook: el cuerpo no es un JSON legible: " + e.toString());
		}
	}

	private static String texto(final JsonNode nodo) {
		return nodo.isMissingNode() || nodo.isNull() ? null : nodo.asText();
	}

	private static BigDecimal numero(final JsonNode nodo) {
		if (nodo.isMissingNode() || nodo.isNull()) {
			return null;
		}
		try {
			return nodo.isNumber() ? nodo.decimalValue() : new BigDecimal(nodo.asText().trim());
		} catch (final Exception e) {
			return null;
		}
	}

	/** null si supera el maximo. */
	private static byte[] leerCuerpo(final InputStream entrada) throws IOException {
		final ByteArrayOutputStream salida = new ByteArrayOutputStream();
		final byte[] buffer = new byte[4096];
		int leidos;
		while ((leidos = entrada.read(buffer)) != -1) {
			salida.write(buffer, 0, leidos);
			if (salida.size() > MAXIMO_CUERPO) {
				return null;
			}
		}
		return salida.toByteArray();
	}

	private static String origen(final HttpServletRequest request) {
		final String reenviado = request.getHeader("X-Forwarded-For");
		if (reenviado != null && !reenviado.trim().isEmpty()) {
			return reenviado.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
