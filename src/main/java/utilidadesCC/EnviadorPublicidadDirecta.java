package utilidadesCC;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import capaDAOCC.CampanaDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;

/**
 * Suelta los correos de una campana de envio directo, de a uno cada tantos
 * segundos.
 *
 * POR QUE VIVE EN EL SERVIDOR Y NO EN EL NAVEGADOR
 *
 * Porque 50 correos cada 30 segundos son 25 minutos. Si el temporizador
 * estuviera en la pantalla, cerrar la pestana -o que se apague el equipo, o que
 * se caiga la red de la oficina- cortaria el envio a la mitad, y nadie sabria
 * cuales salieron y cuales no. Aca el usuario dispara y se va; el servidor
 * termina.
 *
 * Y como el avance se guarda en crm.campana_destinatario de a un correo por
 * vez, si el Tomcat se reinicia a mitad de camino, al volver se reanuda justo
 * donde iba. A nadie se le escribe dos veces.
 *
 * POR QUE DE A UNO Y CON ESPERA
 *
 * Mandar publicidad masiva desde la cuenta de correo de la empresa es la forma
 * mas rapida de que el dominio termine marcado como spam, y de eso no se vuelve
 * facil: despues no llegan ni las facturas ni los correos de pedidos. El tope y
 * la espera son proteccion del dominio, no una limitacion tecnica.
 *
 * Los dos valores viven en general.parametros -PUBLICIDADDIRECTATOPE y
 * PUBLICIDADDIRECTASEGUNDOS- porque son justo lo que operacion va a querer
 * mover cuando vea como se comporta la reputacion, y eso no puede exigir una
 * version nueva.
 */
public class EnviadorPublicidadDirecta {

	/** Si el parametro no esta o viene en cero. */
	private static final int SEGUNDOS_POR_DEFECTO = 30;

	/** Uno solo para toda la aplicacion: dos relojes mandando a la vez romperian la espera. */
	private static ScheduledExecutorService reloj = null;

	/** La tanda que se esta soltando ahora. Cero si no hay ninguna. */
	private static long envioEnCurso = 0;

	private EnviadorPublicidadDirecta() {
	}

	public static int segundosEntreCorreos() {
		int segundos = SEGUNDOS_POR_DEFECTO;
		try {
			final int valor = ParametrosDAO.retornarValorNumerico("PUBLICIDADDIRECTASEGUNDOS");
			if (valor > 0) {
				segundos = valor;
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnviadorPublicidadDirecta.segundos: " + e.toString());
		}
		return (segundos);
	}

	public static int tope() {
		int tope = 50;
		try {
			final int valor = ParametrosDAO.retornarValorNumerico("PUBLICIDADDIRECTATOPE");
			if (valor > 0) {
				tope = valor;
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnviadorPublicidadDirecta.tope: " + e.toString());
		}
		return (tope);
	}

	/**
	 * Arranca el envio de una campana.
	 *
	 * Si ya hay una corriendo NO se encola otra: dos campanas a la vez
	 * duplicarian el ritmo de salida y se perderia justo lo que protege el
	 * intervalo.
	 *
	 * @return "" si arranco, o el motivo por el que no
	 */
	public static synchronized String arrancar(final long idEnvio) {
		if (envioEnCurso != 0 && envioEnCurso != idEnvio) {
			return ("Ya hay un envio de correo directo en curso, el " + envioEnCurso
					+ ". Espere a que termine.");
		}
		envioEnCurso = idEnvio;
		CampanaDAO.cambiarEstado(idEnvio, "ENVIANDO");

		if (reloj == null || reloj.isShutdown()) {
			reloj = Executors.newSingleThreadScheduledExecutor();
			final int segundos = segundosEntreCorreos();
			//Se programa con retraso fijo -y no a frecuencia fija- para que la
			//espera cuente DESPUES de que el correo salio. Con frecuencia fija,
			//un envio que se demore 40 segundos haria que el siguiente saliera
			//de inmediato y se perderia el espaciado.
			reloj.scheduleWithFixedDelay(new Runnable() {
				public void run() {
					try {
						soltarUno();
					} catch (final Throwable t) {
						//Si esta tarea lanza, el programador la cancela para
						//siempre y la campana se queda a medias en silencio.
						Logger.getLogger("log_file").error(
								"EnviadorPublicidadDirecta: fallo soltando, " + t.toString());
					}
				}
			}, 1, segundos, TimeUnit.SECONDS);
		}
		return ("");
	}

	/** Detiene la campana en curso. Lo ya enviado queda enviado. */
	public static synchronized void detener() {
		if (envioEnCurso != 0) {
			CampanaDAO.cambiarEstado(envioEnCurso, "CANCELADA");
			envioEnCurso = 0;
		}
	}

	public static long enCurso() {
		return (envioEnCurso);
	}

	/**
	 * Manda UN correo y marca el resultado.
	 *
	 * Se marca de a uno, antes de pasar al siguiente. Si se marcara al final del
	 * lote y el servidor se cayera en medio, al reanudar se le volveria a
	 * escribir a gente que ya recibio.
	 */
	private static synchronized void soltarUno() {
		if (envioEnCurso == 0) {
			return;
		}
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<CampanaDAO.Destinatario> uno =
				CampanaDAO.pendientes(envioEnCurso, 1);

		if (uno.isEmpty()) {
			logger.info("EnviadorPublicidadDirecta: envio " + envioEnCurso + " terminado.");
			envioEnCurso = 0;
			return;
		}

		final CampanaDAO.Envio tanda = CampanaDAO.obtener(envioEnCurso);
		if (tanda == null) {
			envioEnCurso = 0;
			return;
		}

		final CampanaDAO.Destinatario d = uno.get(0);
		try {
			if (!ControladorEnvioCorreo.esDireccionValida(d.destino)) {
				//Una direccion mal escrita gasta una conexion SMTP y se demora;
				//se descarta antes de intentarlo.
				CampanaDAO.marcar(tanda.idEnvio, d.idPersona, "FALLIDO",
						"La direccion no es valida");
				return;
			}

			final CorreoElectronico cuenta = ControladorEnvioCorreo.recuperarCorreo(
					"CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
			final Correo correo = new Correo();
			correo.setAsunto(tanda.asunto);
			correo.setUsuarioCorreo(cuenta.getCuentaCorreo());
			correo.setContrasena(cuenta.getClaveCorreo());
			correo.setMensaje(personalizar(tanda.cuerpo, d.nombre));

			final ArrayList<String> destinos = new ArrayList<String>();
			destinos.add(d.destino);

			final ControladorEnvioCorreo envio = new ControladorEnvioCorreo(correo, destinos);
			final ControladorEnvioCorreo.ResultadoEnvio resultado = envio.enviarCorreoClasificado();

			if (resultado == ControladorEnvioCorreo.ResultadoEnvio.ENVIADO) {
				CampanaDAO.marcar(tanda.idEnvio, d.idPersona, "ENVIADO", "");
			} else {
				CampanaDAO.marcar(tanda.idEnvio, d.idPersona, "FALLIDO",
						String.valueOf(resultado));
			}
		} catch (final Exception e) {
			logger.error("EnviadorPublicidadDirecta: " + d.destino + ", " + e.toString());
			CampanaDAO.marcar(tanda.idEnvio, d.idPersona, "FALLIDO", e.toString());
		}
	}

	/**
	 * Reemplaza el nombre en el cuerpo.
	 *
	 * Se deja deliberadamente simple -una marca, {{nombre}}- porque el correo
	 * directo es la salida de emergencia, no el canal de campanas grandes. Para
	 * personalizacion de verdad estan las plantillas de Brevo.
	 */
	private static String personalizar(final String cuerpo, final String nombre) {
		if (cuerpo == null) {
			return ("");
		}
		final String saludo = (nombre == null || nombre.trim().length() == 0) ? "" : nombre.trim();
		return (cuerpo.replace("{{nombre}}", saludo));
	}
}
