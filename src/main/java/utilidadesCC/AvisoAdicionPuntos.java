package utilidadesCC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.RedencionManualDAO;
import capaModeloCC.Correo;

/**
 * Le avisa al cliente que se le adicionaron puntos de forma manual.
 *
 * Espera a que el correo salga y devuelve si salio: lo hace una persona sentada frente a la pantalla, que
 * puede esperar dos segundos y TIENE que enterarse si no salio, porque entonces le toca avisarle al cliente
 * por otro lado. Nunca lanza: una falla del correo no puede deshacer un ajuste que ya quedo registrado.
 * Usa la misma cuenta de correo que el aviso de redencion y el de vencimiento de puntos.
 */
public final class AvisoAdicionPuntos {

	private AvisoAdicionPuntos() {
	}

	/** Devuelve vacio si el correo salio, o el motivo si no. */
	public static String enviarAhora(final String correo, final double puntos, final double saldo,
			final String motivo, final String fechaVencimiento) {
		try {
			if (correo == null || correo.trim().length() == 0) {
				return ("no hay correo del cliente.");
			}
			if (!ControladorEnvioCorreo.esDireccionValida(correo.trim())) {
				return ("la direccion [" + correo.trim() + "] no es una direccion valida.");
			}
			final PlantillaCorreoAdicionPuntos.Datos datos = new PlantillaCorreoAdicionPuntos.Datos();
			datos.puntos = puntos;
			datos.saldo = saldo;
			datos.motivo = (motivo == null) ? "" : motivo;
			datos.fechaVencimiento = (fechaVencimiento == null) ? "" : fechaVencimiento;
			datos.fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			datos.nombre = RedencionManualDAO.consultar(correo.trim()).nombre;

			String cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOVENCIMIENTO");
			String clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOVENCIMIENTO");
			if (cuenta == null || cuenta.trim().length() == 0) {
				cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
				clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
			}
			if (cuenta == null || cuenta.trim().length() == 0) {
				return ("no hay una cuenta de correo parametrizada.");
			}

			final Correo mensaje = new Correo();
			mensaje.setUsuarioCorreo(cuenta);
			mensaje.setContrasena(clave);
			mensaje.setAsunto(PlantillaCorreoAdicionPuntos.asunto());
			mensaje.setMensaje(PlantillaCorreoAdicionPuntos.cuerpo(datos));

			final ArrayList destinos = new ArrayList();
			destinos.add(correo.trim());
			final ControladorEnvioCorreo envio = new ControladorEnvioCorreo(mensaje, destinos);
			final ControladorEnvioCorreo.ResultadoEnvio resultado = envio.enviarConReintentos();
			if (resultado == ControladorEnvioCorreo.ResultadoEnvio.ENVIADO) {
				return ("");
			}
			return ("el servidor de correo respondio " + resultado + ".");
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AvisoAdicionPuntos.enviarAhora: " + e.toString());
			return ("error tecnico enviando el correo.");
		}
	}
}
