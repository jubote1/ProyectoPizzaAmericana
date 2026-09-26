package utilidadesCC;

import java.util.HashMap;
import java.util.Map;

/**
 * Freno a quien prueba codigos promocionales al azar.
 *
 * Los servicios de codigos no piden sesion (los consume el POS de las tiendas, que no tiene), asi que
 * cualquiera que llegue a la URL puede probar codigos hasta acertar. Este limitador cuenta los
 * intentos FALLIDOS (codigo que no existe) por origen y, pasado el tope dentro de la ventana, bloquea
 * ese origen un rato.
 *
 * Un acierto NO reinicia la cuenta: si lo hiciera, quien prueba podria intercalar un codigo bueno
 * cada tanto y no bloquearse nunca. La cuenta solo se reinicia cuando pasa la ventana.
 *
 * Vive en memoria: se pierde al reiniciar el servidor, y con un solo Tomcat alcanza. Es una barrera
 * contra el barrido automatico, no un sistema de auditoria (eso lo hace el log de redenciones).
 */
public final class LimitadorIntentos {

	/** Fallos permitidos dentro de la ventana antes de bloquear. */
	public static final int MAX_FALLOS = 15;
	private static final long VENTANA_MS = 10L * 60L * 1000L;
	private static final long BLOQUEO_MS = 15L * 60L * 1000L;

	private static final class Estado {
		int fallos;
		long inicioVentana;
		long bloqueadoHasta;
	}

	private static final Map<String, Estado> ESTADOS = new HashMap<String, Estado>();

	private LimitadorIntentos() {
	}

	/** true si ese origen esta bloqueado en este momento. */
	public static synchronized boolean bloqueado(final String clave) {
		final Estado e = ESTADOS.get(clave);
		return e != null && e.bloqueadoHasta > System.currentTimeMillis();
	}

	/** Anota un intento fallido y bloquea si se paso del tope. */
	public static synchronized void registrarFallo(final String clave) {
		final long ahora = System.currentTimeMillis();
		Estado e = ESTADOS.get(clave);
		if (e == null || ahora - e.inicioVentana > VENTANA_MS) {
			e = new Estado();
			e.inicioVentana = ahora;
			ESTADOS.put(clave, e);
		}
		e.fallos++;
		if (e.fallos >= MAX_FALLOS) {
			e.bloqueadoHasta = ahora + BLOQUEO_MS;
		}
		//Para que el mapa no crezca sin fin con origenes que pasaron una vez.
		if (ESTADOS.size() > 5000) {
			ESTADOS.entrySet().removeIf(x -> ahora - x.getValue().inicioVentana > VENTANA_MS
					&& x.getValue().bloqueadoHasta < ahora);
		}
	}
}
