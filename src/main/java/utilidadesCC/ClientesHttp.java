package utilidadesCC;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

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
	private static final int CONEXIONES_TOTALES = 200;

	private static final int CONEXIONES_POR_HOST = 50;

	private static final RequestConfig CONFIG_APACHE = RequestConfig.custom()
			.setConnectTimeout(MILIS_CONEXION)
			.setSocketTimeout(MILIS_LECTURA)
			.setConnectionRequestTimeout(MILIS_ESPERA_POOL)
			.build();

	/**
	 * Un gestor de conexiones para cada cliente Apache compartido.
	 *
	 * Los dos topes de aqui son criticos y no se pueden dejar por defecto: el
	 * defaultMaxPerRoute de Apache es 2, asi que un pool sin configurar
	 * serializaria todas las llamadas contra un mismo host de dos en dos, y en
	 * hora pico eso seria peor que el problema que veniamos a resolver.
	 *
	 * El validateAfterInactivity revisa la conexion antes de reusarla. Al pasar
	 * de "una conexion nueva por llamada" a un pool que las reutiliza aparece un
	 * riesgo que antes no existia: que el otro extremo haya cerrado la conexion
	 * mientras estaba guardada. Sin esta revision eso sale como un
	 * NoHttpResponseException esporadico en los POST, que Apache no reintenta.
	 */
	private static PoolingHttpClientConnectionManager nuevoGestor() {
		final PoolingHttpClientConnectionManager gestor = new PoolingHttpClientConnectionManager();
		gestor.setMaxTotal(CONEXIONES_TOTALES);
		gestor.setDefaultMaxPerRoute(CONEXIONES_POR_HOST);
		gestor.setValidateAfterInactivity(2000);
		return (gestor);
	}

	private static final CloseableHttpClient APACHE = HttpClientBuilder.create()
			.setConnectionManager(nuevoGestor())
			.setDefaultRequestConfig(CONFIG_APACHE)
			.evictExpiredConnections()
			.evictIdleConnections(30, TimeUnit.SECONDS)
			.build();

	/**
	 * El mismo cliente pero siguiendo redirecciones en POST.
	 *
	 * La estrategia de redireccion es del cliente y no de la peticion, asi que
	 * este caso necesita instancia aparte. Lo usa la integracion de domicilios
	 * tercerizados, cuyo proveedor contesta 307 a los POST.
	 */
	private static final CloseableHttpClient APACHE_REDIRECCIONES = HttpClientBuilder.create()
			.setConnectionManager(nuevoGestor())
			.setDefaultRequestConfig(CONFIG_APACHE)
			.setRedirectStrategy(new LaxRedirectStrategy())
			.evictExpiredConnections()
			.evictIdleConnections(30, TimeUnit.SECONDS)
			.build();

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
