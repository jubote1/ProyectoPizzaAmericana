package capaControladorCC;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.DefinicionSegmentoDAO;
import utilidadesCC.AccesoCRM;

/**
 * Lo que la pantalla de segmentos manda y lo que recibe.
 *
 * Las reglas viajan como un arreglo JSON en un solo parametro y no como
 * campo1, operador1, valor1, campo2...: un segmento puede tener las reglas que
 * necesite y numerar parametros obliga a inventar un tope.
 *
 * NADA DE LO QUE LLEGA AQUI SE DA POR BUENO. La validacion de verdad -la lista
 * blanca de campos, los seis operadores, que un campo numerico traiga un
 * numero- esta en DefinicionSegmentoDAO, que es por donde pasa todo. Aqui solo
 * se traduce.
 */
public class DefinicionSegmentoCtrl {

	/** Nadie construye esto. */
	private DefinicionSegmentoCtrl() {
	}

	// =======================================================================
	// Lo que sale
	// =======================================================================

	@SuppressWarnings("unchecked")
	public static String listar() {
		final JSONObject o = new JSONObject();
		final JSONArray campos = new JSONArray();
		final ArrayList<DefinicionSegmentoDAO.Campo> lc = DefinicionSegmentoDAO.campos();
		for (int i = 0; i < lc.size(); i++) {
			final DefinicionSegmentoDAO.Campo c = lc.get(i);
			final JSONObject j = new JSONObject();
			j.put("campo", c.campo);
			j.put("etiqueta", c.etiqueta);
			j.put("tipo", c.tipo);
			j.put("ayuda", c.ayuda);
			campos.add(j);
		}
		o.put("campos", campos);

		final JSONArray segmentos = new JSONArray();
		final ArrayList<DefinicionSegmentoDAO.Definicion> ld = DefinicionSegmentoDAO.listar();
		for (int i = 0; i < ld.size(); i++) {
			final DefinicionSegmentoDAO.Definicion d = ld.get(i);
			final JSONObject j = new JSONObject();
			j.put("idsegmento", Integer.valueOf(d.idSegmento));
			j.put("nombre", d.nombre);
			j.put("descripcion", d.descripcion);
			j.put("orden", Integer.valueOf(d.orden));
			j.put("color", d.color);
			j.put("activo", d.activo);
			j.put("personas", Integer.valueOf(d.personas));
			j.put("valor", Double.valueOf(d.valor));
			final JSONArray reglas = new JSONArray();
			for (int k = 0; k < d.reglas.size(); k++) {
				final DefinicionSegmentoDAO.Regla r = d.reglas.get(k);
				final JSONObject jr = new JSONObject();
				jr.put("campo", r.campo);
				jr.put("operador", r.operador);
				jr.put("valor", r.valor);
				reglas.add(jr);
			}
			j.put("reglas", reglas);
			segmentos.add(j);
		}
		o.put("segmentos", segmentos);
		o.put("sin_clasificar", Integer.valueOf(DefinicionSegmentoDAO.sinClasificar()));
		return (o.toJSONString());
	}

	// =======================================================================
	// Lo que entra
	// =======================================================================

	/**
	 * Lee el arreglo de reglas. Lo que venga mal armado se devuelve vacio, y el
	 * DAO lo rechaza con un mensaje: mejor eso que guardar medio segmento.
	 */
	public static ArrayList<DefinicionSegmentoDAO.Regla> reglasDe(final String json) {
		final ArrayList<DefinicionSegmentoDAO.Regla> reglas =
				new ArrayList<DefinicionSegmentoDAO.Regla>();
		if (json == null || json.trim().length() == 0) {
			return (reglas);
		}
		try {
			final Object leido = new JSONParser().parse(json);
			if (!(leido instanceof JSONArray)) {
				return (reglas);
			}
			final JSONArray arreglo = (JSONArray) leido;
			for (int i = 0; i < arreglo.size(); i++) {
				final Object item = arreglo.get(i);
				if (!(item instanceof JSONObject)) {
					continue;
				}
				final JSONObject j = (JSONObject) item;
				final DefinicionSegmentoDAO.Regla r = new DefinicionSegmentoDAO.Regla();
				r.campo = texto(j.get("campo"));
				r.operador = texto(j.get("operador"));
				r.valor = texto(j.get("valor"));
				reglas.add(r);
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoCtrl.reglasDe: " + e.toString());
		}
		return (reglas);
	}

	@SuppressWarnings("unchecked")
	public static String probar(final HttpServletRequest request) {
		final ArrayList<DefinicionSegmentoDAO.Regla> reglas =
				reglasDe(request.getParameter("reglas"));
		final int id = entero(request.getParameter("idsegmento"), 0);
		final int orden = entero(request.getParameter("orden"), 100);
		final DefinicionSegmentoDAO.Prueba p = DefinicionSegmentoDAO.probar(reglas, id, orden);

		final JSONObject o = new JSONObject();
		if (p.error.length() > 0) {
			o.put("error", p.error);
			return (o.toJSONString());
		}
		o.put("condicion", p.condicion);
		o.put("cumplen", Integer.valueOf(p.cumplen));
		o.put("quedarian", Integer.valueOf(p.quedarian));
		o.put("contactables", Integer.valueOf(p.contactables));
		o.put("valor", Double.valueOf(p.valor));
		return (o.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String guardar(final HttpServletRequest request) {
		final DefinicionSegmentoDAO.Definicion d = new DefinicionSegmentoDAO.Definicion();
		d.idSegmento = entero(request.getParameter("idsegmento"), 0);
		d.nombre = texto(request.getParameter("nombre"));
		d.descripcion = texto(request.getParameter("descripcion"));
		d.orden = entero(request.getParameter("orden"), 100);
		d.color = texto(request.getParameter("color"));
		d.activo = "N".equalsIgnoreCase(request.getParameter("activo")) ? "N" : "S";
		d.reglas = reglasDe(request.getParameter("reglas"));

		final String error = DefinicionSegmentoDAO.guardar(d, usuario(request));
		final JSONObject o = new JSONObject();
		if (error.length() > 0) {
			o.put("error", error);
		} else {
			o.put("ok", Boolean.TRUE);
			o.put("idsegmento", Integer.valueOf(d.idSegmento));
		}
		return (o.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String borrar(final HttpServletRequest request) {
		final int id = entero(request.getParameter("idsegmento"), 0);
		final String error = DefinicionSegmentoDAO.borrar(id, usuario(request));
		final JSONObject o = new JSONObject();
		if (error.length() > 0) {
			o.put("error", error);
		} else {
			o.put("ok", Boolean.TRUE);
		}
		return (o.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String clasificar(final HttpServletRequest request) {
		final long inicio = System.currentTimeMillis();
		final String error = DefinicionSegmentoDAO.clasificar(usuario(request));
		final JSONObject o = new JSONObject();
		if (error.length() > 0) {
			o.put("error", error);
			return (o.toJSONString());
		}
		o.put("ok", Boolean.TRUE);
		o.put("segundos", Long.valueOf((System.currentTimeMillis() - inicio) / 1000));
		//El reparto nuevo NO se devuelve aqui: la pantalla vuelve a pedir la
		//lista. Son dos viajes en vez de uno, pero el que importa -el de un
		//minuto- ya termino, y asi hay un solo sitio que arma esa respuesta.
		return (o.toJSONString());
	}

	// =======================================================================

	private static String usuario(final HttpServletRequest request) {
		final String u = AccesoCRM.usuarioEnSesion(request);
		return (u == null ? "?" : u);
	}

	private static String texto(final Object o) {
		return (o == null ? "" : o.toString().trim());
	}

	private static int entero(final String s, final int porDefecto) {
		try {
			return (Integer.parseInt(s.trim()));
		} catch (final Exception e) {
			return (porDefecto);
		}
	}
}
