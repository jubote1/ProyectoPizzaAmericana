package utilidadesCC;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;

import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * Los clientes HTTP que comparte toda la aplicacion.
 *
 * Existe por dos motivos, y los dos costaron caidas del servidor central.
 *
 * El primero es que un cliente HTTP NO es un objeto desechable. Cada
 * OkHttpClient trae su propio pool de conexiones con su hilo de limpieza, y
 * cada java.net.http.HttpClient trae un hilo SelectorManager mas su pool de
 * ejecucion. En Java 17 ese HttpClient ni siquiera se puede cerrar: no tiene
 * close() hasta Java 21. Crear uno por llamada deja hilos atras que se
 * acumulan pedido tras pedido hasta que el proceso se come los nucleos del
 * servidor girando sobre epoll. Se llego a ver 73 HttpClient y 29 OkHttpClient
 * vivos al mismo tiempo en produccion. Los tres que estan aqui son los unicos
 * que deberia haber, y son seguros para uso concurrente: estan hechos para
 * compartirse entre hilos.
 *
 * El segundo es que ninguna llamada a un tercero puede quedarse esperando sin
 * limite. Los proveedores externos -mapas, mensajeria, pasarelas- se ponen
 * lentos o dejan de contestar, y sin tiempo limite el hilo de Tomcat que
 * atiende ese pedido se queda colgado para siempre. Aqui todos los tiempos
 * estan puestos de forma explicita.
 *
 * Al usarlos NUNCA se hace .build() de uno nuevo. Si alguna llamada puntual
 * necesita otros tiempos, en OkHttp se deriva del compartido con .newBuilder()
 * y en Apache se le pone un RequestConfig a la peticion, no al cliente.
 */
public final class ClientesHttp {

	/** Lo que se espera para levantar la conexion con el tercero. */
	private static final long SEGUNDOS_CONEXION = 10;

	/** Lo que se espera por la respuesta una vez conectados. */
	private static final long SEGUNDOS_LECTURA = 20;

	private static final long SEGUNDOS_ESCRITURA = 20;

	/**
	 * Techo de la llamada completa, incluidos reintentos y redirecciones. Es la
	 * red de seguridad: pase lo que pase, la llamada termina.
	 */
	private static final long SEGUNDOS_LLAMADA = 30;

	/** Conexiones ociosas que se guardan para reutilizar, y por cuanto tiempo. */
	private static final int CONEXIONES_OCIOSAS = 20;

	private static final long MINUTOS_CONEXION_VIVA = 5;

	private static final OkHttpClient OK = new OkHttpClient.Builder()
			.connectTimeout(SEGUNDOS_CONEXION, TimeUnit.SECONDS)
			.readTimeout(SEGUNDOS_LECTURA, TimeUnit.SECONDS)
			.writeTimeout(SEGUNDOS_ESCRITURA, TimeUnit.SECONDS)
			.callTimeout(SEGUNDOS_LLAMADA, TimeUnit.SECONDS)
			.connectionPool(new ConnectionPool(CONEXIONES_OCIOSAS, MINUTOS_CONEXION_VIVA, TimeUnit.MINUTES))
			.build();

	private static final HttpClient JDK = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(SEGUNDOS_CONEXION))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/**
	 * Tiempo limite que hay que ponerle a cada HttpRequest del cliente del JDK.
	 *
	 * Ojo con esto: a diferencia de OkHttp, en java.net.http el tiempo de espera
	 * de la respuesta NO se configura en el cliente sino en cada peticion. Un
	 * HttpRequest sin .timeout() espera indefinidamente por mas que el cliente
	 * tenga connectTimeout, porque ese solo cubre levantar la conexion.
	 */
	public static final Duration ESPERA_RESPUESTA = Duration.ofSeconds(SEGUNDOS_LLAMADA);

	private static final int MILIS_CONEXION = (int) (SEGUNDOS_CONEXION * 1000);

	private static final int MILIS_LECTURA = (int) (SEGUNDOS_LECTURA * 1000);

	/** Lo que se espera por una conexion libre del pool antes de rendirse. */
	private static final int MILIS_ESPERA_POOL = 10000;

	/** Techo de conexiones simultaneas en total y contra un mismo host. */
	private static final int CONEXIONES_TOTALES = 400;

	private static final int CONEXIONES_POR_HOST = 80;

	private static final RequestConfig CONFIG_APACHE = RequestConfig.custom()
			.setConnectTimeout(MILIS_CONEXION)
			.setSocketTimeout(MILIS_LECTURA)
			.setConnectionRequestTimeout(MILIS_ESPERA_POOL)
			.build();

	/**
	 * Un gestor de conexiones para cada cliente Apache compartido.
	 */
	private static PoolingHttpClientConnectionManager nuevoGestor() {
		final PoolingHttpClientConnectionManager gestor = new PoolingHttpClientConnectionManager();
		gestor.setMaxTotal(CONEXIONES_TOTALES);
		gestor.setDefaultMaxPerRoute(CONEXIONES_POR_HOST);
		gestor.setValidateAfterInactivity(2000);
		return (gestor);
	}

	private static final PoolingHttpClientConnectionManager GESTOR_APACHE = nuevoGestor();
	private static final PoolingHttpClientConnectionManager GESTOR_APACHE_REDIRECCIONES = nuevoGestor();

	private static final CloseableHttpClient APACHE = HttpClientBuilder.create()
			.setConnectionManager(GESTOR_APACHE)
			.setDefaultRequestConfig(CONFIG_APACHE)
			.evictExpiredConnections()
			.evictIdleConnections(30, TimeUnit.SECONDS)
			.build();

	/**
	 * El mismo cliente pero siguiendo redirecciones en POST.
	 */
	private static final CloseableHttpClient APACHE_REDIRECCIONES = HttpClientBuilder.create()
			.setConnectionManager(GESTOR_APACHE_REDIRECCIONES)
			.setDefaultRequestConfig(CONFIG_APACHE)
			.setRedirectStrategy(new LaxRedirectStrategy())
			.evictExpiredConnections()
			.evictIdleConnections(30, TimeUnit.SECONDS)
			.build();

	// --- PARAMETROS DE MONITOREO Y ALERTAS ---
	private static final long COOLDOWN_ALERTA_MS = TimeUnit.MINUTES.toMillis(15);
	private static volatile long ultimaAlertaMs = 0;
	private static final int UMBRAL_ALERTA_LEASED_TOTAL = 280; // 70% de 400
	private static final int UMBRAL_ALERTA_LEASED_RUTA = 60;   // 75% de 80

	static {
		iniciarMonitoreoSegundoPlano();
	}

	private static void iniciarMonitoreoSegundoPlano() {
		ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "PoolHttp-Monitor");
			t.setDaemon(true);
			return t;
		});

		monitor.scheduleAtFixedRate(() -> {
			try {
				verificarSaludPool();
			} catch (Throwable t) {
				// No interrumpir el programador ante fallos imprevistos
			}
		}, 60, 60, TimeUnit.SECONDS);
	}

	/**
	 * Verifica periódicamente la salud de las conexiones en Apache y OkHttp.
	 * Si detecta hilos en cola de espera o saturación cercana al límite, envía una alerta por correo.
	 */
	private static void verificarSaludPool() {
		PoolStats statsApache = GESTOR_APACHE.getTotalStats();
		PoolStats statsRedir = GESTOR_APACHE_REDIRECCIONES.getTotalStats();

		int leasedApache = statsApache.getLeased();
		int pendingApache = statsApache.getPending();
		int leasedRedir = statsRedir.getLeased();
		int pendingRedir = statsRedir.getPending();

		boolean hayPeticionesEnEspera = (pendingApache > 0 || pendingRedir > 0);
		boolean saturacionTotal = (leasedApache >= UMBRAL_ALERTA_LEASED_TOTAL || leasedRedir >= UMBRAL_ALERTA_LEASED_TOTAL);

		String rutaSaturada = null;
		int leasedRutaSaturada = 0;
		Set<HttpRoute> rutas = GESTOR_APACHE.getRoutes();
		if (rutas != null) {
			for (HttpRoute r : rutas) {
				PoolStats rStats = GESTOR_APACHE.getStats(r);
				if (rStats != null) {
					if (rStats.getPending() > 0 || rStats.getLeased() >= UMBRAL_ALERTA_LEASED_RUTA) {
						rutaSaturada = r.getTargetHost().toURI();
						leasedRutaSaturada = rStats.getLeased();
						break;
					}
				}
			}
		}

		if (hayPeticionesEnEspera || saturacionTotal || rutaSaturada != null) {
			long ahora = System.currentTimeMillis();
			if (ahora - ultimaAlertaMs >= COOLDOWN_ALERTA_MS) {
				ultimaAlertaMs = ahora;
				enviarAlertaCorreo(statsApache, statsRedir, rutaSaturada, leasedRutaSaturada);
			}
		}
	}

	/**
	 * Envía de forma asíncrona una alerta por correo a tecnología cuando se detecta estrés en el pool.
	 */
	private static void enviarAlertaCorreo(PoolStats statsApache, PoolStats statsRedir, String rutaSaturada, int leasedRuta) {
		new Thread(() -> {
			try {
				CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOERROR", "CLAVECORREOERROR");
				if (infoCorreo == null) {
					return;
				}

				String asunto = "⚠️ ALERTA CENTRAL: Alta saturación en Pool de Conexiones HTTP";
				StringBuilder html = new StringBuilder();
				html.append("<h3>⚠️ ALERTA DE CONGESTIÓN EN POOL DE CONEXIONES HTTP (CENTRAL)</h3>");
				html.append("<p>El sistema ha detectado alta demanda o peticiones en cola de espera en el pool de conexiones:</p>");
				html.append("<ul>");
				html.append("<li><b>Apache General - En uso (activas):</b> ").append(statsApache.getLeased()).append(" / ").append(statsApache.getMax()).append("</li>");
				html.append("<li><b>Apache General - Libres en reposo:</b> ").append(statsApache.getAvailable()).append("</li>");
				html.append("<li><b>Apache General - En cola esperando (PENDING):</b> <span style='color:red; font-weight:bold;'>").append(statsApache.getPending()).append("</span></li>");
				html.append("<li><b>Apache Redirecciones - En uso:</b> ").append(statsRedir.getLeased()).append(" / ").append(statsRedir.getMax()).append("</li>");
				html.append("<li><b>Apache Redirecciones - En espera:</b> ").append(statsRedir.getPending()).append("</li>");
				if (rutaSaturada != null) {
					html.append("<li><b>Host/Ruta con mayor demanda:</b> ").append(rutaSaturada).append(" (").append(leasedRuta).append(" conexiones activas)</li>");
				}
				html.append("</ul>");
				html.append("<p><i>Nota: Esta notificación cuenta con protección anti-spam (máximo 1 correo cada 15 minutos). Puedes verificar las métricas en tiempo real en el endpoint /EstadoPoolHttp.</i></p>");

				Correo correoAlerta = new Correo();
				ArrayList<String> correos = new ArrayList<>();
				correos.add("tecnologia@pizzaamericana.com.co");
				correoAlerta.setAsunto(asunto);
				correoAlerta.setContrasena(infoCorreo.getClaveCorreo());
				correoAlerta.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
				correoAlerta.setMensaje(html.toString());

				ControladorEnvioCorreo contro = new ControladorEnvioCorreo(correoAlerta, correos);
				contro.enviarCorreo();
				System.out.println("📧 Alerta de saturación de pool HTTP enviada a tecnología@pizzaamericana.com.co exitosamente.");
			} catch (Exception e) {
				System.err.println("Error enviando alerta por correo de pool HTTP: " + e.getMessage());
			}
		}, "PoolHttp-AlertaCorreo").start();
	}

	/**
	 * Genera un JSON con el estado de salud en tiempo real de todos los pools HTTP.
	 * Utilizado por el Servlet /EstadoPoolHttp.
	 */
	public static String obtenerEstadoPoolsJSON() {
		PoolStats statsApache = GESTOR_APACHE.getTotalStats();
		PoolStats statsRedir = GESTOR_APACHE_REDIRECCIONES.getTotalStats();
		ConnectionPool okPool = OK.connectionPool();

		int totalOk = okPool.connectionCount();
		int idleOk = okPool.idleConnectionCount();
		int activeOk = Math.max(0, totalOk - idleOk);

		boolean saludable = (statsApache.getPending() == 0 && statsRedir.getPending() == 0 && statsApache.getLeased() < UMBRAL_ALERTA_LEASED_TOTAL);
		String estadoSalud = saludable ? "SALUDABLE" : "CONGESTIONADO";

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"estado\":\"").append(estadoSalud).append("\",");
		sb.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");

		// Apache General
		sb.append("\"apache_general\":{");
		sb.append("\"en_uso\":").append(statsApache.getLeased()).append(",");
		sb.append("\"disponibles\":").append(statsApache.getAvailable()).append(",");
		sb.append("\"en_cola_esperando\":").append(statsApache.getPending()).append(",");
		sb.append("\"max_total\":").append(statsApache.getMax()).append(",");
		sb.append("\"max_por_host\":").append(CONEXIONES_POR_HOST);
		sb.append("},");

		// Apache Redirecciones
		sb.append("\"apache_redirecciones\":{");
		sb.append("\"en_uso\":").append(statsRedir.getLeased()).append(",");
		sb.append("\"disponibles\":").append(statsRedir.getAvailable()).append(",");
		sb.append("\"en_cola_esperando\":").append(statsRedir.getPending()).append(",");
		sb.append("\"max_total\":").append(statsRedir.getMax());
		sb.append("},");

		// OkHttp
		sb.append("\"okhttp\":{");
		sb.append("\"conexiones_totales\":").append(totalOk).append(",");
		sb.append("\"disponibles_ociosas\":").append(idleOk).append(",");
		sb.append("\"en_uso_activas\":").append(activeOk);
		sb.append("},");

		// Rutas activas
		sb.append("\"rutas_activas\":[");
		Set<HttpRoute> rutas = GESTOR_APACHE.getRoutes();
		boolean primero = true;
		if (rutas != null) {
			for (HttpRoute r : rutas) {
				PoolStats rStats = GESTOR_APACHE.getStats(r);
				if (rStats != null && (rStats.getLeased() > 0 || rStats.getAvailable() > 0 || rStats.getPending() > 0)) {
					if (!primero) sb.append(",");
					primero = false;
					sb.append("{");
					sb.append("\"host\":\"").append(r.getTargetHost().toURI()).append("\",");
					sb.append("\"en_uso\":").append(rStats.getLeased()).append(",");
					sb.append("\"disponibles\":").append(rStats.getAvailable()).append(",");
					sb.append("\"en_cola\":").append(rStats.getPending());
					sb.append("}");
				}
			}
		}
		sb.append("]");

		sb.append("}");
		return sb.toString();
	}

	private ClientesHttp() {
		super();
	}

	/** El OkHttpClient de toda la aplicacion. No se cierra ni se reemplaza. */
	public static OkHttpClient ok() {
		return (OK);
	}

	/** El java.net.http.HttpClient de toda la aplicacion. */
	public static HttpClient jdk() {
		return (JDK);
	}

	/**
	 * El Apache HttpClient de toda la aplicacion.
	 *
	 * NO se le llame close(): es compartido y cerrarlo dejaria inservible el
	 * cliente para todo el resto de la aplicacion. Las respuestas si hay que
	 * cerrarlas o consumirlas como siempre, para que la conexion vuelva al pool.
	 */
	public static CloseableHttpClient apache() {
		return (APACHE);
	}

	/** El Apache HttpClient que sigue redirecciones en POST. Tampoco se cierra. */
	public static CloseableHttpClient apacheConRedirecciones() {
		return (APACHE_REDIRECCIONES);
	}
}
