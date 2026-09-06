package utilidadesCC;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
 * vivos al mismo tiempo en produccion. Los dos que estan aqui son los unicos
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
 * necesita otros tiempos, se deriva del compartido con .newBuilder(), que
 * reutiliza el pool de conexiones y los hilos en vez de crear otros.
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
}
