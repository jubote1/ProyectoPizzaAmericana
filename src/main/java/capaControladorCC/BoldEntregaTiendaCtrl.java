package capaControladorCC;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import capaDAOCC.BoldSedeTiendaDAO;
import capaDAOCC.LogEventoBoldDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.LogEventoBold;
import capaModeloCC.Tienda;
import utilidadesCC.ClientesHttp;

/**
 * Entrega a cada tienda los eventos de Bold que le corresponden, para que los
 * guarde en su base local (servicio RegistrarMovimientoBold de
 * ProyectoTiendaAmericana) y el POS los muestre en la conciliacion de SONO QR.
 *
 * La tienda se resuelve por el usuario de Bold que trae el evento
 * (bold_sede_tienda). Solo se entregan eventos con firma valida.
 *
 * El webhook intenta la entrega de inmediato y sin bloquear la respuesta a Bold
 * (entregarAsync). Si la tienda esta apagada, o la sede todavia no esta
 * registrada, el evento queda pendiente y lo recoge reintentarPendientes(), que
 * corre desde Servicios cada pocos minutos.
 *
 * Si el parametro TOKENEVENTOSBOLD (general.parametros) tiene valor, se envia a
 * la tienda, que solo acepta eventos con ese mismo token: sin el, cualquiera
 * con acceso a la red de la tienda podria inventar pagos QR.
 */
public class BoldEntregaTiendaCtrl {

	private static final String SERVICIO_TIENDA = "RegistrarMovimientoBold";
	private static final String PARAMETRO_TOKEN = "TOKENEVENTOSBOLD";

	private static final int MAX_INTENTOS = 1000;
	private static final int DIAS_ATRAS = 3;
	private static final int LIMITE_POR_CORRIDA = 300;

	/**
	 * Cola acotada: si la tienda no responde y se acumulan entregas, las que no
	 * quepan se descartan aqui y las recoge el reintento. Hilos daemon, para no
	 * frenar el apagado del servidor.
	 */
	private static final ThreadPoolExecutor EJECUTOR = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>(500), r -> {
				final Thread t = new Thread(r, "bold-entrega-tienda");
				t.setDaemon(true);
				return t;
			}, new ThreadPoolExecutor.DiscardPolicy());

	public static void entregarAsync(final long idLog) {
		EJECUTOR.submit(() -> entregar(idLog));
	}

	/** @return true si el evento quedo guardado en la tienda. */
	public static boolean entregar(final long idLog) {
		final Logger logger = Logger.getLogger("log_file");
		try {
			final LogEventoBold evento = LogEventoBoldDAO.obtenerParaEntrega(idLog);
			if (evento == null || !evento.isFirmaValida()) {
				return false;
			}
			if (evento.isEntregadoTienda()) {
				return true;
			}
			int idTienda = evento.getIdTienda();
			if (idTienda <= 0) {
				idTienda = BoldSedeTiendaDAO.buscarIdTienda(evento.getBoldUserId(), evento.getSellerEmail());
			}
			if (idTienda <= 0) {
				LogEventoBoldDAO.registrarIntentoEntrega(idLog, 0, false, "La sede no esta en bold_sede_tienda", false);
				return false;
			}
			final Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
			if (tienda == null || tienda.getUrl() == null || tienda.getUrl().trim().isEmpty()) {
				LogEventoBoldDAO.registrarIntentoEntrega(idLog, idTienda, false, "La tienda no tiene URL de servicio", true);
				return false;
			}
			final String resultado = enviar(tienda.getUrl(), evento);
			final boolean ok = "OK".equals(resultado);
			LogEventoBoldDAO.registrarIntentoEntrega(idLog, idTienda, ok, ok ? null : resultado, true);
			return ok;
		} catch (final Exception e) {
			logger.error("BoldEntregaTiendaCtrl.entregar " + idLog + ": " + e.toString());
			return false;
		}
	}

	/** Reintenta los eventos pendientes. Devuelve cuantos se entregaron. Lo invoca Servicios. */
	public static int reintentarPendientes() {
		final ArrayList<Long> ids = LogEventoBoldDAO.pendientesDeEntrega(MAX_INTENTOS, DIAS_ATRAS,
				LIMITE_POR_CORRIDA);
		int entregados = 0;
		for (final Long id : ids) {
			if (entregar(id.longValue())) {
				entregados++;
			}
		}
		System.out.println("Eventos Bold pendientes: " + ids.size() + ", entregados a su tienda: " + entregados);
		return entregados;
	}

	/** "OK" si la tienda lo guardo; si no, el motivo. */
	private static String enviar(final String urlTienda, final LogEventoBold e) {
		try {
			final RequestConfig config = RequestConfig.custom().setConnectTimeout(5000)
					.setConnectionRequestTimeout(3000).setSocketTimeout(10000).build();
			final HttpClient client = ClientesHttp.apache();
			final HttpPost post = new HttpPost(urlTienda + SERVICIO_TIENDA);
			// Contra la URL de una tienda que puede estar apagada: tiempos mas cortos que los del cliente compartido.
			post.setConfig(config);

			final List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("idlog", String.valueOf(e.getIdLog())));
			params.add(new BasicNameValuePair("paymentid", nulo(e.getPaymentId())));
			params.add(new BasicNameValuePair("tipoevento", nulo(e.getTipoEvento())));
			params.add(new BasicNameValuePair("metodo", nulo(e.getPaymentMethod())));
			params.add(new BasicNameValuePair("monto",
					e.getMontoTotal() == null ? "0" : e.getMontoTotal().toPlainString()));
			params.add(new BasicNameValuePair("moneda", nulo(e.getMoneda())));
			params.add(new BasicNameValuePair("referencia", nulo(e.getReferencia())));
			params.add(new BasicNameValuePair("fecha", aHoraColombia(e.getFechaEvento())));
			final String token = ParametrosDAO.retornarValorAlfanumerico(PARAMETRO_TOKEN);
			if (token != null && !token.trim().isEmpty()) {
				params.add(new BasicNameValuePair("token", token.trim()));
			}
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

			final HttpResponse response = client.execute(post);
			final int status = response.getStatusLine().getStatusCode();
			String cuerpo = "";
			try {
				if (response.getEntity() != null) {
					cuerpo = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8).trim();
				}
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
			if (status >= 200 && status < 300 && "OK".equalsIgnoreCase(cuerpo)) {
				return "OK";
			}
			return "La tienda respondio " + status + " " + cuerpo;
		} catch (final Exception ex) {
			return "No se pudo contactar la tienda: " + ex.toString();
		}
	}

	/**
	 * Bold manda la hora con su desfase (2026-09-21T09:16:36-05:00). A la tienda
	 * se le da la hora de Colombia como texto simple, que es la que ella entiende.
	 * Si no se puede leer, se deja el texto como llego, cortado a segundos.
	 */
	public static String aHoraColombia(final String fechaBold) {
		if (fechaBold == null || fechaBold.trim().isEmpty()) {
			return "";
		}
		final String limpia = fechaBold.trim();
		try {
			final OffsetDateTime fecha = OffsetDateTime.parse(limpia);
			return fecha.atZoneSameInstant(ZoneId.of("America/Bogota"))
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		} catch (final Exception e) {
			final String plana = limpia.replace('T', ' ');
			return plana.length() >= 19 ? plana.substring(0, 19) : plana;
		}
	}

	private static String nulo(final String texto) {
		return texto == null ? "" : texto;
	}

}
