package capaControladorCC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * La operacion de domiciliarios de UN dia en TODAS las tiendas, una fila por tienda.
 *
 * No recalcula nada: le pide a OperacionDomiciliariosCtrl el reporte de cada tienda y se
 * queda con su bloque "resumen". Hacerlo asi cuesta algo de CPU -se arma el detalle por
 * domiciliario y por hora y se bota-, y vale la pena: cualquier cambio en como se cuenta
 * un pedido tarde o un regreso valido entra solo en los dos reportes. Una segunda copia
 * de esas cuentas se habria desincronizado el primer dia.
 *
 * Un dia ya cerrado sale del datamart, asi que son once consultas a la misma base y
 * responde rapido. El dia de hoy se le pregunta a las once tiendas en vivo: ahi si se
 * demora, y una tienda apagada sale listada aparte en vez de contarse como cero.
 */
public class OperacionRedCtrl {

	/** Las once tiendas en paralelo; mas hilos no ayudan, el cuello es la base. */
	private static final int HILOS = 6;

	/** Si una tienda no contesta en este tiempo, se reporta y no se espera mas. */
	private static final int MINUTOS_TOPE = 3;

	/** Lo que se suma de todas las tiendas para dar el Total Red. */
	private static class Total {
		long domiciliarios;
		long conPedidos;
		long salidas;
		long pedidos;
		long medibles;
		long aTiempo;
		long sumaRuta;
		long conRuta;
		long sumaRegreso;
		long regresosValidos;
		long regresosDemorados;
		long regresosSinDato;

		@SuppressWarnings("unchecked")
		JSONObject aJSON() {
			final JSONObject o = new JSONObject();
			o.put("domiciliarios", Long.valueOf(domiciliarios));
			o.put("con_pedidos", Long.valueOf(conPedidos));
			o.put("salidas", Long.valueOf(salidas));
			o.put("pedidos", Long.valueOf(pedidos));
			o.put("medibles", Long.valueOf(medibles));
			o.put("a_tiempo", Long.valueOf(aTiempo));
			o.put("demorados", Long.valueOf(medibles - aTiempo));
			o.put("porcentaje", medibles > 0 ? Double.valueOf(redondear(aTiempo * 100.0 / medibles)) : null);
			o.put("prom_ruta", conRuta > 0 ? Double.valueOf(redondear(sumaRuta / (double) conRuta)) : null);
			o.put("prom_regreso", regresosValidos > 0
					? Double.valueOf(redondear(sumaRegreso / (double) regresosValidos)) : null);
			o.put("regresos_demorados", Long.valueOf(regresosDemorados));
			o.put("regresos_sin_dato", Long.valueOf(regresosSinDato));
			return (o);
		}
	}

	/**
	 * @param fecha yyyy-MM-dd, fecha de la jornada
	 */
	@SuppressWarnings("unchecked")
	public static String consultar(final String fecha) {
		final Logger logger = Logger.getLogger("log_file");
		final JSONObject respuesta = new JSONObject();
		respuesta.put("fecha", fecha);

		if (fecha == null || fecha.length() == 0) {
			respuesta.put("error", "Falta la fecha.");
			return (respuesta.toJSONString());
		}

		//Las que de verdad despachan. Poblado y Medayoung no tienen base propia, y una
		//tienda sin hosbd no se puede consultar ni en vivo ni identificar en el datamart.
		final ArrayList<Tienda> tiendas = new ArrayList<Tienda>();
		for (final Tienda t : TiendaDAO.obtenerTiendasFuncionales()) {
			if (t.getHosbd() != null && t.getHosbd().trim().length() > 0) {
				tiendas.add(t);
			}
		}
		if (tiendas.isEmpty()) {
			respuesta.put("error", "No hay tiendas configuradas para consultar.");
			return (respuesta.toJSONString());
		}

		final ArrayList<JSONObject> crudos = consultarTiendas(tiendas, fecha, logger);

		final JSONArray filas = new JSONArray();
		final JSONArray sinRespuesta = new JSONArray();
		final Total total = new Total();
		String datosHasta = "";

		for (final JSONObject uno : crudos) {
			final String nombre = texto(uno.get("tienda"));
			final Object resumen = uno.get("resumen");
			if (!(resumen instanceof JSONObject)) {
				//Una tienda que no contesto NO es una tienda en ceros: se dice aparte.
				final JSONObject falla = new JSONObject();
				falla.put("idtienda", uno.get("idtienda"));
				falla.put("tienda", nombre);
				falla.put("error", texto(uno.get("error")));
				sinRespuesta.add(falla);
				continue;
			}
			final JSONObject r = (JSONObject) resumen;
			final JSONObject fila = new JSONObject();
			fila.put("idtienda", uno.get("idtienda"));
			fila.put("tienda", nombre);
			fila.put("fuente", uno.get("fuente"));
			fila.putAll(r);
			filas.add(fila);

			total.domiciliarios += entero(r.get("domiciliarios"));
			total.conPedidos += entero(r.get("con_pedidos"));
			total.salidas += entero(r.get("salidas"));
			total.pedidos += entero(r.get("pedidos"));
			total.medibles += entero(r.get("medibles"));
			total.aTiempo += entero(r.get("a_tiempo"));
			total.sumaRuta += entero(r.get("suma_ruta"));
			total.conRuta += entero(r.get("con_ruta"));
			total.sumaRegreso += entero(r.get("suma_regreso"));
			total.regresosValidos += entero(r.get("regresos_validos"));
			total.regresosDemorados += entero(r.get("regresos_demorados"));
			total.regresosSinDato += entero(r.get("regresos_sin_dato"));

			final String hasta = texto(uno.get("datos_hasta"));
			if (hasta.length() > 0 && (datosHasta.length() == 0 || hasta.compareTo(datosHasta) < 0)) {
				//El dia mas atrasado de todas: es hasta donde se puede confiar en el conjunto.
				datosHasta = hasta;
			}
		}

		respuesta.put("tiendas", filas);
		respuesta.put("sin_respuesta", sinRespuesta);
		respuesta.put("total", total.aJSON());
		respuesta.put("tiendas_consultadas", Integer.valueOf(tiendas.size()));
		respuesta.put("datos_hasta", datosHasta);
		return (respuesta.toJSONString());
	}

	/** Las once en paralelo. Una que falle o se demore no tumba a las otras. */
	private static ArrayList<JSONObject> consultarTiendas(final List<Tienda> tiendas, final String fecha,
			final Logger logger) {
		final ArrayList<JSONObject> salida = new ArrayList<JSONObject>();
		final ExecutorService executor = Executors.newFixedThreadPool(HILOS);
		try {
			final ArrayList<Callable<JSONObject>> tareas = new ArrayList<Callable<JSONObject>>();
			for (final Tienda t : tiendas) {
				tareas.add(new Callable<JSONObject>() {
					public JSONObject call() {
						return (unaTienda(t, fecha, logger));
					}
				});
			}
			final List<Future<JSONObject>> futuros = executor.invokeAll(tareas, MINUTOS_TOPE, TimeUnit.MINUTES);
			for (int i = 0; i < futuros.size(); i++) {
				try {
					salida.add(futuros.get(i).get());
				} catch (final Exception e) {
					salida.add(fallo(tiendas.get(i), "No contesto a tiempo."));
				}
			}
		} catch (final Exception e) {
			logger.error("operacion red: fallo el barrido de tiendas: " + e.getMessage());
		} finally {
			executor.shutdownNow();
		}
		return (salida);
	}

	private static JSONObject unaTienda(final Tienda t, final String fecha, final Logger logger) {
		try {
			final String json = OperacionDomiciliariosCtrl.consultar(t.getIdTienda(), fecha);
			final Object leido = new JSONParser().parse(json);
			if (leido instanceof JSONObject) {
				return ((JSONObject) leido);
			}
			return (fallo(t, "Respuesta inesperada."));
		} catch (final Exception e) {
			logger.error("operacion red: tienda " + t.getIdTienda() + ": " + e.getMessage());
			return (fallo(t, "No se pudo consultar."));
		}
	}

	@SuppressWarnings("unchecked")
	private static JSONObject fallo(final Tienda t, final String motivo) {
		final JSONObject o = new JSONObject();
		o.put("idtienda", Integer.valueOf(t.getIdTienda()));
		o.put("tienda", t.getNombreTienda());
		o.put("error", motivo);
		return (o);
	}

	private static long entero(final Object valor) {
		if (valor instanceof Number) {
			return (((Number) valor).longValue());
		}
		return (0);
	}

	private static String texto(final Object valor) {
		return (valor == null ? "" : valor.toString());
	}

	private static double redondear(final double valor) {
		return (Math.round(valor * 10.0) / 10.0);
	}
}
