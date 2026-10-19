package capaControladorCC;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import capaDAOCC.ConfirmacionBotDAO;
import capaDAOCC.ConfirmacionBotDAO.Resumen;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;
import utilidadesCC.ControladorEnvioCorreo;
import utilidadesCC.PlantillaCorreoConfirmacionBot;

/**
 * Le manda al cliente el correo de confirmacion de un pedido hecho por el bot.
 *
 * SOLO PARA EL BOT: es donde la confirmacion a veces falla -el bot no entiende, el pedido se duplica-, y el correo le
 * da al cliente la copia de lo que quedo registrado y el telefono al que llamar si no coincide.
 *
 * NUNCA TUMBA EL PEDIDO. Se llama justo despues de crearlo; todo va en un hilo aparte y dentro de un try: si el correo
 * del cliente esta mal escrito, si Gmail se demora o si falla cualquier cosa, el pedido ya esta hecho y sigue su
 * camino. Por la misma razon no reintenta dos veces el mismo pedido dentro de esta ejecucion del servidor.
 *
 * Sin correo valido no se manda nada: un correo dictado de mala gana que rebota golpea la reputacion de la cuenta
 * desde la que se mandan las facturas y las campanas.
 */
public class ConfirmacionBotCorreoCtrl {

	private static final Logger logger = Logger.getLogger("log_file");

	/** Pedidos a los que ya se les mando (o se intento mandar) el correo, para no mandarlo dos veces. */
	private static final Set<Integer> YA_ENVIADOS = ConcurrentHashMap.newKeySet();

	/**
	 * @param idPedido    el pedido ya creado en el central
	 * @param nombre      el nombre que dio el cliente en el chat
	 * @param correo      el correo que dio el cliente en el chat
	 * @param direccion   la direccion de entrega que dio
	 * @param pagoEnLinea si se paga con el link de pago
	 */
	public static void enviar(final int idPedido, final String nombre, final String correo, final String direccion,
			final boolean pagoEnLinea) {
		try {
			if (idPedido <= 0) {
				return;
			}
			final String destino = correo == null ? "" : correo.trim();
			if (!correoValido(destino)) {
				logger.info("Confirmacion bot pedido " + idPedido + ": sin correo valido ('" + destino + "'), no se envia");
				return;
			}
			if (!YA_ENVIADOS.add(Integer.valueOf(idPedido))) {
				return;
			}
			final Thread hilo = new Thread(new Runnable() {
				public void run() {
					enviarAhora(idPedido, nombre, destino, direccion, pagoEnLinea);
				}
			});
			hilo.setDaemon(true);
			hilo.setName("confirmacion-bot-" + idPedido);
			hilo.start();
		} catch (final Exception e) {
			logger.error("Confirmacion bot pedido " + idPedido + ": " + e);
		}
	}

	private static void enviarAhora(final int idPedido, final String nombre, final String destino,
			final String direccion, final boolean pagoEnLinea) {
		try {
			final Resumen resumen = ConfirmacionBotDAO.obtener(idPedido);
			if (resumen == null) {
				logger.info("Confirmacion bot pedido " + idPedido + ": no se pudo leer el pedido, no se envia");
				return;
			}
			final CorreoElectronico infoCorreo = ControladorEnvioCorreo.recuperarCorreo("CUENTACORREOREPORTES",
					"CLAVECORREOREPORTE");
			final Correo correo = new Correo();
			correo.setAsunto(PlantillaCorreoConfirmacionBot.asunto(idPedido, pagoEnLinea));
			correo.setContrasena(infoCorreo.getClaveCorreo());
			correo.setUsuarioCorreo(infoCorreo.getCuentaCorreo());
			correo.setMensaje(PlantillaCorreoConfirmacionBot.cuerpo(nombre, direccion, resumen, pagoEnLinea));
			final ArrayList<String> destinatarios = new ArrayList<String>();
			destinatarios.add(destino);
			final boolean salio = new ControladorEnvioCorreo(correo, destinatarios).enviarCorreo();
			logger.info("Confirmacion bot pedido " + idPedido + " a " + destino + (salio ? ": enviada" : ": no salio"));
		} catch (final Exception e) {
			logger.error("Confirmacion bot pedido " + idPedido + ": " + e);
		}
	}

	/** Revision minima: una sola arroba, un punto despues y ningun espacio. No verifica que el buzon exista. */
	static boolean correoValido(final String correo) {
		if (correo == null) {
			return false;
		}
		final String c = correo.trim();
		if (c.length() < 6 || c.length() > 200 || c.indexOf(' ') >= 0) {
			return false;
		}
		final int arroba = c.indexOf('@');
		if (arroba <= 0 || arroba != c.lastIndexOf('@')) {
			return false;
		}
		final String dominio = c.substring(arroba + 1);
		final int punto = dominio.indexOf('.');
		return punto > 0 && punto < dominio.length() - 1;
	}
}
