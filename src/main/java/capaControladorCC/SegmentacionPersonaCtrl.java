package capaControladorCC;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.SegmentacionPersonaDAO;

/**
 * Arma el JSON de la segmentacion por persona, y el CSV de la descarga.
 *
 * Aqui se traduce lo que manda la pantalla a un filtro, con valores por defecto
 * seguros: lo que no venga o venga mal se toma como "sin filtro", nunca como
 * cero o como una lista vacia que dejaria el resultado en blanco sin decir por
 * que.
 */
public class SegmentacionPersonaCtrl {

	/**
	 * Lee el filtro de la peticion.
	 *
	 * Los dias van con -1 como "sin tope" y no con 0, porque 0 es un valor
	 * legitimo: quien compro hoy tiene dias_sin_comprar = 0.
	 */
	public static SegmentacionPersonaDAO.Filtro filtroDe(final HttpServletRequest request) {
		final SegmentacionPersonaDAO.Filtro f = new SegmentacionPersonaDAO.Filtro();

		final String segmentos = request.getParameter("segmentos");
		if (segmentos != null && segmentos.trim().length() > 0) {
			final String[] partes = segmentos.split(",");
			for (int i = 0; i < partes.length; i++) {
				final String s = partes[i].trim();
				if (s.length() > 0) {
					f.segmentos.add(s);
				}
			}
		}
		f.idTienda = entero(request.getParameter("idtienda"), 0);
		f.pedidosMin = entero(request.getParameter("pedidosmin"), 0);
		f.valorMin = decimal(request.getParameter("valormin"), 0);
		f.diasMin = entero(request.getParameter("diasmin"), -1);
		f.diasMax = entero(request.getParameter("diasmax"), -1);
		f.soloConCorreo = "S".equals(request.getParameter("concorreo"));
		f.soloAutorizados = "S".equals(request.getParameter("autorizados"));
		f.canal = texto(request.getParameter("canal"), "TODOS");
		f.orden = texto(request.getParameter("orden"), "VALOR");
		f.pagina = entero(request.getParameter("pagina"), 1);
		f.porPagina = entero(request.getParameter("porpagina"), 50);
		return (f);
	}

	@SuppressWarnings("unchecked")
	public static String consultar(final HttpServletRequest request) {
		final SegmentacionPersonaDAO.Filtro f = filtroDe(request);
		final SegmentacionPersonaDAO.Resultado r = SegmentacionPersonaDAO.consultar(f);

		final JSONObject o = new JSONObject();
		if (r.error.length() > 0) {
			o.put("error", r.error);
			return (o.toJSONString());
		}
		o.put("personas", Integer.valueOf(r.personas));
		o.put("pedidos", Long.valueOf(r.pedidos));
		o.put("valor", Double.valueOf(r.valor));
		o.put("con_correo", Integer.valueOf(r.conCorreo));
		o.put("autorizados", Integer.valueOf(r.autorizados));
		o.put("pagina", Integer.valueOf(f.pagina));
		o.put("por_pagina", Integer.valueOf(f.porPagina));
		o.put("tope_descarga", Integer.valueOf(SegmentacionPersonaDAO.TOPE_DESCARGA));

		final JSONArray filas = new JSONArray();
		for (int i = 0; i < r.filas.size(); i++) {
			final SegmentacionPersonaDAO.Fila x = r.filas.get(i);
			final JSONObject j = new JSONObject();
			j.put("idpersona", Long.valueOf(x.idPersona));
			j.put("nombre", x.nombre);
			j.put("apellido", x.apellido);
			j.put("celular", x.celular);
			j.put("email", x.email);
			j.put("politica_datos", x.politicaDatos);
			j.put("segmento", x.segmento);
			j.put("pedidos", Integer.valueOf(x.pedidos));
			j.put("pedidos_central", Integer.valueOf(x.pedidosCentral));
			j.put("pedidos_tienda", Integer.valueOf(x.pedidosTienda));
			j.put("valor", Double.valueOf(x.valor));
			j.put("ticket", Double.valueOf(x.ticket));
			j.put("ultimo_pedido", x.ultimoPedido);
			j.put("dias_sin_comprar", Integer.valueOf(x.diasSinComprar));
			j.put("dias_entre_pedidos", Integer.valueOf(x.diasEntrePedidos));
			j.put("tienda_habitual", Integer.valueOf(x.tiendaHabitual));
			j.put("tiendas_distintas", Integer.valueOf(x.tiendasDistintas));
			filas.add(j);
		}
		o.put("filas", filas);

		final JSONArray porSeg = new JSONArray();
		for (int i = 0; i < r.porSegmento.size(); i++) {
			final SegmentacionPersonaDAO.Conteo c = r.porSegmento.get(i);
			final JSONObject j = new JSONObject();
			j.put("segmento", c.segmento);
			j.put("personas", Integer.valueOf(c.personas));
			j.put("valor", Double.valueOf(c.valor));
			porSeg.add(j);
		}
		o.put("por_segmento", porSeg);

		final JSONArray tiendas = new JSONArray();
		for (final SegmentacionPersonaDAO.Tienda t : SegmentacionPersonaDAO.tiendas()) {
			final JSONObject j = new JSONObject();
			j.put("idtienda", Integer.valueOf(t.idTienda));
			j.put("nombre", t.nombre);
			tiendas.add(j);
		}
		o.put("tiendas", tiendas);

		//Los segmentos que existen, para que la pantalla arme sus casillas sola.
		//Antes estaban escritas en el HTML: crear un segmento nuevo obligaba a
		//tocar tres archivos y, si a uno se le olvidaba este, el segmento
		//quedaba sin forma de filtrarse.
		final JSONArray definidos = new JSONArray();
		for (final capaDAOCC.DefinicionSegmentoDAO.Definicion d : capaDAOCC.DefinicionSegmentoDAO.listar()) {
			final JSONObject j = new JSONObject();
			j.put("nombre", d.nombre);
			j.put("descripcion", d.descripcion);
			j.put("color", d.color);
			j.put("activo", d.activo);
			definidos.add(j);
		}
		o.put("segmentos_definidos", definidos);

		final String[] u = SegmentacionPersonaDAO.umbrales();
		o.put("dias_activo", u[0]);
		o.put("dias_riesgo", u[1]);
		o.put("calculado_en", u[2]);
		return (o.toJSONString());
	}

	/**
	 * El CSV.
	 *
	 * Separador punto y coma y no coma: Excel en espanol abre con coma todo en
	 * una sola columna, y quien pide esta lista la abre en Excel.
	 *
	 * El BOM del principio es lo que hace que Excel entienda que es UTF-8. Sin
	 * el, los nombres con tilde salen rotos y parece un problema de la base.
	 */
	public static String csv(final ArrayList<SegmentacionPersonaDAO.Fila> filas) {
		final StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append("idpersona;nombre;apellido;celular;correo;autorizo_datos;segmento;");
		sb.append("pedidos;pedidos_domicilio;pedidos_mostrador;valor;ticket_promedio;");
		sb.append("ultimo_pedido;dias_sin_comprar;dias_entre_pedidos;tienda_habitual;tiendas\n");
		for (int i = 0; i < filas.size(); i++) {
			final SegmentacionPersonaDAO.Fila x = filas.get(i);
			sb.append(x.idPersona).append(';');
			sb.append(limpiar(x.nombre)).append(';');
			sb.append(limpiar(x.apellido)).append(';');
			//El celular va con comilla simple delante para que Excel no lo tome
			//por un numero y lo muestre como 3,00E+09.
			sb.append(x.celular.length() > 0 ? "'" + limpiar(x.celular) : "").append(';');
			sb.append(limpiar(x.email)).append(';');
			sb.append(limpiar(x.politicaDatos)).append(';');
			sb.append(limpiar(x.segmento)).append(';');
			sb.append(x.pedidos).append(';');
			sb.append(x.pedidosCentral).append(';');
			sb.append(x.pedidosTienda).append(';');
			sb.append(Math.round(x.valor)).append(';');
			sb.append(Math.round(x.ticket)).append(';');
			sb.append(limpiar(x.ultimoPedido)).append(';');
			sb.append(x.diasSinComprar < 0 ? "" : String.valueOf(x.diasSinComprar)).append(';');
			sb.append(x.diasEntrePedidos < 0 ? "" : String.valueOf(x.diasEntrePedidos)).append(';');
			sb.append(x.tiendaHabitual == 0 ? "" : String.valueOf(x.tiendaHabitual)).append(';');
			sb.append(x.tiendasDistintas).append('\n');
		}
		return (sb.toString());
	}

	/**
	 * Un nombre con punto y coma partiria la fila en dos columnas, y un salto
	 * de linea la partiria en dos filas. Se cambian por espacio en vez de
	 * entrecomillar todo: es mas simple y estos campos no los necesita nadie
	 * literales.
	 */
	private static String limpiar(final String s) {
		if (s == null) {
			return ("");
		}
		return (s.replace(';', ' ').replace('\n', ' ').replace('\r', ' ').trim());
	}

	private static int entero(final String s, final int porDefecto) {
		try {
			if (s == null || s.trim().length() == 0) {
				return (porDefecto);
			}
			return (Integer.parseInt(s.trim()));
		} catch (final Exception e) {
			return (porDefecto);
		}
	}

	private static double decimal(final String s, final double porDefecto) {
		try {
			if (s == null || s.trim().length() == 0) {
				return (porDefecto);
			}
			//Llega como lo escribio el usuario: puede traer puntos de miles.
			return (Double.parseDouble(s.trim().replace(".", "").replace(",", ".")));
		} catch (final Exception e) {
			return (porDefecto);
		}
	}

	private static String texto(final String s, final String porDefecto) {
		if (s == null || s.trim().length() == 0) {
			return (porDefecto);
		}
		return (s.trim());
	}
}
