package capaSeguridad.filtro;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import capaDAOCC.CampanaDAO;
import utilidadesCC.EnviadorPublicidadDirecta;

/**
 * Reanuda el envio de correo directo que haya quedado a medio camino cuando Tomcat se cae o se
 * redespliega el war.
 *
 * EL PROBLEMA QUE RESUELVE
 *
 * EnviadorPublicidadDirecta.arrancar programa un ScheduledExecutorService que suelta un correo cada
 * tantos segundos, y su propio comentario decia "si el Tomcat se reinicia a mitad de camino, al volver
 * se reanuda justo donde iba" -pero eso nunca se implemento-. El ScheduledExecutorService es un campo
 * estatico: un redespliegue crea una JVM/classloader nuevo y ese campo nace en null otra vez, mientras
 * que en la base de datos la tanda se quedo en ENVIANDO para siempre. Como
 * EnvioPublicidadCtrl.crearYEnviar no deja abrir una tanda nueva mientras exista una en ENVIANDO
 * (CampanaDAO.envioDirectoEnCurso), el resultado es que UN redespliegue a mitad de un envio bloquea el
 * correo directo hasta que alguien note el problema y lo arregle a mano en la base de datos -paso lo
 * mismo el 2026-09-27 con el envio 11, congelado 47 minutos porque el ultimo redespliegue lo dejo a
 * medias-.
 *
 * QUE HACE
 *
 * Al arrancar la aplicacion, si hay una tanda de correo directo en ENVIANDO, la retoma exactamente donde
 * iba: los destinatarios ya marcados como ENVIADO no se repiten (EnviadorPublicidadDirecta.soltarUno solo
 * toma PENDIENTES). No hace nada si no hay ninguna tanda asi -que sera lo normal-.
 */
@WebListener
public class ReanudarEnvioDirectoListener implements ServletContextListener {

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		try {
			final long idEnvio = CampanaDAO.envioDirectoEnCurso();
			if (idEnvio > 0) {
				final String problema = EnviadorPublicidadDirecta.arrancar(idEnvio);
				final Logger logger = Logger.getLogger("log_file");
				if (problema.length() == 0) {
					logger.info("ReanudarEnvioDirectoListener: se reanudo el envio " + idEnvio
							+ " que quedo en ENVIANDO de un arranque anterior.");
				} else {
					logger.error("ReanudarEnvioDirectoListener: no se pudo reanudar el envio " + idEnvio + ": "
							+ problema);
				}
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ReanudarEnvioDirectoListener.contextInitialized: " + e.toString());
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		//Nada que liberar: el ScheduledExecutorService de EnviadorPublicidadDirecta muere solo con la JVM.
	}
}
