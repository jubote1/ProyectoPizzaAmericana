package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.BonoRecompraDAO;

/**
 * La pantalla del bono de recompra.
 *
 * Lo unico que hace de verdad es definir la regla. El calculo lo hace el
 * barrido nocturno de Servicios, que es el unico que ve las compras de
 * mostrador; desde aqui se puede pedir el cierre a mano, pero solo cuenta lo
 * que el barrido ya trajo.
 */
public class BonoRecompraCtrl {

	@SuppressWarnings("unchecked")
	public static String listar() {
		final JSONArray lista = new JSONArray();
		for (final BonoRecompraDAO.Campana c : BonoRecompraDAO.listar(false)) {
			final JSONObject o = new JSONObject();
			o.put("idbono", c.idBono);
			o.put("nombre", c.nombre);
			o.put("idoferta", c.idOferta);
			o.put("compra_desde", c.compraDesde);
			o.put("compra_hasta", c.compraHasta);
			o.put("porcentaje", c.porcentaje);
			o.put("tope_bono", c.topeBono);
			o.put("base_minima", c.baseMinima);
			o.put("productos", c.productos);
			o.put("excluir_promociones", c.excluirPromociones ? "S" : "N");
			o.put("repetible", c.repetible ? "S" : "N");
			o.put("abierta", c.abierta ? "S" : "N");
			o.put("idenvio", c.idEnvio);
			o.put("idcampana", c.idCampana);
			o.put("fecha_emision", c.fechaEmision);
			o.put("redime_desde", c.redimeDesde);
			o.put("redime_hasta", c.redimeHasta);
			o.put("hora_desde", c.horaDesde);
			o.put("hora_hasta", c.horaHasta);
			o.put("tipos_pedido", c.tiposPedido);
			o.put("estado", c.estado);
			o.put("emitir", c.emitir ? "S" : "N");
			o.put("avisar", c.avisar ? "S" : "N");
			o.put("con_pedidos", c.personasConPedidos);
			o.put("emitidos", c.emitidos);
			o.put("valor_emitido", c.valorEmitido);
			o.put("ultimo_calculo_en", c.ultimoCalculoEn);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("campanas", lista);
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String guardar(final BonoRecompraDAO.Campana c) {
		final JSONObject r = new JSONObject();

		if (c.nombre == null || c.nombre.trim().length() == 0) {
			r.put("error", "La campana necesita un nombre.");
			return (r.toJSONString());
		}
		if (c.idOferta <= 0) {
			r.put("error", "Escoja la oferta con la que se va a emitir el bono.");
			return (r.toJSONString());
		}
		if (c.compraDesde == null || c.compraHasta == null
				|| c.compraDesde.length() < 10 || c.compraHasta.length() < 10) {
			r.put("error", "Ponga las dos fechas de la ventana de compra.");
			return (r.toJSONString());
		}
		if (c.compraHasta.compareTo(c.compraDesde) < 0) {
			r.put("error", "La fecha final es anterior a la inicial.");
			return (r.toJSONString());
		}
		if (c.porcentaje <= 0 || c.porcentaje > 100) {
			r.put("error", "El porcentaje tiene que estar entre 1 y 100.");
			return (r.toJSONString());
		}
		//Sin tope, un pedido empresarial de dos millones genera un bono de
		//doscientos mil. El tope es lo que acota cuanto se puede regalar.
		if (c.topeBono <= 0) {
			r.put("error", "Pongale tope al bono. Sin tope, un pedido grande genera un bono"
					+ " enorme y no hay forma de saber cuanto se puede llegar a regalar.");
			return (r.toJSONString());
		}
		//Una campana por invitacion sin campana de correo no tiene publico.
		//Guardarla asi dejaria un bono que el barrido no va a emitir nunca, y
		//nadie sabria por que.
		if (!c.abierta && c.idCampana <= 0) {
			r.put("error", "La campana es por invitacion: escoja la campana de correo con la que"
					+ " se invita. Cuentan todas sus tandas, asi que puede mandar varios envios"
					+ " y a segmentos distintos.");
			return (r.toJSONString());
		}
		//Las fechas de redencion tienen que ser coherentes entre si y con la
		//emision. Un bono que vence antes de emitirse nace muerto, y eso no se
		//descubre hasta que el cliente lo intenta usar.
		if (c.redimeHasta.length() >= 10) {
			final String emite = c.fechaEmision.length() >= 10 ? c.fechaEmision : c.compraHasta;
			if (c.redimeHasta.compareTo(emite) < 0) {
				r.put("error", "El bono vencerira el " + c.redimeHasta + ", antes de emitirse el "
						+ emite + ". Nadie alcanzaria a usarlo.");
				return (r.toJSONString());
			}
		}
		if (c.redimeDesde.length() >= 10 && c.redimeHasta.length() >= 10
				&& c.redimeHasta.compareTo(c.redimeDesde) < 0) {
			r.put("error", "La redencion termina antes de empezar.");
			return (r.toJSONString());
		}
		//Emitir antes de que cierre la ventana de compra dejaria por fuera las
		//compras de los ultimos dias, que es justo lo que se prometio sumar.
		//
		//Con franja horaria eso deja de ser cierto el ultimo dia: si la compra
		//solo cuenta hasta las 5 pm, emitir esa misma noche esta bien y es lo
		//que se quiere. Por eso la regla se afloja a "no ANTES del ultimo dia"
		//cuando hay hora de cierre.
		final boolean conFranja = c.horaHasta.length() >= 4;
		final boolean emiteMuyPronto = conFranja
				? c.fechaEmision.compareTo(c.compraHasta) < 0
				: c.fechaEmision.compareTo(c.compraHasta) <= 0;
		if (c.fechaEmision.length() >= 10 && emiteMuyPronto) {
			r.put("error", "El bono se emitiria el " + c.fechaEmision + ", antes de que cierre la"
					+ " ventana de compra el " + c.compraHasta + ". Las compras de los ultimos dias"
					+ " no alcanzarian a entrar.");
			return (r.toJSONString());
		}

		//La oferta tiene que poder llevar el valor del bono. Se revisa aqui y no
		//al emitir: descubrirlo de noche, con la ventana ya cerrada, seria tarde.
		final String problema = capaDAOCC.CodigoPromoDAO.problemaParaEnviar(c.idOferta);
		if (problema.length() > 0) {
			r.put("error", "La oferta no sirve para esto: " + problema);
			return (r.toJSONString());
		}
		//Y ademas tiene que admitir SALDO. El bono vale distinto para cada
		//quien; sin redencion parcial ese valor no se respeta al redimir y
		//quien se gano $8.000 usaria la oferta completa. Se revisa aqui, al
		//guardar, y no al emitir: descubrirlo de noche con la ventana ya
		//cerrada seria tarde para corregirlo.
		if (!admiteSaldo(c.idOferta)) {
			r.put("error", "Esa oferta no admite redencion parcial, asi que no puede llevar el valor"
					+ " del bono: quien se gane $8.000 la usaria completa. Marquele"
					+ " 'redencion parcial' en Administrar Ofertas, o escoja otra.");
			return (r.toJSONString());
		}

		final int id = BonoRecompraDAO.guardar(c);
		if (id == 0) {
			r.put("error", "No se pudo guardar la campana.");
			return (r.toJSONString());
		}
		r.put("idbono", id);
		r.put("mensaje", "Campana guardada.");
		return (r.toJSONString());
	}

	private static boolean admiteSaldo(final int idOferta) {
		final ArrayList<capaDAOCC.CodigoPromoDAO.OfertaEnviable> ofertas =
				capaDAOCC.CodigoPromoDAO.ofertasEnviables();
		for (int i = 0; i < ofertas.size(); i++) {
			if (ofertas.get(i).idOferta == idOferta) {
				return ("S".equals(ofertas.get(i).redParcial));
			}
		}
		return (false);
	}

	@SuppressWarnings("unchecked")
	public static String previsualizar(final int idBono) {
		final JSONObject r = new JSONObject();
		if (idBono <= 0) {
			r.put("error", "Falta escoger la campana.");
			return (r.toJSONString());
		}
		final BonoRecompraDAO.Resultado res = BonoRecompraDAO.previsualizar(idBono);
		r.put("califican", res.califican);
		r.put("valor", res.valor);
		r.put("aviso", res.aviso);
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String emisiones(final int idBono, final int cuantas) {
		final JSONArray lista = new JSONArray();
		final ArrayList<BonoRecompraDAO.Emision> emisiones =
				BonoRecompraDAO.emisiones(idBono, cuantas > 0 ? cuantas : 200);
		for (int i = 0; i < emisiones.size(); i++) {
			final BonoRecompraDAO.Emision e = emisiones.get(i);
			final JSONObject o = new JSONObject();
			o.put("idemision", e.idEmision);
			o.put("idpersona", e.idPersona);
			o.put("pedidos", e.pedidos);
			o.put("base", e.base);
			o.put("valor", e.valor);
			o.put("topado", e.topado ? "S" : "N");
			o.put("codigo", e.codigo);
			o.put("fecha_caducidad", e.fechaCaducidad);
			o.put("estado", e.estado);
			o.put("detalle", e.detalle);
			o.put("destino", e.destino);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("emisiones", lista);
		return (r.toJSONString());
	}
}
