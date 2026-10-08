package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.FidelizacionRedencionDAO;
import capaDAOCC.RedencionManualDAO;

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

	/** Con que queda marcada en fidelizacion_redencion. La columna es char(3). */
	private static final String ORIGEN_MANUAL = "ADM";

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

		//---- 1 y 2. El descuento y el registro, en una sola transaccion ----
		//
		//Se usa el MISMO camino que la redencion normal. La primera version de
		//esta pantalla tenia el suyo propio -restaba el saldo y guardaba el
		//registro- y eso dejaba a medias lo mas importante: no repartia el
		//debito entre las acumulaciones de fidelizacion_transaccion.
		//
		//La consecuencia no se ve de inmediato pero es seria: el saldo del
		//cliente baja, pero sus acumulaciones siguen diciendo que esos puntos
		//estan disponibles. Con eso, el correo de vencimiento le avisa al
		//cliente que se le vencen puntos que ya no tiene, y una reversa no
		//sabria de que acumulacion devolverlos.
		//
		//ejecutarRedencion ademas valida el saldo DENTRO de la transaccion, con
		//las filas bloqueadas, y aborta si las acumulaciones no cubren la
		//redencion en vez de agrandar un descuadre que ya existia.
		//
		//El codigo va vacio a proposito: una redencion manual no pasa por codigo
		//de redencion, y ejecutarRedencion lo admite.
		final FidelizacionRedencionDAO.ResultadoRedencion hecho =
				FidelizacionRedencionDAO.ejecutarRedencion("", cliente.correo, puntos, idTienda, 0,
						usuario, ORIGEN_MANUAL, false);
		if (!hecho.exitosa) {
			return (malo(respuesta, "No se pudo redimir: " + hecho.detalleError));
		}
		final int idRedencion = hecho.idRedencion;
		final double saldo = hecho.puntosRestantes;

		//El motivo es lo unico que ejecutarRedencion no guarda, porque las
		//redenciones normales no lo tienen. Se agrega aparte; si fallara, la
		//redencion ya esta hecha y correcta, solo quedaria sin la explicacion.
		RedencionManualDAO.guardarMotivo(idRedencion, motivo.trim());

		//---- 3. El aviso al cliente ---------------------------------------
		//Aca SI se espera a que el correo salga, al reves de las redenciones
		//normales. Esto lo hace una persona sentada frente a la pantalla, que
		//puede esperar dos segundos y que tiene que enterarse si no salio,
		//porque entonces le toca avisarle al cliente por otro lado.
		final String avisoError = utilidadesCC.AvisoRedencion.enviarAhora(
				cliente.correo, puntos, motivo.trim(), idTienda, true);

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
