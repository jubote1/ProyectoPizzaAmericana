package capaControladorCC;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EspecialidadDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.PedidoEnCursoDAO;
import capaDAOCC.ProductoDAO;
import capaDAOCC.RetencionPedidoDAO;
import capaModeloCC.DetallePedido;
import capaModeloCC.Especialidad;
import capaModeloCC.Producto;
import capaModeloCC.ProductoIncluido;
import capaModeloCC.SaborLiquido;

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
	// EDITAR EL CONTENIDO: ver lo que tiene, quitar un producto, agregar otro
	//
	// Todo pasa por el servidor, y el PRECIO LO CALCULA EL SERVIDOR con las mismas reglas que ya usa el bot de
	// WhatsApp para armar un pedido (precio general del producto, mas el adicional de cada especialidad, mas el
	// adicional del sabor de la bebida si la incluye). El navegador nunca manda un valor: manda que producto,
	// cuantos y con que especialidades.
	//
	// SOLO SE EDITA LO QUE LA PERSONA TIENE RETENIDO. La retencion es la misma que usa Terminar: mientras
	// alguien edita, ni la red de seguridad ni nadie mas se lo lleva a la tienda a medio cambiar.
	//
	// QUE NO HACE (todavia): adiciones, quitar ingredientes ("sin cebolla"), excepciones de precio ni
	// promociones. Esas viven en pedidos.js, que arma el pedido en el navegador; para esas, la persona usa la
	// pantalla de tomar pedidos.
	// ------------------------------------------------------------------

	/** Maximo de unidades por linea: una cifra mayor casi siempre es un error de digitacion. */
	private static final int MAXIMO_CANTIDAD = 20;

	/** Cuanto se alarga la retencion en cada cambio. */
	private static final int MINUTOS_RENOVACION = 10;

	/**
	 * Revisa que el pedido se pueda editar y que sea de quien lo pide. Devuelve null si todo esta bien, o el
	 * JSON de error que hay que responder.
	 */
	private String noSePuedeEditar(final int idPedido, final String usuario,
			final PedidoEnCursoDAO.Cabecera cabecera) {
		if (idPedido <= 0 || !cabecera.existe) {
			return (error("No se indico un pedido valido."));
		}
		if (cabecera.idEstado != 1 || cabecera.numPosHeader > 0) {
			return (error("Este pedido ya no se puede editar: o ya esta en la tienda o alguien lo cerro."));
		}
		final RetencionPedidoDAO.Estado e = RetencionPedidoDAO.consultar(idPedido);
		if (!e.retenido || !RetencionPedidoDAO.POR_EDICION.equals(e.motivo)) {
			return (error("Se vencio el tiempo de edicion. Vuelva a abrir el pedido para editarlo."));
		}
		final String quien = usuario == null ? "" : usuario.trim();
		if (!quien.equals(e.por == null ? "" : e.por.trim())) {
			return (error("Este pedido lo esta editando " + (e.por != null && e.por.length() > 0 ? e.por
					: "otra persona") + "."));
		}
		return (null);
	}

	@SuppressWarnings("unchecked")
	private String error(final String mensaje) {
		final JSONObject r = new JSONObject();
		r.put("ok", "N");
		r.put("mensaje", mensaje);
		return (r.toJSONString());
	}

	/** El detalle del pedido y su total. Es lo que pinta el dialogo de edicion. */
	@SuppressWarnings("unchecked")
	public String lineas(final int idPedido) {
		final JSONObject r = new JSONObject();
		final JSONArray lista = new JSONArray();
		for (final PedidoEnCursoDAO.Linea l : PedidoEnCursoDAO.lineas(idPedido)) {
			final JSONObject o = new JSONObject();
			o.put("iddetalle", Integer.valueOf(l.idDetalle));
			o.put("idproducto", Integer.valueOf(l.idProducto));
			o.put("producto", l.producto);
			o.put("cantidad", Double.valueOf(l.cantidad));
			o.put("valorunitario", Double.valueOf(l.valorUnitario));
			o.put("valortotal", Double.valueOf(l.valorTotal));
			o.put("especialidad1", l.especialidad1);
			o.put("especialidad2", l.especialidad2);
			o.put("adicion", l.adicion);
			o.put("observacion", l.observacion);
			o.put("eshija", Boolean.valueOf(l.esHija));
			lista.add(o);
		}
		r.put("ok", "S");
		r.put("lineas", lista);
		r.put("total", Double.valueOf(PedidoEnCursoDAO.totalDelDetalle(idPedido)));
		return (r.toJSONString());
	}

	/**
	 * Los productos que esa tienda tiene disponibles hoy: los mismos que ve quien toma el pedido en la pantalla
	 * principal, ya filtrados por lo que la tienda tiene bloqueado.
	 */
	@SuppressWarnings("unchecked")
	public String catalogo(final int idPedido) {
		final PedidoEnCursoDAO.Cabecera c = PedidoEnCursoDAO.cabecera(idPedido);
		if (!c.existe) {
			return (error("No se indico un pedido valido."));
		}
		final JSONArray lista = new JSONArray();
		for (final Producto p : ProductoDAO.GetProductosTiendaPlat(c.idTienda, "N")) {
			final JSONObject o = new JSONObject();
			o.put("idproducto", Integer.valueOf(p.getIdProducto()));
			o.put("nombre", p.getNombre());
			o.put("tipo", p.getTipo());
			o.put("precio", Double.valueOf(p.getPreciogeneral()));
			o.put("controlaespecialidades", p.getControlaEspecialidades());
			o.put("incluyeliquido", p.getIncluye_liquido());
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("ok", "S");
		r.put("productos", lista);
		return (r.toJSONString());
	}

	/** Las especialidades y los sabores de bebida que admite un producto en la tienda del pedido. */
	@SuppressWarnings("unchecked")
	public String opciones(final int idPedido, final int idProducto) {
		final PedidoEnCursoDAO.Cabecera c = PedidoEnCursoDAO.cabecera(idPedido);
		if (!c.existe || idProducto <= 0) {
			return (error("No se indico un pedido y un producto validos."));
		}
		final JSONObject r = new JSONObject();
		final JSONArray especialidades = new JSONArray();
		for (final Especialidad e : PedidoDAO.obtenerEspecialidad(0, idProducto)) {
			final JSONObject o = new JSONObject();
			o.put("idespecialidad", Integer.valueOf(e.getIdespecialidad()));
			o.put("nombre", e.getNombre());
			o.put("adicional", Double.valueOf(EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(e.getIdespecialidad(),
					idProducto)));
			especialidades.add(o);
		}
		final JSONArray sabores = new JSONArray();
		for (final SaborLiquido s : PedidoDAO.ObtenerSaboresLiquidoProducto(idProducto, c.idTienda)) {
			final JSONObject o = new JSONObject();
			o.put("idsabor", Integer.valueOf(s.getIdSaborTipoLiquido()));
			o.put("nombre", s.getDescripcionSabor());
			o.put("adicional", Double.valueOf(s.getValorAdicional()));
			sabores.add(o);
		}
		r.put("ok", "S");
		r.put("especialidades", especialidades);
		r.put("sabores", sabores);
		return (r.toJSONString());
	}

	/** Alarga la edicion. La pantalla lo llama cada tanto mientras la persona trabaja. */
	@SuppressWarnings("unchecked")
	public String renovar(final int idPedido, final String usuario) {
		final JSONObject r = new JSONObject();
		final boolean ok = RetencionPedidoDAO.renovar(idPedido, usuario, MINUTOS_RENOVACION);
		r.put("ok", ok ? "S" : "N");
		if (!ok) {
			r.put("mensaje", "Se perdio la edicion de este pedido. Vuelva a abrirlo.");
		}
		return (r.toJSONString());
	}

	/** Agrega un producto al pedido, con el precio calculado aqui. */
	@SuppressWarnings("unchecked")
	public String agregar(final int idPedido, final int idProducto, final int cantidad, final int idEsp1,
			final int idEsp2, final int idSabor, final String observacion, final String usuario) {
		final PedidoEnCursoDAO.Cabecera c = PedidoEnCursoDAO.cabecera(idPedido);
		final String problema = noSePuedeEditar(idPedido, usuario, c);
		if (problema != null) {
			return (problema);
		}
		if (cantidad < 1 || cantidad > MAXIMO_CANTIDAD) {
			return (error("La cantidad debe estar entre 1 y " + MAXIMO_CANTIDAD + "."));
		}

		//El producto tiene que estar disponible en ESA tienda: es la misma lista que ve quien toma el pedido.
		Producto producto = null;
		for (final Producto p : ProductoDAO.GetProductosTiendaPlat(c.idTienda, "N")) {
			if (p.getIdProducto() == idProducto) {
				producto = p;
				break;
			}
		}
		if (producto == null) {
			return (error("Ese producto no esta disponible en la tienda de este pedido."));
		}

		double valorUnitario = producto.getPreciogeneral();

		//Especialidades: solo los productos que las controlan (las pizzas), y solo las que ese producto admite.
		int esp1 = 0;
		int esp2 = 0;
		if ("S".equalsIgnoreCase(producto.getControlaEspecialidades())) {
			if (idEsp1 <= 0) {
				return (error("Escoja la especialidad de la pizza."));
			}
			if (idEsp2 > 0 && idEsp2 == idEsp1) {
				return (error("Las dos mitades no pueden ser la misma especialidad."));
			}
			boolean ok1 = false;
			boolean ok2 = idEsp2 <= 0;
			for (final Especialidad e : PedidoDAO.obtenerEspecialidad(0, idProducto)) {
				if (e.getIdespecialidad() == idEsp1) {
					ok1 = true;
				}
				if (idEsp2 > 0 && e.getIdespecialidad() == idEsp2) {
					ok2 = true;
				}
			}
			if (!ok1 || !ok2) {
				return (error("Esa especialidad no esta disponible para este producto."));
			}
			esp1 = idEsp1;
			esp2 = idEsp2 > 0 ? idEsp2 : 0;
			//Mismas reglas del bot: una sola especialidad suma su adicional completo; dos mitades, la mitad
			//de cada una.
			if (esp2 == 0) {
				valorUnitario += EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(esp1, idProducto);
			} else {
				valorUnitario += EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(esp1, idProducto) / 2;
				valorUnitario += EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(esp2, idProducto) / 2;
			}
		} else if (idEsp1 > 0 || idEsp2 > 0) {
			return (error("Este producto no lleva especialidad."));
		}

		//Bebida incluida: el sabor se guarda en la linea y su adicional -si lo tiene- se suma al precio.
		int sabor = 0;
		if ("S".equalsIgnoreCase(producto.getIncluye_liquido())) {
			if (idSabor <= 0) {
				return (error("Escoja el sabor de la bebida."));
			}
			boolean valido = false;
			for (final SaborLiquido s : PedidoDAO.ObtenerSaboresLiquidoProducto(idProducto, c.idTienda)) {
				if (s.getIdSaborTipoLiquido() == idSabor) {
					valido = true;
					valorUnitario += s.getValorAdicional();
					break;
				}
			}
			if (!valido) {
				return (error("Ese sabor no esta disponible para este producto en la tienda."));
			}
			sabor = idSabor;
		} else if (idSabor > 0) {
			return (error("Este producto no incluye bebida."));
		}

		String obs = observacion == null ? "" : observacion.trim();
		if (obs.length() > 100) {
			obs = obs.substring(0, 100);
		}

		try {
			final DetallePedido linea = new DetallePedido(idProducto, idPedido, cantidad, esp1, esp2, valorUnitario,
					valorUnitario * cantidad, "", obs, sabor, 0, "", "");
			final int idDetalle = PedidoDAO.InsertarDetallePedido(linea);
			if (idDetalle <= 0) {
				return (error("No se pudo agregar el producto. Intente de nuevo."));
			}
			//Los productos que vienen incluidos con ese, como en el resto de canales.
			for (final ProductoIncluido inc : PedidoDAO.obtenerProductosIncluidos()) {
				if (inc.getIdproductopadre() == idProducto) {
					PedidoDAO.InsertarDetallePedido(new DetallePedido(inc.getIdproductohijo(), idPedido,
							inc.getCantidad() * cantidad, 0, 0, 0, 0, "", "Producto Incluido-" + idDetalle, 0, 0, "",
							""));
				}
			}
			RetencionPedidoDAO.renovar(idPedido, usuario, MINUTOS_RENOVACION);
			Logger.getLogger("log_file").info("[EditarPedidoEnCurso] " + usuario + " agrego al pedido " + idPedido
					+ " producto " + idProducto + " x" + cantidad + " a " + valorUnitario);
			return (lineas(idPedido));
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoCtrl.agregar pedido " + idPedido + ": " + e.toString());
			return (error("No se pudo agregar el producto. Intente de nuevo."));
		}
	}

	/** Quita una linea con sus adiciones, modificadores y productos incluidos. */
	public String quitar(final int idPedido, final int idDetalle, final String usuario) {
		final PedidoEnCursoDAO.Cabecera c = PedidoEnCursoDAO.cabecera(idPedido);
		final String problema = noSePuedeEditar(idPedido, usuario, c);
		if (problema != null) {
			return (problema);
		}
		//La linea tiene que ser DE ESTE pedido: el id de la linea viene del navegador.
		PedidoEnCursoDAO.Linea objetivo = null;
		for (final PedidoEnCursoDAO.Linea l : PedidoEnCursoDAO.lineas(idPedido)) {
			if (l.idDetalle == idDetalle) {
				objetivo = l;
				break;
			}
		}
		if (objetivo == null) {
			return (error("Esa linea no es de este pedido."));
		}
		if (objetivo.esHija) {
			return (error("Quite el producto principal: sus adiciones y lo que trae incluido se quitan con el."));
		}
		try {
			for (final Integer incluido : PedidoEnCursoDAO.incluidosDe(idPedido, idDetalle)) {
				PedidoDAO.EliminarDetallePedido(incluido.intValue());
			}
			PedidoDAO.EliminarDetallePedido(idDetalle);
			RetencionPedidoDAO.renovar(idPedido, usuario, MINUTOS_RENOVACION);
			Logger.getLogger("log_file").info("[EditarPedidoEnCurso] " + usuario + " quito del pedido " + idPedido
					+ " la linea " + idDetalle + " (" + objetivo.producto + " x" + objetivo.cantidad + ")");
			return (lineas(idPedido));
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoCtrl.quitar pedido " + idPedido + ": " + e.toString());
			return (error("No se pudo quitar el producto. Intente de nuevo."));
		}
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
