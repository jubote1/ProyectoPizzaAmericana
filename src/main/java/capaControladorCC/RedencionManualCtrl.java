package capaControladorCC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.RedencionManualDAO;
import capaModeloCC.Correo;
import utilidadesCC.ControladorEnvioCorreo;
import utilidadesCC.PlantillaCorreoRedencionManual;

/**
 * Redencion manual de puntos.
 *
 * Para cuando la tienda no pudo hacer la redencion por el camino normal -se
 * cayo la conexion, el producto no estaba en el catalogo, el pedido ya se habia
 * cerrado- y le toca a administracion moverle los puntos al cliente y mandarle
 * el producto.
 *
 * EL ORDEN DE LAS OPERACIONES, Y POR QUE ESE
 *
 *   1. Se descuentan los puntos, con el saldo validado dentro del UPDATE.
 *   2. Se registra la redencion con su motivo.
 *   3. Se le avisa al cliente por correo.
 *
 * Si falla el paso 2 se devuelven los puntos: un descuento sin registro es
 * plata que desaparecio del saldo de alguien sin que quede escrito por que, y
 * eso es peor que no haber hecho nada.
 *
 * Si falla el paso 3 NO se devuelve nada. El producto ya se entrego y la
 * redencion ya quedo registrada; deshacerla porque el correo no salio dejaria
 * el saldo mintiendo. Lo que se hace es DECIRLO en la respuesta, para que quien
 * la proceso sepa que tiene que avisarle al cliente por otro lado. Un correo
 * que se creyo enviado y no salio es justo el caso que despues nadie puede
 * explicar.
 */
public class RedencionManualCtrl {

	/** Mas que esto en una sola operacion casi siempre es un dedo de mas. */
	private static final double PUNTOS_MAXIMOS = 5000;

	/**
	 * Lo que la pantalla necesita para mostrar antes de confirmar: si el cliente
	 * existe, si esta activo, como se llama y cuantos puntos tiene.
	 */
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
		return (respuesta.toJSONString());
	}

	/** Las tiendas, para decir donde se entrego el producto. */
	@SuppressWarnings("unchecked")
	public String tiendas() {
		final JSONObject respuesta = new JSONObject();
		final JSONArray lista = new JSONArray();
		final ArrayList<String[]> tiendas = RedencionManualDAO.obtenerTiendas();
		for (int i = 0; i < tiendas.size(); i++) {
			final JSONObject t = new JSONObject();
			t.put("idtienda", tiendas.get(i)[0]);
			t.put("nombre", tiendas.get(i)[1]);
			lista.add(t);
		}
		respuesta.put("tiendas", lista);
		respuesta.put("respuesta", "OK");
		return (respuesta.toJSONString());
	}

	/**
	 * Hace la redencion.
	 *
	 * @param correo    cliente del plan
	 * @param puntos    puntos a descontar
	 * @param idTienda  tienda donde se entrego; 0 si no aplica
	 * @param tienda    nombre de la tienda, para el correo
	 * @param motivo    por que se hace; obligatorio, y se le muestra al cliente
	 * @param usuario   quien la procesa, tomado de la sesion
	 */
	@SuppressWarnings("unchecked")
	public String redimir(final String correo, final double puntos, final int idTienda,
			final String tienda, final String motivo, final String usuario) {

		final JSONObject respuesta = new JSONObject();

		//---- Lo que se revisa antes de tocar nada -------------------------
		if (correo == null || correo.trim().length() == 0) {
			return (malo(respuesta, "Falta el correo del cliente."));
		}
		if (puntos <= 0) {
			return (malo(respuesta, "Los puntos a redimir tienen que ser mayores que cero."));
		}
		if (puntos > PUNTOS_MAXIMOS) {
			//No es un limite de negocio, es un freno al dedo: 20000 en vez de
			//2000 se digita facil y no hay como deshacerlo despues.
			return (malo(respuesta, "Son " + formatear(puntos) + " puntos en una sola operacion. "
					+ "Si es correcto, haga dos redenciones o avise para subir el tope."));
		}
		if (motivo == null || motivo.trim().length() < 5) {
			//El motivo es el unico lugar donde queda escrito por que
			//administracion le movio los puntos a un cliente. Sin el, dentro de
			//seis meses esta redencion es indistinguible de un error.
			return (malo(respuesta, "Escriba el motivo. Es lo que va a quedar registrado y lo que "
					+ "se le muestra al cliente en el correo."));
		}

		final RedencionManualDAO.Cliente cliente = RedencionManualDAO.consultar(correo.trim());
		if (!cliente.existe) {
			return (malo(respuesta, "Ese correo no esta en el plan de fidelizacion."));
		}
		if (!"S".equalsIgnoreCase(cliente.activo)) {
			return (malo(respuesta, "El cliente esta inactivo en el plan. Hay que activarlo antes de redimir."));
		}
		if (cliente.puntos < puntos) {
			return (malo(respuesta, "El cliente tiene " + formatear(cliente.puntos)
					+ " puntos y se intentan redimir " + formatear(puntos) + "."));
		}

		//---- 1. El descuento ---------------------------------------------
		final double saldo = RedencionManualDAO.redimirConSaldo(cliente.correo, puntos);
		if (saldo < 0) {
			//El saldo alcanzaba hace un instante y ya no. Casi siempre es que la
			//tienda alcanzo a redimir por el camino normal mientras se digitaba.
			return (malo(respuesta, "No se pudo descontar: el saldo cambio mientras se procesaba. "
					+ "Vuelva a consultar el cliente."));
		}

		//---- 2. El registro ----------------------------------------------
		final int idRedencion = RedencionManualDAO.registrar(cliente.correo, puntos, idTienda,
				usuario, motivo.trim());
		if (idRedencion == 0) {
			final boolean devuelto = RedencionManualDAO.devolver(cliente.correo, puntos);
			return (malo(respuesta, "No se pudo registrar la redencion, asi que no se hizo. "
					+ (devuelto ? "Los puntos quedaron como estaban."
							: "OJO: los puntos se descontaron y NO se pudieron devolver. "
							+ "Avise a sistemas con el correo del cliente.")));
		}

		//---- 3. El aviso al cliente ---------------------------------------
		final String fecha = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		final String avisoError = avisar(cliente, puntos, saldo, motivo.trim(), tienda, fecha);

		respuesta.put("respuesta", "OK");
		respuesta.put("idredencion", Integer.valueOf(idRedencion));
		respuesta.put("puntos", Double.valueOf(puntos));
		respuesta.put("saldo", Double.valueOf(saldo));
		respuesta.put("correoenviado", Boolean.valueOf(avisoError.length() == 0));
		respuesta.put("detalle", "Se redimieron " + formatear(puntos) + " puntos. "
				+ "Al cliente le quedan " + formatear(saldo) + "."
				+ (avisoError.length() == 0
						? " Se le envio el correo."
						: " NO se pudo enviar el correo: " + avisoError
						+ " La redencion si quedo hecha; hay que avisarle al cliente por otro medio."));
		return (respuesta.toJSONString());
	}

	/**
	 * Manda el correo. Devuelve vacio si salio, o el motivo si no.
	 *
	 * Nunca lanza: un problema mandando el correo no puede tumbar una redencion
	 * que ya quedo hecha.
	 */
	private String avisar(final RedencionManualDAO.Cliente cliente, final double puntos,
			final double saldo, final String motivo, final String tienda, final String fecha) {
		try {
			if (!ControladorEnvioCorreo.esDireccionValida(cliente.correo)) {
				return ("la direccion [" + cliente.correo + "] no es una direccion valida.");
			}

			//La misma cuenta del aviso de vencimiento de puntos, que es el otro
			//correo del plan de fidelizacion. Si no esta parametrizada se cae a
			//la de siempre, para que el aviso salga igual.
			String cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOVENCIMIENTO");
			String clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOVENCIMIENTO");
			if (cuenta == null || cuenta.trim().length() == 0) {
				cuenta = ParametrosDAO.retornarValorAlfanumerico("CUENTACORREOWOMPI");
				clave = ParametrosDAO.retornarValorAlfanumerico("CLAVECORREOWOMPI");
			}
			if (cuenta == null || cuenta.trim().length() == 0) {
				return ("no hay una cuenta de correo parametrizada.");
			}

			final Correo correo = new Correo();
			correo.setUsuarioCorreo(cuenta);
			correo.setContrasena(clave);
			correo.setAsunto(PlantillaCorreoRedencionManual.asunto());
			correo.setMensaje(PlantillaCorreoRedencionManual.cuerpo(cliente.nombre, puntos, saldo,
					motivo, tienda, fecha));

			final ArrayList destinos = new ArrayList();
			destinos.add(cliente.correo);

			final ControladorEnvioCorreo envio = new ControladorEnvioCorreo(correo, destinos);
			final ControladorEnvioCorreo.ResultadoEnvio resultado = envio.enviarConReintentos();
			if (resultado == ControladorEnvioCorreo.ResultadoEnvio.ENVIADO) {
				return ("");
			}
			return ("el servidor de correo respondio " + resultado + ".");
		} catch (final Exception e) {
			System.out.println("RedencionManualCtrl.avisar: " + e.toString());
			return ("error tecnico enviando el correo.");
		}
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
