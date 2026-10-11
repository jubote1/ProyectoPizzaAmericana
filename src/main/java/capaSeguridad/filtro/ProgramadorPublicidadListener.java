package capaSeguridad.filtro;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;

import utilidadesCC.ProgramadorPublicidad;

/**
 * Arranca con la aplicacion el reloj de los envios de publicidad programados, y lo apaga al
 * descargarla.
 *
 * Se apaga al descargar -a diferencia del reloj del correo directo, que muere solo con la JVM- porque
 * un redespliegue del war no reinicia Tomcat: sin parar el reloj viejo, quedarian dos corriendo, cada
 * uno en su classloader, mirando la misma tabla.
 */
@WebListener
public class ProgramadorPublicidadListener implements ServletContextListener {

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		try {
			ProgramadorPublicidad.arrancar();
		} catch (final Throwable t) {
			Logger.getLogger("log_file").error("ProgramadorPublicidadListener.contextInitialized: " + t.toString());
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
		try {
			ProgramadorPublicidad.detener();
		} catch (final Throwable t) {
			Logger.getLogger("log_file").error("ProgramadorPublicidadListener.contextDestroyed: " + t.toString());
		}
	}
}
