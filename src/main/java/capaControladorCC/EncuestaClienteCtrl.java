package capaControladorCC;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.EncuestaClienteDAO;
import capaDAOCC.EncuestaClienteDAO.Envios;
import capaDAOCC.EncuestaClienteDAO.Pregunta;
import capaDAOCC.EncuestaClienteDAO.Respuesta;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Reporte de las encuestas de satisfaccion de los CLIENTES, para el administrador de
 * punto de venta (y para quien vea todas las tiendas).
 *
 * Todo se calcula aqui, en una sola pasada sobre las respuestas del periodo:
 *   - por pregunta: cuantos contestaron cada opcion (la distribucion), el promedio y el
 *     indicador que corresponde a su tipo (valoracion: % buena o muy buena; calificacion
 *     de 1 a 10: promotores, pasivos, detractores e indice de recomendacion; cerrada:
 *     % de si; abierta: cuantos dejaron comentario);
 *   - la comparacion contra la red cuando se mira UNA tienda: solo los promedios de las
 *     once, nunca el detalle de otra tienda;
 *   - los patrones: semana a semana, por tipo de atencion (domicilio o punto de venta),
 *     por dia de la semana, y las palabras que mas se repiten en los comentarios;
 *   - el detalle, una fila por encuesta.
 *
 * Las preguntas salen de pregunta_servicio (id, descripcion y TIPO): no hay ninguna
 * pregunta escrita aqui. Una pregunta nueva configurada ahi aparece sola.
 *
 * Una encuesta es el conjunto de respuestas de un mismo (idtienda, idpedido).
 */
public class EncuestaClienteCtrl {

	/** Tope de filas del detalle, para no mandarle al navegador un ano entero de comentarios. */
	private static final int TOPE_DETALLE = 3000;

	/** Tope del rango consultable, en dias. */
	private static final int TOPE_DIAS = 400;

	private static final String[] DIAS = { "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo" };

	/** Palabras que no dicen nada en un comentario. Ya sin tildes y en minuscula. */
	private static final Set<String> PALABRAS_VACIAS = new HashSet<String>(Arrays.asList(
			"para", "pero", "como", "porque", "este", "esta", "esto", "estos", "estas", "estoy", "estaba", "estan",
			"todo", "todos", "toda", "todas", "mucho", "muchos", "mucha", "muchas", "muy", "mas", "menos", "tambien",
			"tengo", "tiene", "tienen", "tuvo", "hacer", "hace", "hacen", "hay", "ser", "son", "fue", "era", "han",
			"con", "sin", "del", "los", "las", "una", "uno", "unos", "unas", "que", "por", "ese", "esa", "eso",
			"aplica", "ninguno", "ninguna", "nada", "nunca", "siempre", "ahora", "solo", "sino", "cuando", "donde",
			"quien", "cual", "bien", "gracias", "pizza", "pizzas", "servicio", "atencion", "pedido", "ustedes",
			"nosotros", "sobre", "entre", "desde", "hasta", "mejorar", "ellos", "ellas", "algo", "otro", "otra",
			"favor", "seria", "puede", "pueden", "podria", "deberian", "debe", "deben", "cada", "aun", "asi", "les",
			"nos", "mis", "mas", "pues", "voy", "vez", "veces", "dia", "dias", "buen", "buena", "bueno", "buenas",
			"buenos", "excelente", "perfecto", "super", "todo", "tarde", "noche", "hola"));

	/** Una encuesta completa: las respuestas de un pedido. */
	private static class Enc {
		int idTienda;
		int idPedido;
		String tipo = "";
		LocalDateTime fecha;
		final HashMap<Integer, String> r = new HashMap<Integer, String>();
	}

	/** Las cifras de una pregunta sobre un conjunto de encuestas. */
	private static class Estad {
		int n = 0;
		double suma = 0;
		final int[] cuenta = new int[11];
		int si = 0;
		int no = 0;
		int conComentario = 0;

		double promedio() {
			return (n == 0 ? 0d : suma / n);
		}
	}

	@SuppressWarnings("unchecked")
	public String consultar(final int idTienda, final String fechaInicial, final String fechaFinal) {
		final JSONObject respuesta = new JSONObject();
		try {
			if (!fechaValida(fechaInicial) || !fechaValida(fechaFinal)) {
				return (error("Las fechas deben venir en formato aaaa-mm-dd."));
			}
			final LocalDate ini = LocalDate.parse(fechaInicial);
			final LocalDate fin = LocalDate.parse(fechaFinal);
			if (fin.isBefore(ini)) {
				return (error("La fecha final no puede ser anterior a la inicial."));
			}
			if (ini.plusDays(TOPE_DIAS).isBefore(fin)) {
				return (error("El rango es muy largo: consulte hasta " + TOPE_DIAS + " dias."));
			}
			final String hastaExclusivo = fin.plusDays(1).toString();

			final ArrayList<Pregunta> preguntas = EncuestaClienteDAO.obtenerPreguntas();
			final ArrayList<Enc> encuestas = agrupar(EncuestaClienteDAO.obtenerRespuestas(fechaInicial, hastaExclusivo, idTienda));
			//Para comparar: la red entera, solo si se esta mirando una tienda.
			final ArrayList<Enc> encuestasRed = (idTienda > 0)
					? agrupar(EncuestaClienteDAO.obtenerRespuestas(fechaInicial, hastaExclusivo, 0))
					: null;

			final HashMap<Integer, String> nombresTienda = nombresTienda();
			final HashMap<String, String> nombresCliente = EncuestaClienteDAO.obtenerNombres(fechaInicial, hastaExclusivo, idTienda);
			final Envios envios = EncuestaClienteDAO.obtenerEnvios(fechaInicial, hastaExclusivo, idTienda);

			//--- por pregunta
			final JSONArray arregloPreguntas = new JSONArray();
			int conComentario = 0;
			int conInconvenientes = 0;
			for (final Pregunta p : preguntas) {
				final Estad e = estadistica(p, encuestas);
				final JSONObject jp = jsonPregunta(p, e);
				if (encuestasRed != null) {
					jp.put("red", jsonPregunta(p, estadistica(p, encuestasRed)));
				}
				arregloPreguntas.add(jp);
				if ("abierta".equals(p.tipo)) {
					conComentario = Math.max(conComentario, e.conComentario);
				}
				if ("cerrada".equals(p.tipo)) {
					conInconvenientes = Math.max(conInconvenientes, e.si);
				}
			}

			//--- totales
			final JSONObject totales = new JSONObject();
			totales.put("encuestas", Integer.valueOf(encuestas.size()));
			totales.put("encuestasRed", Integer.valueOf(encuestasRed == null ? encuestas.size() : encuestasRed.size()));
			totales.put("enviadas", Integer.valueOf(envios.enviadas));
			totales.put("contestadasDelEnvio", Integer.valueOf(envios.contestadas));
			totales.put("tasaRespuesta", Double.valueOf(envios.enviadas == 0 ? 0d : redondear(100d * envios.contestadas / envios.enviadas)));
			totales.put("conComentario", Integer.valueOf(conComentario));
			totales.put("conInconvenientes", Integer.valueOf(conInconvenientes));

			respuesta.put("resultado", "OK");
			respuesta.put("totales", totales);
			respuesta.put("preguntas", arregloPreguntas);
			respuesta.put("semanas", jsonSemanas(preguntas, encuestas));
			respuesta.put("tipos", jsonPorTipo(preguntas, encuestas));
			respuesta.put("dias", jsonPorDia(preguntas, encuestas));
			respuesta.put("palabras", jsonPalabras(preguntas, encuestas));
			respuesta.put("detalle", jsonDetalle(preguntas, encuestas, nombresTienda, nombresCliente));
			respuesta.put("detalleTope", Integer.valueOf(TOPE_DETALLE));
			respuesta.put("tienda", idTienda > 0 ? nombresTienda.get(Integer.valueOf(idTienda)) : "Todas las tiendas");
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EncuestaClienteCtrl.consultar: " + e.toString());
			return (error("Error consultando el reporte."));
		}
		return (respuesta.toJSONString());
	}

	// ---------------------------------------------------------------------------------------------
	// Agrupar y calcular
	// ---------------------------------------------------------------------------------------------

	private ArrayList<Enc> agrupar(final List<Respuesta> respuestas) {
		final LinkedHashMap<String, Enc> porClave = new LinkedHashMap<String, Enc>();
		for (final Respuesta r : respuestas) {
			final String clave = r.idTienda + "-" + r.idPedido;
			Enc e = porClave.get(clave);
			if (e == null) {
				e = new Enc();
				e.idTienda = r.idTienda;
				e.idPedido = r.idPedido;
				e.tipo = r.tipoAtencion;
				e.fecha = (r.fecha == null ? null : r.fecha.toLocalDateTime());
				porClave.put(clave, e);
			}
			if (e.tipo.length() == 0) {
				e.tipo = r.tipoAtencion;
			}
			e.r.put(Integer.valueOf(r.idPregunta), r.respuesta);
		}
		return (new ArrayList<Enc>(porClave.values()));
	}

	/** El puntaje de una respuesta segun el tipo de pregunta; 0 si no es valida. */
	private static int puntaje(final String tipoPregunta, final String texto) {
		if (texto == null) {
			return (0);
		}
		if ("valoracion".equals(tipoPregunta)) {
			final String t = normalizar(texto);
			if (t.equals("muy buena") || t.equals("muy bueno") || t.equals("excelente")) {
				return (4);
			}
			if (t.equals("buena") || t.equals("bueno")) {
				return (3);
			}
			if (t.equals("regular")) {
				return (2);
			}
			if (t.equals("mala") || t.equals("malo") || t.equals("muy mala") || t.equals("muy malo")) {
				return (1);
			}
			return (0);
		}
		if ("calificacion".equals(tipoPregunta)) {
			try {
				final int v = Integer.parseInt(texto.trim());
				return (v >= 1 && v <= 10 ? v : 0);
			} catch (final Exception e) {
				return (0);
			}
		}
		return (0);
	}

	private static boolean esPuntuable(final Pregunta p) {
		return ("valoracion".equals(p.tipo) || "calificacion".equals(p.tipo));
	}

	/** Un comentario que de verdad dice algo: ni vacio ni "no" ni "no aplica". */
	private static boolean comentarioUtil(final String texto) {
		if (texto == null) {
			return (false);
		}
		final String t = normalizar(texto);
		if (t.length() < 4) {
			return (false);
		}
		return (!(t.equals("no aplica") || t.equals("ninguna") || t.equals("ninguno") || t.equals("nada")
				|| t.equals("n a") || t.equals("sin comentarios") || t.equals("sin comentario")));
	}

	private Estad estadistica(final Pregunta p, final List<Enc> encuestas) {
		final Estad e = new Estad();
		for (final Enc enc : encuestas) {
			final String texto = enc.r.get(Integer.valueOf(p.idPregunta));
			if (texto == null) {
				continue;
			}
			if (esPuntuable(p)) {
				final int v = puntaje(p.tipo, texto);
				if (v > 0) {
					e.n++;
					e.suma += v;
					e.cuenta[v]++;
				}
			} else if ("cerrada".equals(p.tipo)) {
				final String t = normalizar(texto);
				if (t.equals("si")) {
					e.n++;
					e.si++;
				} else if (t.equals("no")) {
					e.n++;
					e.no++;
				}
			} else {
				e.n++;
				if (comentarioUtil(texto)) {
					e.conComentario++;
				}
			}
		}
		return (e);
	}

	// ---------------------------------------------------------------------------------------------
	// JSON
	// ---------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private JSONObject jsonPregunta(final Pregunta p, final Estad e) {
		final JSONObject o = new JSONObject();
		o.put("idpregunta", Integer.valueOf(p.idPregunta));
		o.put("titulo", p.titulo);
		o.put("descripcion", p.descripcion);
		o.put("tipo", p.tipo);
		o.put("n", Integer.valueOf(e.n));
		final JSONArray distribucion = new JSONArray();
		if ("valoracion".equals(p.tipo)) {
			final String[] etiquetas = { "", "Mala", "Regular", "Buena", "Muy buena" };
			for (int v = 4; v >= 1; v--) {
				distribucion.add(opcion(String.valueOf(v), etiquetas[v], e.cuenta[v], e.n));
			}
			o.put("promedio", Double.valueOf(redondear(e.promedio())));
			o.put("escala", Integer.valueOf(4));
			o.put("pctBuena", Double.valueOf(e.n == 0 ? 0d : redondear(100d * (e.cuenta[3] + e.cuenta[4]) / e.n)));
		} else if ("calificacion".equals(p.tipo)) {
			for (int v = 10; v >= 1; v--) {
				distribucion.add(opcion(String.valueOf(v), String.valueOf(v), e.cuenta[v], e.n));
			}
			final int promotores = e.cuenta[9] + e.cuenta[10];
			final int pasivos = e.cuenta[7] + e.cuenta[8];
			final int detractores = e.n - promotores - pasivos;
			o.put("promedio", Double.valueOf(redondear(e.promedio())));
			o.put("escala", Integer.valueOf(10));
			o.put("promotores", Integer.valueOf(promotores));
			o.put("pasivos", Integer.valueOf(pasivos));
			o.put("detractores", Integer.valueOf(detractores));
			o.put("indice", Double.valueOf(e.n == 0 ? 0d : redondear(100d * (promotores - detractores) / e.n)));
		} else if ("cerrada".equals(p.tipo)) {
			distribucion.add(opcion("Si", "Si", e.si, e.n));
			distribucion.add(opcion("No", "No", e.no, e.n));
			o.put("pctSi", Double.valueOf(e.n == 0 ? 0d : redondear(100d * e.si / e.n)));
		} else {
			o.put("conComentario", Integer.valueOf(e.conComentario));
		}
		o.put("distribucion", distribucion);
		return (o);
	}

	@SuppressWarnings("unchecked")
	private JSONObject opcion(final String valor, final String etiqueta, final int cantidad, final int total) {
		final JSONObject o = new JSONObject();
		o.put("valor", valor);
		o.put("etiqueta", etiqueta);
		o.put("cantidad", Integer.valueOf(cantidad));
		o.put("porcentaje", Double.valueOf(total == 0 ? 0d : redondear(100d * cantidad / total)));
		return (o);
	}

	/** Los promedios de cada pregunta puntuable para un grupo de encuestas: {idpregunta: promedio}. */
	@SuppressWarnings("unchecked")
	private JSONObject promedios(final List<Pregunta> preguntas, final List<Enc> grupo) {
		final JSONObject o = new JSONObject();
		for (final Pregunta p : preguntas) {
			if (!esPuntuable(p)) {
				continue;
			}
			final Estad e = estadistica(p, grupo);
			if (e.n > 0) {
				o.put(String.valueOf(p.idPregunta), Double.valueOf(redondear(e.promedio())));
			}
		}
		return (o);
	}

	/** Semana a semana (la semana empieza el lunes): cuantas encuestas y los promedios. */
	@SuppressWarnings("unchecked")
	private JSONArray jsonSemanas(final List<Pregunta> preguntas, final List<Enc> encuestas) {
		final TreeMap<String, ArrayList<Enc>> porSemana = new TreeMap<String, ArrayList<Enc>>();
		for (final Enc e : encuestas) {
			if (e.fecha == null) {
				continue;
			}
			final LocalDate lunes = e.fecha.toLocalDate().with(DayOfWeek.MONDAY);
			ArrayList<Enc> lista = porSemana.get(lunes.toString());
			if (lista == null) {
				lista = new ArrayList<Enc>();
				porSemana.put(lunes.toString(), lista);
			}
			lista.add(e);
		}
		final JSONArray arreglo = new JSONArray();
		for (final Map.Entry<String, ArrayList<Enc>> en : porSemana.entrySet()) {
			final JSONObject o = new JSONObject();
			o.put("semana", en.getKey());
			o.put("encuestas", Integer.valueOf(en.getValue().size()));
			o.put("promedios", promedios(preguntas, en.getValue()));
			arreglo.add(o);
		}
		return (arreglo);
	}

	/** Por tipo de atencion: domicilio o punto de venta. */
	@SuppressWarnings("unchecked")
	private JSONArray jsonPorTipo(final List<Pregunta> preguntas, final List<Enc> encuestas) {
		final TreeMap<String, ArrayList<Enc>> porTipo = new TreeMap<String, ArrayList<Enc>>();
		for (final Enc e : encuestas) {
			final String tipo = (e.tipo == null || e.tipo.trim().length() == 0) ? "sin dato" : e.tipo.trim();
			ArrayList<Enc> lista = porTipo.get(tipo);
			if (lista == null) {
				lista = new ArrayList<Enc>();
				porTipo.put(tipo, lista);
			}
			lista.add(e);
		}
		final JSONArray arreglo = new JSONArray();
		for (final Map.Entry<String, ArrayList<Enc>> en : porTipo.entrySet()) {
			final JSONObject o = new JSONObject();
			o.put("tipo", en.getKey());
			o.put("encuestas", Integer.valueOf(en.getValue().size()));
			o.put("promedios", promedios(preguntas, en.getValue()));
			arreglo.add(o);
		}
		return (arreglo);
	}

	/** Por dia de la semana en que el cliente contesto. */
	@SuppressWarnings("unchecked")
	private JSONArray jsonPorDia(final List<Pregunta> preguntas, final List<Enc> encuestas) {
		final ArrayList<ArrayList<Enc>> porDia = new ArrayList<ArrayList<Enc>>();
		for (int i = 0; i < 7; i++) {
			porDia.add(new ArrayList<Enc>());
		}
		for (final Enc e : encuestas) {
			if (e.fecha != null) {
				porDia.get(e.fecha.getDayOfWeek().getValue() - 1).add(e);
			}
		}
		final JSONArray arreglo = new JSONArray();
		for (int i = 0; i < 7; i++) {
			final JSONObject o = new JSONObject();
			o.put("dia", DIAS[i]);
			o.put("encuestas", Integer.valueOf(porDia.get(i).size()));
			o.put("promedios", promedios(preguntas, porDia.get(i)));
			arreglo.add(o);
		}
		return (arreglo);
	}

	/** Las palabras que mas se repiten en los comentarios (preguntas abiertas). */
	@SuppressWarnings("unchecked")
	private JSONArray jsonPalabras(final List<Pregunta> preguntas, final List<Enc> encuestas) {
		final HashMap<String, Integer> cuenta = new HashMap<String, Integer>();
		for (final Pregunta p : preguntas) {
			if (!"abierta".equals(p.tipo)) {
				continue;
			}
			for (final Enc enc : encuestas) {
				final String texto = enc.r.get(Integer.valueOf(p.idPregunta));
				if (!comentarioUtil(texto)) {
					continue;
				}
				//Una palabra cuenta una vez por comentario: si no, un comentario largo pesaria como cien.
				final Set<String> vistas = new HashSet<String>();
				for (final String palabra : normalizar(texto).split("[^a-z]+")) {
					if (palabra.length() >= 4 && !PALABRAS_VACIAS.contains(palabra) && vistas.add(palabra)) {
						final Integer actual = cuenta.get(palabra);
						cuenta.put(palabra, Integer.valueOf(actual == null ? 1 : actual.intValue() + 1));
					}
				}
			}
		}
		final ArrayList<Map.Entry<String, Integer>> lista = new ArrayList<Map.Entry<String, Integer>>(cuenta.entrySet());
		Collections.sort(lista, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(final Map.Entry<String, Integer> a, final Map.Entry<String, Integer> b) {
				final int c = b.getValue().compareTo(a.getValue());
				return (c != 0 ? c : a.getKey().compareTo(b.getKey()));
			}
		});
		final JSONArray arreglo = new JSONArray();
		for (int i = 0; i < lista.size() && i < 20; i++) {
			if (lista.get(i).getValue().intValue() < 2) {
				break;
			}
			final JSONObject o = new JSONObject();
			o.put("palabra", lista.get(i).getKey());
			o.put("veces", lista.get(i).getValue());
			arreglo.add(o);
		}
		return (arreglo);
	}

	/** Una fila por encuesta, de la mas reciente a la mas antigua. */
	@SuppressWarnings("unchecked")
	private JSONArray jsonDetalle(final List<Pregunta> preguntas, final List<Enc> encuestas,
			final Map<Integer, String> nombresTienda, final Map<String, String> nombresCliente) {
		final ArrayList<Enc> orden = new ArrayList<Enc>(encuestas);
		Collections.sort(orden, new Comparator<Enc>() {
			public int compare(final Enc a, final Enc b) {
				if (a.fecha == null || b.fecha == null) {
					return (a.fecha == null ? (b.fecha == null ? 0 : 1) : -1);
				}
				return (b.fecha.compareTo(a.fecha));
			}
		});
		final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		final JSONArray arreglo = new JSONArray();
		for (int i = 0; i < orden.size() && i < TOPE_DETALLE; i++) {
			final Enc e = orden.get(i);
			final JSONObject o = new JSONObject();
			o.put("fecha", e.fecha == null ? "" : e.fecha.format(fmt));
			o.put("idtienda", Integer.valueOf(e.idTienda));
			final String tienda = nombresTienda.get(Integer.valueOf(e.idTienda));
			o.put("tienda", tienda == null ? String.valueOf(e.idTienda) : tienda);
			o.put("pedido", Integer.valueOf(e.idPedido));
			o.put("tipo", e.tipo);
			final String cliente = nombresCliente.get(e.idTienda + "-" + e.idPedido);
			o.put("cliente", cliente == null ? "" : cliente);
			final JSONObject resp = new JSONObject();
			final StringBuilder comentario = new StringBuilder();
			boolean baja = false;
			for (final Pregunta p : preguntas) {
				final String texto = e.r.get(Integer.valueOf(p.idPregunta));
				if (texto == null) {
					continue;
				}
				resp.put(String.valueOf(p.idPregunta), texto);
				if ("abierta".equals(p.tipo) && comentarioUtil(texto)) {
					if (comentario.length() > 0) {
						comentario.append(" | ");
					}
					comentario.append(texto);
				}
				//Calificacion baja: valoracion en Regular o Mala, recomendacion de 6 o menos, o un inconveniente.
				final int v = puntaje(p.tipo, texto);
				if ("valoracion".equals(p.tipo) && v > 0 && v <= 2) {
					baja = true;
				}
				if ("calificacion".equals(p.tipo) && v > 0 && v <= 6) {
					baja = true;
				}
				if ("cerrada".equals(p.tipo) && normalizar(texto).equals("si")) {
					baja = true;
				}
			}
			o.put("resp", resp);
			o.put("comentario", comentario.toString());
			o.put("baja", Boolean.valueOf(baja));
			arreglo.add(o);
		}
		return (arreglo);
	}

	// ---------------------------------------------------------------------------------------------
	// Utilidades
	// ---------------------------------------------------------------------------------------------

	private HashMap<Integer, String> nombresTienda() {
		final HashMap<Integer, String> nombres = new HashMap<Integer, String>();
		try {
			for (final Tienda t : TiendaDAO.obtenerTiendas()) {
				nombres.put(Integer.valueOf(t.getIdTienda()), t.getNombreTienda());
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EncuestaClienteCtrl.nombresTienda: " + e.toString());
		}
		return (nombres);
	}

	private static String normalizar(final String texto) {
		final String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return (sinTildes.toLowerCase().trim().replaceAll("\\s+", " "));
	}

	private static double redondear(final double valor) {
		return (Math.round(valor * 10d) / 10d);
	}

	@SuppressWarnings("unchecked")
	private String error(final String mensaje) {
		final JSONObject o = new JSONObject();
		o.put("resultado", "ERROR");
		o.put("mensaje", mensaje);
		return (o.toJSONString());
	}

	private boolean fechaValida(final String fecha) {
		if (fecha == null || !fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return (false);
		}
		try {
			LocalDate.parse(fecha);
			return (true);
		} catch (final Exception e) {
			return (false);
		}
	}
}
