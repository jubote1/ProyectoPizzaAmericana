package utilidadesCC;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import capaControladorCC.EnvioPublicidadCtrl;
import capaDAOCC.CampanaProgramadaDAO;

/**
 * El reloj de los envios de publicidad programados para una fecha y hora futura.
 *
 * Cada minuto mira crm.campana_programada y dispara las que ya llegaron a su hora, por el mismo
 * camino que un envio inmediato (EnvioPublicidadCtrl.ejecutarProgramada -> crearYEnviar).
 *
 * POR QUE VIVE EN EL CENTRAL Y NO EN UN PROCESO DE SERVICIOS
 *
 * Porque el envio mismo -el publico, Brevo, el correo directo con su espera entre correos y su
 * "no caben dos a la vez"- ya vive aqui. Un proceso aparte tendria que llamar de vuelta al central
 * por HTTP para poder disparar algo, y sumaria una tarea programada de Windows mas que alguien
 * tiene que acordarse de crear en cada servidor.
 *
 * QUE PASA SI EL SERVIDOR ESTA APAGADO A LA HORA
 *
 * Al volver, la programacion sigue en PROGRAMADA y el primer minuto la encuentra "ya llegada": se
 * dispara si esta dentro del plazo de gracia (PUBLICIDADPROGRAMADAGRACIAMIN, 120 por defecto) y
 * si no queda VENCIDA y no se manda. Ver EnvioPublicidadCtrl.ejecutarProgramada.
 *
 * QUE PASA SI SE CAE A MITAD DE DISPARAR
 *
 * Las que quedaron EJECUTANDO se marcan FALLIDA al arrancar y NO se reintentan: pudieron haber
 * alcanzado a mandar parte, y reintentar le escribe dos veces a un cliente.
 *
 * Una sola instancia toma cada programacion aunque dos centrales corrieran el reloj: reclamar() es
 * un UPDATE condicionado, la base decide.
 */
public class ProgramadorPublicidad {

	/** Cada cuanto mira. Un minuto basta: la hora se escoge a nivel de minuto. */
	private static final int SEGUNDOS_ENTRE_REVISIONES = 60;

	/** Cuantas dispara por revision. Cada una puede tardar (arma su publico); no hace falta mas. */
	private static final int MAXIMO_POR_REVISION = 5;

	private static ScheduledExecutorService reloj = null;

	private ProgramadorPublicidad() {
	}

	/** Arranca el reloj. Si ya esta corriendo no hace nada. */
	public static synchronized void arrancar() {
		if (reloj != null && !reloj.isShutdown()) {
			return;
		}
		final Logger logger = Logger.getLogger("log_file");
		try {
			final int interrumpidas = CampanaProgramadaDAO.marcarInterrumpidas();
			if (interrumpidas > 0) {
				logger.warn("ProgramadorPublicidad: " + interrumpidas
						+ " programacion(es) se quedaron EJECUTANDO por un reinicio; quedaron FALLIDA.");
			}
		} catch (final Throwable t) {
			logger.error("ProgramadorPublicidad.arrancar: " + t.toString());
		}

		reloj = Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
			public Thread newThread(final Runnable r) {
				final Thread hilo = new Thread(r, "publicidad-programada");
				hilo.setDaemon(true);
				return (hilo);
			}
		});
		//Con retraso fijo, y con TODO envuelto: una excepcion no capturada cancelaria el reloj para
		//siempre y las programaciones dejarian de salir sin que nadie lo note.
		reloj.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				try {
					revisar();
				} catch (final Throwable t) {
					Logger.getLogger("log_file").error("ProgramadorPublicidad: fallo la revision, " + t.toString());
				}
			}
		}, 30, SEGUNDOS_ENTRE_REVISIONES, TimeUnit.SECONDS);
		logger.info("ProgramadorPublicidad: reloj de envios programados arrancado.");
	}

	public static synchronized void detener() {
		if (reloj != null) {
			reloj.shutdownNow();
			reloj = null;
		}
	}

	private static void revisar() {
		final ArrayList<Long> listas = CampanaProgramadaDAO.idsListas(MAXIMO_POR_REVISION);
		for (final Long id : listas) {
			EnvioPublicidadCtrl.ejecutarProgramada(id.longValue());
		}
	}
}
