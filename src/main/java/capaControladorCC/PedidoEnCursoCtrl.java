package capaControladorCC;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaDAOCC.PedidoEnCursoDAO;
import capaDAOCC.RetencionPedidoDAO;

/**
 * Lo que se puede hacer con un pedido que quedo a medio tomar.
 *
 * EL ORDEN IMPORTA Y NO ES OBVIO
 *
 * terminar() SUELTA la retencion antes de finalizar. Tiene que ser asi: el
 * primer paso de FinalizarPedido es pedirle turno a EnvioTiendaDAO, y
 * EnvioTiendaDAO niega el turno a los pedidos retenidos -justamente para que
 * nadie se los lleve mientras alguien los trabaja-. Si no se suelta antes, el
 * pedido se niega a si mismo y la pantalla muestra un error incomprensible.
 *
 * EL TOTAL SALE DEL DETALLE, NO DEL PEDIDO
 *
 * En un pedido a medio tomar total_neto esta en CERO: ese campo lo llena la
 * finalizacion. Finalizar con cero mandaria a cocina un pedido que no se le
 * cobra a nadie. El valor bueno es la suma de detalle_pedido.
 */
public class PedidoEnCursoCtrl {

	/** Toma el pedido para trabajarlo. */
	public String retener(final int idPedido, final String usuario, final int minutos) {
		final JSONObject r = new JSONObject();
		if (idPedido <= 0) {
			r.put("ok", "N");
			r.put("mensaje", "No se indico cual pedido.");
			return (r.toJSONString());
		}
		if (RetencionPedidoDAO.retener(idPedido, RetencionPedidoDAO.POR_EDICION, usuario, minutos)) {
			r.put("ok", "S");
			r.put("minutos", Integer.valueOf(minutos));
			return (r.toJSONString());
		}
		//No se pudo: o ya esta en la tienda, o lo tiene otra persona. Se dice
		//cual de las dos, que es lo que la persona necesita saber para decidir
		//si espera o deja de insistir.
		final RetencionPedidoDAO.Estado e = RetencionPedidoDAO.consultar(idPedido);
		r.put("ok", "N");
		if (e.retenido) {
			r.put("mensaje", "Este pedido lo esta trabajando "
					+ (e.por != null && e.por.length() > 0 ? e.por : "otra persona")
					+ " en este momento.");
		} else {
			r.put("mensaje", "Este pedido ya no se puede tomar: o ya esta en la tienda,"
					+ " o alguien lo cerro.");
		}
		return (r.toJSONString());
	}

	/** Suelta el pedido sin terminarlo. */
	public String soltar(final int idPedido) {
		final JSONObject r = new JSONObject();
		RetencionPedidoDAO.liberar(idPedido);
		r.put("ok", "S");
		return (r.toJSONString());
	}

	/**
	 * Le pone forma de pago, lo finaliza y lo manda a la tienda.
	 *
	 * El valor que paga el cliente puede venir en cero: significa "paga
	 * exacto". No se valida contra el total porque en efectivo la persona
	 * puede pagar de mas y la devuelta la calcula la tienda.
	 */
	public String terminar(final int idPedido, final int idCliente, final int idFormaPago,
			final double valorFormaPago, final String usuario) {
		final JSONObject r = new JSONObject();

		if (idPedido <= 0 || idFormaPago <= 0) {
			r.put("ok", "N");
			r.put("mensaje", "Falta el pedido o la forma de pago.");
			return (r.toJSONString());
		}

		//Se vuelve a mirar que siga siendo nuestro. Entre que se retuvo y que
		//la persona escogio la forma de pago pudo vencerse la retencion y
		//habersela llevado otro.
		final RetencionPedidoDAO.Estado estado = RetencionPedidoDAO.consultar(idPedido);
		if (!estado.retenido) {
			r.put("ok", "N");
			r.put("mensaje", "Se vencio el tiempo para terminar este pedido."
					+ " Vuelva a escogerlo de la lista.");
			return (r.toJSONString());
		}

		final double total = PedidoEnCursoDAO.totalDelDetalle(idPedido);
		if (total <= 0) {
			//Sin productos no hay nada que mandar a cocina. Esto no es un
			//error raro: un pedido se puede haber abierto y abandonado sin
			//cargarle nada.
			r.put("ok", "N");
			r.put("mensaje", "Este pedido no tiene productos cargados."
					+ " No se puede terminar; descartelo.");
			return (r.toJSONString());
		}

		final double valorPaga = valorFormaPago > 0 ? valorFormaPago : total;

		//SE SUELTA ANTES DE FINALIZAR. Ver el comentario de la clase: si no,
		//EnvioTiendaDAO le niega el turno al propio pedido que estamos
		//terminando.
		RetencionPedidoDAO.liberar(idPedido);

		try {
			final PedidoCtrl pedidoCtrl = new PedidoCtrl();
			//insertado = 0 porque el cliente ya existe: este pedido se tomo
			//contra un cliente que ya estaba. validadir en S como lo hace la
			//pantalla normal. Sin descuento ni programacion: terminar un
			//pedido colgado no es el sitio para abrir esas dos puertas.
			final String respuesta = pedidoCtrl.FinalizarPedido(idPedido, idFormaPago, valorPaga,
					total, idCliente, 0, PedidoEnCursoDAO.tiempoDelPedido(idPedido),
					"S", 0, "", "", "");

			PedidoEnCursoDAO.anotarQuienTermino(idPedido, usuario);

			r.put("ok", "S");
			r.put("total", Double.valueOf(total));
			r.put("respuesta", respuesta);
			return (r.toJSONString());
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoCtrl.terminar pedido " + idPedido
					+ ": " + e.toString());
			r.put("ok", "N");
			r.put("mensaje", "No se pudo terminar el pedido. Quedo sin enviar;"
					+ " vuelva a intentarlo.");
			return (r.toJSONString());
		}
	}
}
