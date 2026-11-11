package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import capaDAOCC.AdicionPuntosManualDAO;
import capaDAOCC.RedencionManualDAO;
import capaDAOCC.UsuarioRolDAO;

/**
 * Adicion manual de puntos a un cliente: quien decide, cuanto y por que.
 *
 * ORDEN: primero se registra el ajuste (acumulacion con vigencia, saldo y registro, en una transaccion) y
 * despues se avisa al cliente por correo. Si el correo falla NO se deshace nada: el ajuste ya esta hecho y
 * correcto; se dice en la respuesta para que quien lo hizo avise por otro medio.
 *
 * El motivo es obligatorio y de verdad: es lo que lee el cliente en el correo y lo que queda escrito para
 * explicar el ajuste dentro de seis meses.
 */
public class AdicionPuntosManualCtrl {

	/** Rol que da acceso a la pantalla. Un solo miembro: Juan David Botero. */
	public static final String ROL = "Ajuste Manual de Puntos";

	/** Un motivo mas corto que esto no explica nada. */
	private static final int MOTIVO_MINIMO = 15;

	/** Este usuario puede usar la pantalla? Se lee de la base en cada peticion. */
	public static boolean puede(final int idUsuario) {
		if (idUsuario <= 0) {
			return (false);
		}
		final ArrayList<String> roles = UsuarioRolDAO.listarNombresRolPorUsuario(idUsuario);
		if (roles == null) {
			return (false);
		}
		for (int i = 0; i < roles.size(); i++) {
			if (ROL.equalsIgnoreCase(roles.get(i))) {
				return (true);
			}
		}
		return (false);
	}

	/** Lo que la pantalla muestra antes de confirmar: existe, esta activo, nombre, saldo, vigencia y tope. */
	@SuppressWarnings("unchecked")
	public String consultar(final String correo) {
		final JSONObject respuesta = new JSONObject();
		final RedencionManualDAO.Cliente cliente = RedencionManualDAO.consultar(correo);
		if (!cliente.existe) {
			respuesta.put("respuesta", "NOEXISTE");
			respuesta.put("detalle", "Ese correo no esta en el plan de fidelizacion.");
			return (respuesta.toJSONString());
		}
		respuesta.put("respuesta", "OK");
		respuesta.put("correo", cliente.correo);
		respuesta.put("nombre", cliente.nombre);
		respuesta.put("activo", cliente.activo);
		respuesta.put("puntos", Double.valueOf(cliente.puntos));
		respuesta.put("diasvigencia", Integer.valueOf(AdicionPuntosManualDAO.diasVigencia()));
		respuesta.put("tope", Double.valueOf(AdicionPuntosManualDAO.topePorOperacion()));
		return (respuesta.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public String adicionar(final String correo, final double puntos, final String motivo, final String usuario) {
		final JSONObject respuesta = new JSONObject();

		if (correo == null || correo.trim().length() == 0) {
			return (malo(respuesta, "Falta el correo del cliente."));
		}
		if (puntos <= 0) {
			return (malo(respuesta, "Los puntos a adicionar tienen que ser mayores que cero."));
		}
		final double tope = AdicionPuntosManualDAO.topePorOperacion();
		if (puntos > tope) {
			//No es un limite de negocio: es un freno al dedo. 20000 en vez de 2000 se digita facil.
			return (malo(respuesta, "Son " + formatear(puntos) + " puntos en una sola operacion; el tope es "
					+ formatear(tope) + ". Si es correcto, haga dos ajustes o suba el tope (TOPEAJUSTEPUNTOS)."));
		}
		if (motivo == null || motivo.trim().length() < MOTIVO_MINIMO) {
			return (malo(respuesta, "Escriba el motivo (minimo " + MOTIVO_MINIMO + " caracteres). Es lo que queda "
					+ "registrado y lo que lee el cliente en el correo."));
		}

		final AdicionPuntosManualDAO.Resultado hecho =
				AdicionPuntosManualDAO.adicionar(correo.trim(), puntos, motivo.trim(), usuario);
		if (!hecho.exitoso) {
			return (malo(respuesta, hecho.error));
		}

		final String avisoError = utilidadesCC.AvisoAdicionPuntos.enviarAhora(correo.trim(), puntos,
				hecho.saldoDespues, motivo.trim(), hecho.fechaVencimiento);
		AdicionPuntosManualDAO.marcarCorreo(hecho.idAjuste, avisoError.length() == 0, avisoError);

		respuesta.put("respuesta", "OK");
		respuesta.put("idajuste", Integer.valueOf(hecho.idAjuste));
		respuesta.put("puntos", Double.valueOf(puntos));
		respuesta.put("saldoantes", Double.valueOf(hecho.saldoAntes));
		respuesta.put("saldo", Double.valueOf(hecho.saldoDespues));
		respuesta.put("vencen", hecho.fechaVencimiento);
		respuesta.put("diasvigencia", Integer.valueOf(hecho.diasVigencia));
		respuesta.put("correoenviado", Boolean.valueOf(avisoError.length() == 0));
		respuesta.put("detalle", "Se adicionaron " + formatear(puntos) + " puntos. Ahora tiene "
				+ formatear(hecho.saldoDespues) + ", vigentes hasta el " + hecho.fechaVencimiento + "."
				+ (avisoError.length() == 0
						? " Se le envio el correo."
						: " NO se pudo enviar el correo: " + avisoError
						+ " El ajuste si quedo hecho; hay que avisarle al cliente por otro medio."));
		return (respuesta.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private String malo(final JSONObject respuesta, final String detalle) {
		respuesta.put("respuesta", "NOK");
		respuesta.put("detalle", detalle);
		return (respuesta.toJSONString());
	}

	private String formatear(final double puntos) {
		if (puntos == Math.floor(puntos) && !Double.isInfinite(puntos)) {
			return (new java.text.DecimalFormat("###,###").format(puntos));
		}
		return (new java.text.DecimalFormat("###,###.##").format(puntos));
	}
}
