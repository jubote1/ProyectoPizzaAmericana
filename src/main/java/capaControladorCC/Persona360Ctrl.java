package capaControladorCC;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.Persona360DAO;

/**
 * Arma el JSON de la vista 360 de una persona.
 *
 * Todo se pide por idpersona y no por idcliente. Es la diferencia de fondo del
 * CRM: el cliente es una fila, la persona es quien compra.
 */
public class Persona360Ctrl {

	@SuppressWarnings("unchecked")
	public static String buscar(final String texto) {
		final JSONObject respuesta = new JSONObject();
		if (texto == null || texto.trim().length() < 3) {
			respuesta.put("error", "Escriba al menos tres caracteres.");
			respuesta.put("personas", new JSONArray());
			return (respuesta.toJSONString());
		}
		final JSONArray lista = new JSONArray();
		for (final Persona360DAO.Resumen r : Persona360DAO.buscar(texto)) {
			final JSONObject o = new JSONObject();
			o.put("idpersona", r.idPersona);
			o.put("nombre", r.nombre);
			o.put("apellido", r.apellido);
			o.put("celular", r.celular);
			o.put("email", r.email);
			o.put("pedidos", r.pedidos);
			o.put("ultimo", r.ultimoPedido);
			lista.add(o);
		}
		respuesta.put("personas", lista);
		//Se dice como se busco para que la pantalla pueda explicar por que no
		//encontro: un numero que no es celular colombiano no falla, no existe.
		respuesta.put("busco_por", tipoDeBusqueda(texto.trim()));
		return (respuesta.toJSONString());
	}

	private static String tipoDeBusqueda(final String texto) {
		if (texto.indexOf('@') > 0) {
			return ("correo");
		}
		if (Persona360DAO.normalizarCelular(texto) != null) {
			return ("celular");
		}
		boolean hayLetra = false;
		for (int i = 0; i < texto.length(); i++) {
			final char c = texto.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				hayLetra = true;
				break;
			}
		}
		return (hayLetra ? "nombre" : "celular no valido");
	}

	@SuppressWarnings("unchecked")
	public static String resumen() {
		final Persona360DAO.Resumen360 r = Persona360DAO.resumen();
		final JSONObject o = new JSONObject();
		o.put("personas", r.personas);
		o.put("alias", r.alias);
		o.put("central_colgados", r.centralColgados);
		o.put("central_sueltos", r.centralSueltos);
		o.put("tienda_colgados", r.tiendaColgados);
		o.put("tienda_sueltos", r.tiendaSueltos);
		o.put("ultima_persona", r.ultimaPersona);
		final int totalCentral = r.centralColgados + r.centralSueltos;
		final int totalTienda = r.tiendaColgados + r.tiendaSueltos;
		o.put("cobertura_central", totalCentral > 0
				? Math.round(r.centralColgados * 1000.0 / totalCentral) / 10.0 : 0);
		o.put("cobertura_tienda", totalTienda > 0
				? Math.round(r.tiendaColgados * 1000.0 / totalTienda) / 10.0 : 0);
		return (o.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String consultar(final long idPersona) {
		final JSONObject respuesta = new JSONObject();
		if (idPersona <= 0) {
			respuesta.put("error", "Falta la persona.");
			return (respuesta.toJSONString());
		}
		final Persona360DAO.Detalle d = Persona360DAO.obtener(idPersona);
		if (d == null) {
			respuesta.put("error", "No se encontro esa persona en el maestro.");
			return (respuesta.toJSONString());
		}

		final JSONObject p = new JSONObject();
		p.put("idpersona", d.idPersona);
		p.put("nombre", d.nombre);
		p.put("apellido", d.apellido);
		p.put("celular", d.celular);
		p.put("email", d.email);
		p.put("politica_datos", d.politicaDatos);
		p.put("origen", d.origen);
		p.put("filas_central", d.filasCentral);
		p.put("filas_tienda", d.filasTienda);
		p.put("tiendas", d.tiendas);
		p.put("pedidos", d.pedidos);
		p.put("valor_comprado", d.valorComprado);
		p.put("ticket_promedio", d.pedidos > 0 ? Math.round(d.valorComprado / d.pedidos) : 0);
		p.put("primer_pedido", d.primerPedido);
		p.put("ultimo_pedido", d.ultimoPedido);
		p.put("pedidos_cancelados", d.pedidosCancelados);
		p.put("tienda_habitual", d.tiendaHabitual);
		p.put("ofertas_asignadas", d.ofertasAsignadas);
		p.put("ofertas_usadas", d.ofertasUsadas);
		p.put("pqrs", d.pqrs);
		p.put("ultima_pqrs", d.ultimaPqrs);
		p.put("encuestas", d.encuestas);
		p.put("giros_ruleta", d.girosRuleta);
		p.put("puntos", d.puntos);
		respuesta.put("persona", p);

		final JSONArray caras = new JSONArray();
		for (final Persona360DAO.Cara c : Persona360DAO.caras(d.idPersona)) {
			final JSONObject o = new JSONObject();
			o.put("idcliente", c.idCliente);
			o.put("idtienda", c.idTienda);
			o.put("nombre", c.nombre);
			o.put("apellido", c.apellido);
			o.put("telefono", c.telefono);
			o.put("celular", c.celular);
			o.put("email", c.email);
			o.put("direccion", c.direccion);
			o.put("origen", c.origen);
			caras.add(o);
		}
		respuesta.put("caras", caras);

		final JSONArray pedidos = new JSONArray();
		for (final Persona360DAO.Pedido pe : Persona360DAO.pedidos(d.idPersona)) {
			final JSONObject o = new JSONObject();
			o.put("idpedido", pe.idPedido);
			o.put("idtienda", pe.idTienda);
			o.put("fecha", pe.fecha);
			o.put("valor", pe.valor);
			o.put("tipo", pe.tipo);
			o.put("cancelado", pe.cancelado);
			pedidos.add(o);
		}
		respuesta.put("pedidos", pedidos);

		final JSONArray ofertas = new JSONArray();
		for (final Persona360DAO.Oferta of : Persona360DAO.ofertas(d.idPersona)) {
			final JSONObject o = new JSONObject();
			o.put("oferta", of.oferta);
			o.put("ingreso", of.fechaIngreso);
			o.put("utilizada", of.utilizada);
			o.put("uso", of.fechaUso);
			o.put("codigo", of.codigo);
			ofertas.add(o);
		}
		respuesta.put("ofertas", ofertas);

		final JSONArray pqrs = new JSONArray();
		for (final Persona360DAO.Pqrs q : Persona360DAO.pqrs(d.idPersona)) {
			final JSONObject o = new JSONObject();
			o.put("idsolicitud", q.idSolicitud);
			o.put("fecha", q.fecha);
			o.put("idtienda", q.idTienda);
			o.put("tipo", q.tipo);
			o.put("comentario", q.comentario);
			o.put("estado", q.estado);
			pqrs.add(o);
		}
		respuesta.put("pqrs", pqrs);

		return (respuesta.toJSONString());
	}
}
