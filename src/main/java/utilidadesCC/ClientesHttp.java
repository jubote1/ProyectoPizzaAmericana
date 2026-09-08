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
	 * Un Apache HttpClient con los tiempos limite puestos, NUEVO en cada llamada.
	 *
	 * OJO: aqui NO se comparte la instancia, y es a proposito. Se intento
	 * compartirla el 2026-09-06 y el 07 en la noche los pedidos de plataforma
	 * dejaron de llegar solos a las tiendas: quien operaba el central tuvo que
	 * reenviarlos a mano toda la noche.
	 *
	 * El motivo es el que ya se habia advertido al hacer el cambio y que no se
	 * mitigo lo suficiente: al compartir el cliente se comparte su pool y las
	 * conexiones se REUTILIZAN. Los endpoints de las tiendas son POS pequenos que
	 * cierran las conexiones ociosas sin avisar, asi que una conexion guardada
	 * llega muerta a la siguiente llamada. Eso sale como NoHttpResponseException,
	 * y Apache NO reintenta un POST porque no es idempotente: la llamada falla.
	 * El setValidateAfterInactivity de 2 segundos no alcanza, porque una conexion
	 * que la tienda cerro medio segundo antes ni siquiera se revalida.
	 *
	 * Y encima, los metodos que llaman a las tiendas leen la respuesta con un
	 * BufferedReader que nunca cierran; si el parseo del JSON revienta -y hay
	 * casts sin proteger- la conexion queda arrendada y con un pool compartido
	 * ese cupo se pierde para todo el servidor.
	 *
	 * Crear un cliente por llamada tiene el costo que se conoce -no reutiliza
	 * conexiones y deja objetos para el recolector-, pero es el comportamiento
	 * que funciono durante anos. Los tiempos limite si se conservan, porque esos
	 * nunca fueron el problema: sin ellos una tienda lenta dejaba el hilo de
	 * Tomcat esperando para siempre.
	 *
	 * Antes de volver a intentar compartirlo hay que, en este orden: cerrar las
	 * respuestas en los 37 sitios que llaman, y probar con una sola tienda.
	 */
	public static CloseableHttpClient apache() {
		return (HttpClientBuilder.create()
				.setDefaultRequestConfig(CONFIG_APACHE)
				.build());
	}

	/**
	 * Lo mismo, siguiendo redirecciones en POST, para el proveedor de domicilios
	 * tercerizados que contesta 307. Tambien nuevo en cada llamada.
	 */
	public static CloseableHttpClient apacheConRedirecciones() {
		return (HttpClientBuilder.create()
				.setDefaultRequestConfig(CONFIG_APACHE)
				.setRedirectStrategy(new LaxRedirectStrategy())
				.build());
	}
}
