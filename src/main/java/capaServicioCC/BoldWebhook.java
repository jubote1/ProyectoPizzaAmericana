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
 * Servicio que modifica datos de Pagos virtuales y conciliacion: invoca
 * IntegracionCRMDAO.obtenerInformacionIntegracion, LogEventoBoldDAO.insertar.
 *
 * Ruta: /ProyectoPizzaAmericana/api/bold/webhook   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera X-Forwarded-For, x-bold-signature.
 * Devuelve: application/json; claves: message, ok, servicio.
 * Lee: bold_sede_tienda, integracion_crm, log_evento_bold, parametros, tienda.
 * Escribe: log_evento_bold (INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha BoldWebhook.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/api/bold/webhook")
public class BoldWebhook extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String CRM_SONOQR = "SONOQR";
	/** Un evento de Bold pesa unos cientos de bytes; esto solo evita que el log se llene de basura. */
	private static final int MAXIMO_CUERPO = 256 * 1024;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Cada cuanto se vuelve a leer la configuracion de Bold de la base.
	 *
	 * Son las llaves de firma: cambian cuando alguien las rota, o sea casi
	 * nunca. Cinco minutos es suficiente para que una rotacion entre sola y
	 * corto para que nadie tenga que reiniciar nada.
	 */
	private static final long VIGENCIA_CONFIGURACION_MS = 5L * 60L * 1000L;

	/** volatile y no synchronized: leer dos veces de mas no hace danio. */
	private static volatile IntegracionCRM configuracion;
	private static volatile long configuracionLeidaEn;

	/**
	 * La configuracion de Bold, de memoria mientras siga vigente.
	 *
	 * POR QUE NO SE LEE EN CADA NOTIFICACION
	 *
	 * Bold espera el 200 en dos segundos y reintenta si no llega. Antes del 200
	 * habia DOS viajes a la base: este y el insert del evento. El insert no se
	 * puede mover -es lo que hace durable el evento, y acusar antes de guardar
	 * seria perderlo si falla-, pero este si: es un select de una fila de
	 * configuracion que practicamente nunca cambia.
	 *
	 * El 16 de octubre de 2026, entre las 16:00 y las 23:00, el central iba
	 * lento y Bold reintento 67 notificaciones. Con un viaje menos el camino
	 * caliente es la mitad de largo.
	 *
	 * CUIDADO CON LO QUE SE GUARDA
	 *
	 * obtenerInformacionIntegracion NO devuelve null cuando falla: devuelve un
	 * IntegracionCRM vacio. Guardar eso en memoria seria peor que no tener
	 * cache, porque durante los cinco minutos siguientes todas las firmas se
	 * validarian contra una llave en blanco y ninguna coincidiria. Por eso solo
	 * se guarda lo que trae llave, y si una lectura sale vacia se sigue usando
	 * la anterior aunque este vencida: una firma validada con llaves de hace
	 * diez minutos es mejor que rechazar todo.
	 */
	private static IntegracionCRM configuracionBold() {
		final long ahora = System.currentTimeMillis();
		final IntegracionCRM enMemoria = configuracion;
		if (enMemoria != null && ahora - configuracionLeidaEn < VIGENCIA_CONFIGURACION_MS) {
			return enMemoria;
		}
		final IntegracionCRM leida = IntegracionCRMDAO.obtenerInformacionIntegracion(CRM_SONOQR);
		if (leida == null || leida.getFreshToken() == null || leida.getFreshToken().trim().isEmpty()) {
			//Lectura fallida o configuracion sin llave. Si habia una buena se
			//sigue con ella; si nunca la hubo, se devuelve lo que vino y
			//FirmaBold respondera SIN_LLAVE, que es lo que pasaba antes.
			return enMemoria != null ? enMemoria : leida;
		}
		configuracion = leida;
		configuracionLeidaEn = ahora;
		return leida;
	}

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

		final IntegracionCRM integracion = configuracionBold();
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
		// (entregar() decide si un evento sin firma valida pasa: solo con BOLDENTREGASINFIRMA = 'S'.)
		if (resultado == LogEventoBoldDAO.GUARDADO && evento.getIdLog() > 0) {
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
