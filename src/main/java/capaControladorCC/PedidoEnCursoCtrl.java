package capaControladorCC;


import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
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

	// ------------------------------------------------------------------
	// REANUDAR: abrir el pedido a medio tomar en la pantalla de tomar pedidos
	//
	// Para agregarle productos se usa la MISMA pantalla con la que se toma un pedido (Pedidos.html), no una
	// copia: ahi viven las reglas que hoy se soportan (especialidades por mitad, adiciones, "con" y "sin",
	// excepciones de precio y tope de ingredientes, productos incluidos, ofertas, puntos, descuentos, domicilio).
	// Duplicarlas en otra pantalla las dejaria desparejas a la primera regla nueva.
	//
	// Este metodo solo entrega lo que esa pantalla necesita para continuar el pedido donde quedo: el cliente, la
	// tienda, el tipo de pedido y lo que ya tiene cargado. Y lo RETIENE a nombre de quien lo abre: mientras alguien
	// lo trabaja, nadie mas lo toca. Al finalizar, FinalizarPedido suelta esa retencion (ver el servlet).
	// ------------------------------------------------------------------

	/** Cuanto dura la retencion; la pantalla la renueva cada pocos minutos mientras sigue abierta. */
	private static final int MINUTOS_RENOVACION = 10;

	@SuppressWarnings("unchecked")
	private String error(final String mensaje) {
		final JSONObject r = new JSONObject();
		r.put("ok", "N");
		r.put("mensaje", mensaje);
		return (r.toJSONString());
	}

	/**
	 * Retiene el pedido a nombre de quien lo abre y entrega lo necesario para continuarlo en la pantalla de
	 * tomar pedidos. Solo pedidos en curso (estado 1) que la tienda todavia no tiene.
	 */
	@SuppressWarnings("unchecked")
	public String reanudar(final int idPedido, final String usuario) {
		final PedidoEnCursoDAO.Cabecera c = PedidoEnCursoDAO.cabecera(idPedido);
		if (idPedido <= 0 || !c.existe) {
			return (error("No se indico un pedido valido."));
		}
		if (c.idEstado != 1 || c.numPosHeader > 0) {
			return (error("Este pedido ya no se puede editar: o ya esta en la tienda o alguien lo cerro."));
		}
		final String quien = usuario == null ? "" : usuario.trim();

		final RetencionPedidoDAO.Estado e = RetencionPedidoDAO.consultar(idPedido);
		if (e.retenido) {
			final boolean esMio = RetencionPedidoDAO.POR_EDICION.equals(e.motivo)
					&& quien.equals(e.por == null ? "" : e.por.trim());
			if (!esMio) {
				return (error("Este pedido lo esta trabajando "
						+ (e.por != null && e.por.length() > 0 ? e.por : "otra persona") + " en este momento."));
			}
			RetencionPedidoDAO.renovar(idPedido, quien, MINUTOS_RENOVACION);
		} else if (!RetencionPedidoDAO.retener(idPedido, RetencionPedidoDAO.POR_EDICION, quien, MINUTOS_RENOVACION)) {
			return (error("No se pudo tomar el pedido para editarlo. Intente de nuevo."));
		}

		final JSONObject r = new JSONObject();
		r.put("ok", "S");
		r.put("idpedido", Integer.valueOf(idPedido));
		r.put("idcliente", Integer.valueOf(c.idCliente));
		r.put("telefono", c.telefono);
		r.put("idtienda", Integer.valueOf(c.idTienda));
		r.put("tienda", c.tienda);
		r.put("idtipopedido", Integer.valueOf(c.idTipoPedido));
		r.put("programado", c.programado);
		r.put("horaprogramado", c.horaProgramado);

		final JSONArray lista = new JSONArray();
		for (final PedidoEnCursoDAO.Linea l : PedidoEnCursoDAO.lineas(idPedido)) {
			final JSONObject o = new JSONObject();
			o.put("iddetallepedido", Integer.valueOf(l.idDetalle));
			o.put("nombre", l.producto);
			o.put("tipo", l.tipo);
			o.put("cantidad", Double.valueOf(l.cantidad));
			o.put("especialidad1", l.especialidad1);
			o.put("especialidad2", l.especialidad2);
			o.put("adicion", l.adicion);
			o.put("observacion", l.observacion);
			o.put("valorunitario", Double.valueOf(l.valorUnitario));
			o.put("valortotal", Double.valueOf(l.valorTotal));
			lista.add(o);
		}
		r.put("lineas", lista);
		r.put("total", Double.valueOf(PedidoEnCursoDAO.totalDelDetalle(idPedido)));
		Logger.getLogger("log_file").info("[ReanudarPedidoEnCurso] " + quien + " retomo el pedido " + idPedido
				+ " en la pantalla de tomar pedidos.");
		return (r.toJSONString());
	}

	/** Alarga la retencion. La pantalla lo llama cada tanto mientras la persona sigue trabajando el pedido. */
	@SuppressWarnings("unchecked")
	public String renovar(final int idPedido, final String usuario) {
		final JSONObject r = new JSONObject();
		final boolean ok = RetencionPedidoDAO.renovar(idPedido, usuario, MINUTOS_RENOVACION);
		r.put("ok", ok ? "S" : "N");
		if (!ok) {
			r.put("mensaje", "Se perdio la retencion de este pedido. Vuelva a abrirlo desde la lista.");
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
