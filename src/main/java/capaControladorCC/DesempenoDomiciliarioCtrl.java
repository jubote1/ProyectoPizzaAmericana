package capaControladorCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.DesempenoDomiciliarioDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Arma el tablero de desempeno de un domiciliario.
 *
 * POR QUE BARRE LAS TIENDAS Y EN PARALELO
 *
 * El trabajo del domiciliario queda en la base local de cada tienda, no en el
 * central. Un domiciliario normalmente esta en una sola tienda, pero rota, y
 * preguntarle al usuario en cual estaba seria pedirle justo lo que viene a
 * averiguar.
 *
 * Se pregunta en paralelo porque en serie no sirve: obtenerConexionBDTiendaRemota
 * espera hasta 10 segundos por tienda, y con doce tiendas una sola apagada ya
 * se siente. El 2026-09-17, sin ir mas lejos, Manrique Piloto no contestaba. En
 * paralelo el peor caso es el de la tienda mas lenta, no la suma de todas.
 *
 * La que no contesta NO se cuenta como cero: se devuelve en tiendas_sin_respuesta
 * para que la pantalla lo diga. Un domiciliario que aparece con cero pedidos
 * porque su tienda estaba apagada es peor que no mostrar nada.
 */
public class DesempenoDomiciliarioCtrl {

	/** Seis a la vez, como el monitoreo de tiendas de TiendaCtrl. */
	private static final int HILOS = 6;

	/** Tope para que una tienda trabada no deje la pantalla colgada. */
	private static final int SEGUNDOS_ESPERA = 45;

	public static String obtenerDomiciliarios() {
		final JSONArray lista = new JSONArray();
		for (final DesempenoDomiciliarioDAO.Domiciliario d : DesempenoDomiciliarioDAO.obtenerDomiciliarios()) {
			final JSONObject o = new JSONObject();
			o.put("id", d.id);
			o.put("nombre", d.nombre);
			o.put("nombrelargo", d.nombreLargo);
			o.put("tipo", d.tipo);
			lista.add(o);
		}
		final JSONObject respuesta = new JSONObject();
		respuesta.put("domiciliarios", lista);
		return (respuesta.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String consultarDesempeno(final int idEmpleado, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final JSONObject respuesta = new JSONObject();
		respuesta.put("desde", desde);
		respuesta.put("hasta", hasta);

		if (idEmpleado <= 0 || desde == null || desde.length() == 0 || hasta == null || hasta.length() == 0) {
			respuesta.put("error", "Falta el domiciliario o el rango de fechas.");
			return (respuesta.toJSONString());
		}

		//El nombre sale de la misma lista de domiciliarios: asi la pantalla no
		//puede mostrar el desempeno de alguien que ya no esta activo.
		DesempenoDomiciliarioDAO.Domiciliario elegido = null;
		for (final DesempenoDomiciliarioDAO.Domiciliario d : DesempenoDomiciliarioDAO.obtenerDomiciliarios()) {
			if (d.id == idEmpleado) {
				elegido = d;
				break;
			}
		}
		if (elegido == null) {
			respuesta.put("error", "Ese empleado no esta activo o no esta marcado como domiciliario.");
			return (respuesta.toJSONString());
		}
		final JSONObject jsonDomi = new JSONObject();
		jsonDomi.put("id", elegido.id);
		jsonDomi.put("nombre", elegido.nombre);
		jsonDomi.put("nombrelargo", elegido.nombreLargo);
		jsonDomi.put("tipo", elegido.tipo);
		respuesta.put("domiciliario", jsonDomi);

		final ArrayList<DesempenoDomiciliarioDAO.ResultadoTienda> resultados = barrerTiendas(idEmpleado, desde,
				hasta, logger);

		//Todo junto, para los numeros consolidados.
		final ArrayList<DesempenoDomiciliarioDAO.Entrega> entregas = new ArrayList<DesempenoDomiciliarioDAO.Entrega>();
		final ArrayList<DesempenoDomiciliarioDAO.Salida> salidas = new ArrayList<DesempenoDomiciliarioDAO.Salida>();
		final JSONArray sinRespuesta = new JSONArray();
		final JSONArray porTienda = new JSONArray();
		final DesempenoDomiciliarioDAO.Enrutamiento enrTotal = new DesempenoDomiciliarioDAO.Enrutamiento();

		for (final DesempenoDomiciliarioDAO.ResultadoTienda r : resultados) {
			if (!r.conecto) {
				sinRespuesta.add(r.tienda);
				continue;
			}
			entregas.addAll(r.entregas);
			salidas.addAll(r.salidas);
			sumarEnrutamiento(enrTotal, r.enrutamiento);
			//Una tienda donde nunca trabajo no aporta nada a la pantalla.
			if (r.entregas.isEmpty() && r.salidas.isEmpty()) {
				continue;
			}
			porTienda.add(resumenTienda(r));
		}

		respuesta.put("tiendas_consultadas", resultados.size());
		respuesta.put("tiendas_sin_respuesta", sinRespuesta);
		respuesta.put("por_tienda", porTienda);
		respuesta.put("resumen", resumenEntregas(entregas, salidas.size()));
		respuesta.put("regreso", resumenRegreso(salidas));
		respuesta.put("enrutamiento", aJson(enrTotal));
		respuesta.put("distribucion_promesa", distribucionPromesa(entregas));
		respuesta.put("distribucion_regreso", distribucionRegreso(salidas));
		respuesta.put("por_dia", porDia(entregas));
		respuesta.put("peores", peoresEntregas(entregas));
		return (respuesta.toJSONString());
	}

	// =======================================================================
	// El barrido
	// =======================================================================

	private static ArrayList<DesempenoDomiciliarioDAO.ResultadoTienda> barrerTiendas(final int idEmpleado,
			final String desde, final String hasta, final Logger logger) {
		final ArrayList<DesempenoDomiciliarioDAO.ResultadoTienda> resultados =
				new ArrayList<DesempenoDomiciliarioDAO.ResultadoTienda>();
		final ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendasFuncionales();
		if (tiendas == null || tiendas.isEmpty()) {
			return (resultados);
		}

		final ExecutorService executor = Executors.newFixedThreadPool(HILOS);
		try {
			final List<Callable<DesempenoDomiciliarioDAO.ResultadoTienda>> tareas =
					new ArrayList<Callable<DesempenoDomiciliarioDAO.ResultadoTienda>>();
			for (final Tienda t : tiendas) {
				final String hosbd = t.getHosbd();
				if (hosbd == null || hosbd.trim().length() == 0) {
					continue;
				}
				tareas.add(new Callable<DesempenoDomiciliarioDAO.ResultadoTienda>() {
					public DesempenoDomiciliarioDAO.ResultadoTienda call() {
						return (DesempenoDomiciliarioDAO.consultarTienda(t.getIdTienda(), t.getNombreTienda(),
								hosbd.trim(), idEmpleado, desde, hasta));
					}
				});
			}
			final List<Future<DesempenoDomiciliarioDAO.ResultadoTienda>> futuros =
					executor.invokeAll(tareas, SEGUNDOS_ESPERA, TimeUnit.SECONDS);
			for (int i = 0; i < futuros.size(); i++) {
				try {
					resultados.add(futuros.get(i).get());
				} catch (final Exception e) {
					//La tienda se paso del tiempo o reviento: cuenta como que
					//no contesto, no como que el domiciliario no trabajo.
					logger.error("DesempenoDomiciliarioCtrl: tienda sin respuesta, " + e.toString());
					final DesempenoDomiciliarioDAO.ResultadoTienda fallo =
							new DesempenoDomiciliarioDAO.ResultadoTienda();
					fallo.conecto = false;
					fallo.tienda = nombreDe(tiendas, i);
					fallo.error = "Se paso del tiempo de espera";
					resultados.add(fallo);
				}
			}
		} catch (final Exception e) {
			logger.error("DesempenoDomiciliarioCtrl.barrerTiendas: " + e.toString());
		} finally {
			executor.shutdownNow();
		}
		return (resultados);
	}

	/** El nombre de la tienda que quedo en esa posicion de la lista de tareas. */
	private static String nombreDe(final ArrayList<Tienda> tiendas, final int posicion) {
		int i = 0;
		for (final Tienda t : tiendas) {
			final String hosbd = t.getHosbd();
			if (hosbd == null || hosbd.trim().length() == 0) {
				continue;
			}
			if (i == posicion) {
				return (t.getNombreTienda());
			}
			i++;
		}
		return ("Tienda " + posicion);
	}

	// =======================================================================
	// Los numeros
	// =======================================================================

	@SuppressWarnings("unchecked")
	private static JSONObject resumenEntregas(final ArrayList<DesempenoDomiciliarioDAO.Entrega> entregas,
			final int salidas) {
		final JSONObject o = new JSONObject();
		final ArrayList<Integer> totales = new ArrayList<Integer>();
		final ArrayList<Integer> calles = new ArrayList<Integer>();
		int aTiempo = 0;
		int medibles = 0;
		for (final DesempenoDomiciliarioDAO.Entrega e : entregas) {
			if (!e.medible) {
				continue;
			}
			medibles++;
			totales.add(Integer.valueOf(e.minutosTotal));
			if (e.minutosCalle >= 0) {
				calles.add(Integer.valueOf(e.minutosCalle));
			}
			if (e.minutosTotal <= e.prometido) {
				aTiempo++;
			}
		}
		o.put("pedidos", entregas.size());
		o.put("salidas", salidas);
		o.put("pedidos_por_salida", salidas > 0 ? redondear((double) entregas.size() / salidas, 2) : 0);
		o.put("medibles", medibles);
		o.put("sin_medir", entregas.size() - medibles);
		o.put("a_tiempo", aTiempo);
		o.put("tarde", medibles - aTiempo);
		o.put("porcentaje", medibles > 0 ? redondear(aTiempo * 100.0 / medibles, 1) : 0);
		o.put("promedio_total", promedio(totales));
		o.put("mediana_total", mediana(totales));
		o.put("promedio_calle", promedio(calles));
		o.put("mediana_calle", mediana(calles));
		return (o);
	}

	@SuppressWarnings("unchecked")
	private static JSONObject resumenRegreso(final ArrayList<DesempenoDomiciliarioDAO.Salida> salidas) {
		final JSONObject o = new JSONObject();
		final ArrayList<Integer> minutos = new ArrayList<Integer>();
		int aTiempo = 0;
		int demorados = 0;
		int negativos = 0;
		int sinMarcar = 0;
		int sinRegreso = 0;
		int sinEntrega = 0;
		for (final DesempenoDomiciliarioDAO.Salida s : salidas) {
			if ("OK".equals(s.estadoRegreso)) {
				minutos.add(Integer.valueOf(s.minutosRegreso));
				if (s.minutosRegreso <= DesempenoDomiciliarioDAO.MINUTOS_REGRESO_ALERTA) {
					aTiempo++;
				} else {
					demorados++;
				}
			} else if ("NEGATIVO".equals(s.estadoRegreso)) {
				negativos++;
			} else if ("SINMARCAR".equals(s.estadoRegreso)) {
				sinMarcar++;
			} else if ("SINREGRESO".equals(s.estadoRegreso)) {
				sinRegreso++;
			} else {
				sinEntrega++;
			}
		}
		o.put("salidas", salidas.size());
		o.put("medibles", minutos.size());
		o.put("a_tiempo", aTiempo);
		o.put("demorados", demorados);
		o.put("porcentaje", minutos.size() > 0 ? redondear(aTiempo * 100.0 / minutos.size(), 1) : 0);
		o.put("promedio", promedio(minutos));
		o.put("mediana", mediana(minutos));
		o.put("negativos", negativos);
		o.put("sin_marcar", sinMarcar);
		o.put("sin_regreso", sinRegreso);
		o.put("sin_entrega", sinEntrega);
		o.put("alerta_minutos", DesempenoDomiciliarioDAO.MINUTOS_REGRESO_ALERTA);
		o.put("tope_minutos", DesempenoDomiciliarioDAO.MINUTOS_REGRESO_MAXIMO);
		return (o);
	}

	@SuppressWarnings("unchecked")
	private static JSONObject resumenTienda(final DesempenoDomiciliarioDAO.ResultadoTienda r) {
		final JSONObject o = new JSONObject();
		o.put("idtienda", r.idTienda);
		o.put("tienda", r.tienda);
		o.put("resumen", resumenEntregas(r.entregas, r.salidas.size()));
		o.put("regreso", resumenRegreso(r.salidas));
		o.put("enrutamiento", aJson(r.enrutamiento));
		return (o);
	}

	@SuppressWarnings("unchecked")
	private static JSONObject aJson(final DesempenoDomiciliarioDAO.Enrutamiento e) {
		final JSONObject o = new JSONObject();
		o.put("sugerencias", e.sugerenciasRecibidas);
		o.put("aceptadas", e.aceptadas);
		o.put("rechazadas", e.rechazadas);
		o.put("expiradas", e.expiradas);
		o.put("pendientes", e.pendientes);
		o.put("reasignaciones", e.vecesReasignada);
		o.put("pedidos_llevados", e.pedidosLlevados);
		o.put("pedidos_reasignados", e.pedidosReasignados);
		o.put("pedidos_sin_llevar", e.pedidosSinLlevar);
		o.put("salidas_sugeridas", e.salidasDesdeSugerencia);
		return (o);
	}

	private static void sumarEnrutamiento(final DesempenoDomiciliarioDAO.Enrutamiento total,
			final DesempenoDomiciliarioDAO.Enrutamiento parte) {
		total.sugerenciasRecibidas = total.sugerenciasRecibidas + parte.sugerenciasRecibidas;
		total.aceptadas = total.aceptadas + parte.aceptadas;
		total.rechazadas = total.rechazadas + parte.rechazadas;
		total.expiradas = total.expiradas + parte.expiradas;
		total.pendientes = total.pendientes + parte.pendientes;
		total.vecesReasignada = total.vecesReasignada + parte.vecesReasignada;
		total.pedidosLlevados = total.pedidosLlevados + parte.pedidosLlevados;
		total.pedidosReasignados = total.pedidosReasignados + parte.pedidosReasignados;
		total.pedidosSinLlevar = total.pedidosSinLlevar + parte.pedidosSinLlevar;
		total.salidasDesdeSugerencia = total.salidasDesdeSugerencia + parte.salidasDesdeSugerencia;
	}

	// =======================================================================
	// Para las graficas
	// =======================================================================

	/**
	 * Cuanto se paso o le sobro contra la promesa. Se mide la DIFERENCIA y no
	 * el minuto suelto porque la promesa cambia: hay pedidos de 30 minutos y
	 * de 60, y 45 minutos es tarde en uno y temprano en el otro.
	 */
	@SuppressWarnings("unchecked")
	private static JSONArray distribucionPromesa(final ArrayList<DesempenoDomiciliarioDAO.Entrega> entregas) {
		final String[] rangos = { "20 o mas antes", "10 a 19 antes", "1 a 9 antes", "justo a tiempo",
				"1 a 9 tarde", "10 a 19 tarde", "20 a 29 tarde", "30 o mas tarde" };
		final int[] cuenta = new int[rangos.length];
		for (final DesempenoDomiciliarioDAO.Entrega e : entregas) {
			if (!e.medible) {
				continue;
			}
			final int diferencia = e.minutosTotal - e.prometido;
			if (diferencia <= -20) {
				cuenta[0]++;
			} else if (diferencia <= -10) {
				cuenta[1]++;
			} else if (diferencia <= -1) {
				cuenta[2]++;
			} else if (diferencia == 0) {
				cuenta[3]++;
			} else if (diferencia <= 9) {
				cuenta[4]++;
			} else if (diferencia <= 19) {
				cuenta[5]++;
			} else if (diferencia <= 29) {
				cuenta[6]++;
			} else {
				cuenta[7]++;
			}
		}
		return (aBarras(rangos, cuenta, 3));
	}

	@SuppressWarnings("unchecked")
	private static JSONArray distribucionRegreso(final ArrayList<DesempenoDomiciliarioDAO.Salida> salidas) {
		final String[] rangos = { "0 a 5", "6 a 10", "11 a 15", "16 a 30", "31 a 60", "mas de 60" };
		final int[] cuenta = new int[rangos.length];
		for (final DesempenoDomiciliarioDAO.Salida s : salidas) {
			if (!"OK".equals(s.estadoRegreso)) {
				continue;
			}
			final int m = s.minutosRegreso;
			if (m <= 5) {
				cuenta[0]++;
			} else if (m <= 10) {
				cuenta[1]++;
			} else if (m <= 15) {
				cuenta[2]++;
			} else if (m <= 30) {
				cuenta[3]++;
			} else if (m <= 60) {
				cuenta[4]++;
			} else {
				cuenta[5]++;
			}
		}
		//Los tres primeros rangos estan dentro del limite de alerta.
		return (aBarras(rangos, cuenta, 2));
	}

	/** Las barras, diciendo cual es el ultimo rango que todavia esta bien. */
	@SuppressWarnings("unchecked")
	private static JSONArray aBarras(final String[] rangos, final int[] cuenta, final int ultimoBueno) {
		final JSONArray lista = new JSONArray();
		for (int i = 0; i < rangos.length; i++) {
			final JSONObject o = new JSONObject();
			o.put("rango", rangos[i]);
			o.put("valor", cuenta[i]);
			o.put("bueno", i <= ultimoBueno);
			lista.add(o);
		}
		return (lista);
	}

	@SuppressWarnings("unchecked")
	private static JSONArray porDia(final ArrayList<DesempenoDomiciliarioDAO.Entrega> entregas) {
		//LinkedHashMap para que los dias salgan en el orden en que llegaron,
		//que ya viene ordenado por fecha desde la consulta.
		final Map<String, int[]> dias = new LinkedHashMap<String, int[]>();
		for (final DesempenoDomiciliarioDAO.Entrega e : entregas) {
			int[] d = dias.get(e.fecha);
			if (d == null) {
				d = new int[4];
				dias.put(e.fecha, d);
			}
			d[0]++;
			if (e.medible) {
				d[1]++;
				if (e.minutosTotal <= e.prometido) {
					d[2]++;
				}
				if (e.minutosCalle >= 0) {
					d[3] = d[3] + e.minutosCalle;
				}
			}
		}
		final ArrayList<String> fechas = new ArrayList<String>(dias.keySet());
		Collections.sort(fechas);
		final JSONArray lista = new JSONArray();
		for (final String fecha : fechas) {
			final int[] d = dias.get(fecha);
			final JSONObject o = new JSONObject();
			o.put("fecha", fecha);
			o.put("pedidos", d[0]);
			o.put("medibles", d[1]);
			o.put("a_tiempo", d[2]);
			o.put("porcentaje", d[1] > 0 ? redondear(d[2] * 100.0 / d[1], 1) : 0);
			o.put("promedio_calle", d[1] > 0 ? redondear((double) d[3] / d[1], 1) : 0);
			lista.add(o);
		}
		return (lista);
	}

	/** Las quince entregas que mas se pasaron, para poder ir a mirarlas. */
	@SuppressWarnings("unchecked")
	private static JSONArray peoresEntregas(final ArrayList<DesempenoDomiciliarioDAO.Entrega> entregas) {
		final ArrayList<DesempenoDomiciliarioDAO.Entrega> tarde =
				new ArrayList<DesempenoDomiciliarioDAO.Entrega>();
		for (final DesempenoDomiciliarioDAO.Entrega e : entregas) {
			if (e.medible && e.minutosTotal > e.prometido) {
				tarde.add(e);
			}
		}
		Collections.sort(tarde, new java.util.Comparator<DesempenoDomiciliarioDAO.Entrega>() {
			public int compare(final DesempenoDomiciliarioDAO.Entrega a,
					final DesempenoDomiciliarioDAO.Entrega b) {
				return ((b.minutosTotal - b.prometido) - (a.minutosTotal - a.prometido));
			}
		});
		final JSONArray lista = new JSONArray();
		for (int i = 0; i < tarde.size() && i < 15; i++) {
			final DesempenoDomiciliarioDAO.Entrega e = tarde.get(i);
			final JSONObject o = new JSONObject();
			o.put("tienda", e.tienda);
			o.put("fecha", e.fecha);
			o.put("idpedido", e.idPedido);
			o.put("prometido", e.prometido);
			o.put("minutos", e.minutosTotal);
			o.put("exceso", e.minutosTotal - e.prometido);
			o.put("calle", e.minutosCalle);
			lista.add(o);
		}
		return (lista);
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	private static double promedio(final ArrayList<Integer> valores) {
		if (valores.isEmpty()) {
			return (0);
		}
		long suma = 0;
		for (final Integer v : valores) {
			suma = suma + v.intValue();
		}
		return (redondear((double) suma / valores.size(), 1));
	}

	/**
	 * La mediana. Se muestra al lado del promedio a proposito: cuando las dos
	 * se separan mucho es que hay unos pocos casos extremos estirando el
	 * promedio, y conviene mirar el detalle antes de sacar conclusiones.
	 */
	private static int mediana(final ArrayList<Integer> valores) {
		if (valores.isEmpty()) {
			return (0);
		}
		final ArrayList<Integer> copia = new ArrayList<Integer>(valores);
		Collections.sort(copia);
		return (copia.get(copia.size() / 2).intValue());
	}

	private static double redondear(final double valor, final int decimales) {
		double factor = 1;
		for (int i = 0; i < decimales; i++) {
			factor = factor * 10;
		}
		return (Math.round(valor * factor) / factor);
	}
}
