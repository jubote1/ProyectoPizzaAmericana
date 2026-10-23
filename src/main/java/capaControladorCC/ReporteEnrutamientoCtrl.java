package capaControladorCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.ReporteEnrutamientoDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Arma el reporte de uso del enrutamiento.
 *
 * PARA QUE EXISTE
 *
 * El enrutamiento de domiciliarios solo sirve si las tiendas lo usan, y para
 * impulsarlo hay que saber tres cosas: que porcentaje de los domicilios propios
 * salen dentro de una ruta sugerida, en que momentos del dia se usa y en cuales
 * no, y si los domiciliarios estan SOLTANDO los pedidos que se les enrutan, cosa
 * que no deberia pasar. Todo sale de la base local de cada tienda, no del
 * central, asi que hay que ir a preguntarle a cada una (ver
 * ReporteEnrutamientoDAO para las definiciones exactas).
 *
 * EN PARALELO, COMO DesempenoDomiciliarioCtrl
 *
 * obtenerConexionBDTiendaRemota espera hasta 10 segundos por tienda; en serie,
 * una tienda apagada ya se siente. En paralelo el peor caso es el de la tienda
 * mas lenta y no la suma de todas.
 *
 * LA TIENDA APAGADA SE LEE DEL DATAMART
 *
 * Una tienda que no contesta (o que se pasa del tiempo) se vuelve a pedir al
 * datamart, que el servicio de replica llena de madrugada con el dia anterior.
 * Esa lectura sale marcada como DATAMART con el ultimo dia que tiene, porque NO
 * incluye hoy. Las cifras de la red se entregan de dos formas: solo con las
 * tiendas leidas en vivo, y sumando tambien las del datamart; la pantalla deja
 * escoger. Sumarlas por defecto mezclaria "hasta hoy" con "hasta ayer" sin que
 * nadie lo decida.
 *
 * LA QUE NO CONTESTA NI TIENE DATAMART NO ES CERO
 *
 * Sale marcada y NO entra en los totales: contarla como cero domicilios haria
 * ver peor a la red entera por una tienda apagada. Lo mismo con una tienda que
 * todavia no tiene el enrutamiento instalado: sus pedidos se muestran, pero no
 * entran al porcentaje de uso, porque "no puede usarlo" no es lo mismo que "no
 * lo usa".
 */
public class ReporteEnrutamientoCtrl {

	/** Seis a la vez, como el resto de los barridos de tiendas. */
	private static final int HILOS = 6;

	/** Tope para que una tienda trabada no deje la pantalla colgada. */
	private static final int SEGUNDOS_ESPERA = 60;

	/** El rango maximo que se acepta: mas largo que esto es pedirle a las tiendas una consulta enorme. */
	public static final int DIAS_MAXIMOS = 92;

	/** Cuantos domiciliarios y cuantos soltados se listan en el total de la red. */
	private static final int MAXIMO_RANKING = 15;
	private static final int MAXIMO_DETALLE = 60;

	/** Un renglon del ranking de la red: el domiciliario con la tienda en la que lo solto. */
	private static class FilaRanking {
		int idTienda;
		String tienda;
		ReporteEnrutamientoDAO.PorDomiciliario d;
	}

	/** Un soltado del detalle de la red, con su tienda. */
	private static class FilaDetalle {
		String tienda;
		ReporteEnrutamientoDAO.Soltado s;
	}

	@SuppressWarnings("unchecked")
	public static String consultar(final int idTienda, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> resultados = barrerTiendas(idTienda, desde, hasta,
				logger);
		completarNombres(resultados);

		final JSONArray tiendasJSON = new JSONArray();
		boolean hayDatamart = false;
		for (final ReporteEnrutamientoDAO.ResultadoTienda r : resultados) {
			if (ReporteEnrutamientoDAO.FUENTE_DATAMART.equals(r.fuente) && ReporteEnrutamientoDAO.ESTADO_OK.equals(r.estado)) {
				hayDatamart = true;
			}
			tiendasJSON.add(tiendaJSON(r));
		}

		final JSONObject respuesta = new JSONObject();
		respuesta.put("desde", desde);
		respuesta.put("hasta", hasta);
		respuesta.put("hay_datamart", hayDatamart);
		respuesta.put("red", armarRed(resultados, false));
		respuesta.put("red_con_datamart", armarRed(resultados, true));
		respuesta.put("tiendas", tiendasJSON);
		return (respuesta.toJSONString());
	}

	// =======================================================================
	// La red
	// =======================================================================

	/**
	 * Los totales de la red: solo las tiendas que respondieron (en vivo y, si
	 * conDatamart, tambien las leidas del datamart).
	 *
	 * El porcentaje de uso solo cuenta tiendas que respondieron Y tienen el
	 * enrutamiento instalado. Los soltados cuentan toda tienda que pudo leerse.
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject armarRed(final ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> resultados,
			final boolean conDatamart) {
		int pedidos = 0;
		int enrutados = 0;
		int salidas = 0;
		int salidasEnrutadas = 0;
		int excluidosPlataforma = 0;
		int excluidosTercerizados = 0;
		int tiendasEnLaCuenta = 0;
		final ReporteEnrutamientoDAO.Desglose enr = new ReporteEnrutamientoDAO.Desglose();
		int tiendasConSoltados = 0;
		final int[] entradas = new int[24];
		final int[] entradasEnrutadas = new int[24];
		final int[] salidasHora = new int[24];
		final int[] salidasHoraEnrutadas = new int[24];
		final ArrayList<FilaRanking> ranking = new ArrayList<FilaRanking>();
		final ArrayList<FilaDetalle> detalle = new ArrayList<FilaDetalle>();

		for (final ReporteEnrutamientoDAO.ResultadoTienda r : resultados) {
			if (!ReporteEnrutamientoDAO.ESTADO_OK.equals(r.estado)) {
				continue;
			}
			if (!conDatamart && ReporteEnrutamientoDAO.FUENTE_DATAMART.equals(r.fuente)) {
				continue;
			}
			excluidosPlataforma = excluidosPlataforma + r.excluidosPlataforma;
			excluidosTercerizados = excluidosTercerizados + r.excluidosTercerizados;
			if (r.enrutamientoInstalado) {
				tiendasEnLaCuenta++;
				pedidos = pedidos + r.pedidos;
				enrutados = enrutados + r.enrutados;
				salidas = salidas + r.salidasTotal;
				salidasEnrutadas = salidasEnrutadas + r.salidasEnrutadasTotal;
				for (int h = 0; h < 24; h++) {
					entradas[h] = entradas[h] + r.entradas[h];
					entradasEnrutadas[h] = entradasEnrutadas[h] + r.entradasEnrutadas[h];
					salidasHora[h] = salidasHora[h] + r.salidas[h];
					salidasHoraEnrutadas[h] = salidasHoraEnrutadas[h] + r.salidasEnrutadas[h];
				}
			}
			if (r.soltadosDisponible) {
				tiendasConSoltados++;
				enr.sumar(r.enrutamiento);
				for (final ReporteEnrutamientoDAO.PorDomiciliario d : r.porDomiciliario) {
					final FilaRanking f = new FilaRanking();
					f.idTienda = r.idTienda;
					f.tienda = r.tienda;
					f.d = d;
					ranking.add(f);
				}
				for (final ReporteEnrutamientoDAO.Soltado s : r.detalleSoltados) {
					final FilaDetalle f = new FilaDetalle();
					f.tienda = r.tienda;
					f.s = s;
					detalle.add(f);
				}
			}
		}

		Collections.sort(ranking, new Comparator<FilaRanking>() {
			public int compare(final FilaRanking a, final FilaRanking b) {
				return (Integer.compare(b.d.soltados + b.d.libres, a.d.soltados + a.d.libres));
			}
		});
		Collections.sort(detalle, new Comparator<FilaDetalle>() {
			public int compare(final FilaDetalle a, final FilaDetalle b) {
				return (b.s.fechaHora.compareTo(a.s.fechaHora));
			}
		});

		final JSONObject red = new JSONObject();
		red.put("pedidos", pedidos);
		red.put("enrutados", enrutados);
		red.put("no_enrutados", pedidos - enrutados);
		red.put("porcentaje", porcentaje(enrutados, pedidos));
		red.put("salidas", salidas);
		red.put("salidas_enrutadas", salidasEnrutadas);
		red.put("sin_salida", pedidos - salidas > 0 ? pedidos - salidas : 0);
		red.put("excluidos_plataforma", excluidosPlataforma);
		red.put("excluidos_tercerizados", excluidosTercerizados);
		red.put("tiendas_en_la_cuenta", tiendasEnLaCuenta);
		red.put("horas", horasJSON(entradas, entradasEnrutadas, salidasHora, salidasHoraEnrutadas));
		red.put("soltados", soltadosJSON(tiendasConSoltados > 0, enr,
				rankingJSON(ranking), detalleJSON(detalle)));
		return (red);
	}

	@SuppressWarnings("unchecked")
	private static JSONArray rankingJSON(final ArrayList<FilaRanking> ranking) {
		final JSONArray a = new JSONArray();
		for (int i = 0; i < ranking.size() && i < MAXIMO_RANKING; i++) {
			final FilaRanking f = ranking.get(i);
			final JSONObject o = new JSONObject();
			o.put("idtienda", f.idTienda);
			o.put("tienda", f.tienda);
			o.put("iddomiciliario", f.d.idDomiciliario);
			o.put("domiciliario", nombreVisible(f.d.nombre, f.d.idDomiciliario));
			o.put("sugeridos", f.d.sugeridos);
			o.put("llevados", f.d.llevados);
			o.put("soltados", f.d.soltados);
			o.put("libres", f.d.libres);
			o.put("libres_otro", f.d.libresOtro);
			o.put("porcentaje", porcentaje(f.d.soltados, f.d.sugeridos));
			o.put("porcentaje_no_cumplido", porcentaje(f.d.soltados + f.d.libres, f.d.sugeridos));
			a.add(o);
		}
		return (a);
	}

	@SuppressWarnings("unchecked")
	private static JSONArray detalleJSON(final ArrayList<FilaDetalle> detalle) {
		final JSONArray a = new JSONArray();
		for (int i = 0; i < detalle.size() && i < MAXIMO_DETALLE; i++) {
			final FilaDetalle f = detalle.get(i);
			final JSONObject o = new JSONObject();
			o.put("tienda", f.tienda);
			o.put("fecha_hora", f.s.fechaHora);
			o.put("iddomiciliario", f.s.idDomiciliario);
			o.put("domiciliario", nombreVisible(f.s.domiciliario, f.s.idDomiciliario));
			o.put("idpedido", f.s.idPedido);
			o.put("motivo", f.s.motivo);
			a.add(o);
		}
		return (a);
	}

	@SuppressWarnings("unchecked")
	private static JSONObject soltadosJSON(final boolean disponible, final ReporteEnrutamientoDAO.Desglose g,
			final JSONArray ranking, final JSONArray detalle) {
		final JSONObject o = new JSONObject();
		o.put("disponible", disponible);
		o.put("sugeridos", g.sugeridos);
		o.put("llevados", g.llevados);
		o.put("soltados", g.soltados);
		o.put("reasignados", g.reasignados);
		o.put("anulados", g.anulados);
		o.put("libres", g.libres);
		o.put("libres_otro", g.libresOtro);
		o.put("libres_sin_dueno", g.libresSinDueno);
		o.put("libres_asignado", g.libresAsignado);
		o.put("rutas_tardias", g.rutasTardias);
		o.put("expiradas_sin_salir", g.expiradasSinSalir);
		o.put("en_curso", g.enCurso);
		o.put("otros", g.otros);
		o.put("sugerencias", g.sugerencias);
		o.put("sugerencias_aceptadas", g.sugerenciasAceptadas);
		o.put("sugerencias_expiradas", g.sugerenciasExpiradas);
		o.put("porcentaje", porcentaje(g.soltados, g.sugeridos));
		o.put("porcentaje_llevados", porcentaje(g.llevados, g.sugeridos));
		o.put("porcentaje_no_cumplido", porcentaje(g.soltados + g.libres, g.sugeridos));
		o.put("porcentaje_aceptacion", porcentaje(g.sugerenciasAceptadas, g.sugerencias));
		o.put("por_domiciliario", ranking);
		o.put("detalle", detalle);
		return (o);
	}

	// =======================================================================
	// Una tienda
	// =======================================================================

	@SuppressWarnings("unchecked")
	private static JSONObject tiendaJSON(final ReporteEnrutamientoDAO.ResultadoTienda r) {
		final boolean responde = ReporteEnrutamientoDAO.ESTADO_OK.equals(r.estado);
		final JSONObject t = new JSONObject();
		t.put("idtienda", r.idTienda);
		t.put("tienda", r.tienda);
		t.put("estado", r.estado);
		t.put("error", r.error);
		t.put("fuente", r.fuente);
		t.put("datos_hasta", r.datosHasta);
		t.put("motivo_vivo", r.motivoVivo);
		t.put("instalado", r.enrutamientoInstalado);
		t.put("pedidos", r.pedidos);
		t.put("enrutados", r.enrutados);
		t.put("no_enrutados", r.pedidos - r.enrutados);
		t.put("porcentaje", responde && r.enrutamientoInstalado ? porcentaje(r.enrutados, r.pedidos) : 0.0);
		t.put("salidas", r.salidasTotal);
		t.put("sin_salida", r.pedidos - r.salidasTotal > 0 ? r.pedidos - r.salidasTotal : 0);
		t.put("excluidos_plataforma", r.excluidosPlataforma);
		t.put("excluidos_tercerizados", r.excluidosTercerizados);
		t.put("horas", horasJSON(r.entradas, r.entradasEnrutadas, r.salidas, r.salidasEnrutadas));

		final ArrayList<FilaRanking> ranking = new ArrayList<FilaRanking>();
		for (final ReporteEnrutamientoDAO.PorDomiciliario d : r.porDomiciliario) {
			final FilaRanking f = new FilaRanking();
			f.idTienda = r.idTienda;
			f.tienda = r.tienda;
			f.d = d;
			ranking.add(f);
		}
		final ArrayList<FilaDetalle> detalle = new ArrayList<FilaDetalle>();
		for (final ReporteEnrutamientoDAO.Soltado s : r.detalleSoltados) {
			final FilaDetalle f = new FilaDetalle();
			f.tienda = r.tienda;
			f.s = s;
			detalle.add(f);
		}
		t.put("soltados", soltadosJSON(responde && r.soltadosDisponible, r.enrutamiento,
				rankingJSON(ranking), detalleJSON(detalle)));
		return (t);
	}

	/** Las 24 horas del dia, una fila por hora, con sus cuatro cifras. */
	@SuppressWarnings("unchecked")
	private static JSONArray horasJSON(final int[] entradas, final int[] entradasEnrutadas, final int[] salidas,
			final int[] salidasEnrutadas) {
		final JSONArray horas = new JSONArray();
		for (int h = 0; h < 24; h++) {
			final JSONObject o = new JSONObject();
			o.put("hora", h);
			o.put("entradas", entradas[h]);
			o.put("entradas_enrutadas", entradasEnrutadas[h]);
			o.put("salidas", salidas[h]);
			o.put("salidas_enrutadas", salidasEnrutadas[h]);
			horas.add(o);
		}
		return (horas);
	}

	/** Un porcentaje con un decimal. Con total cero devuelve 0: la pantalla decide como decirlo. */
	private static double porcentaje(final int parte, final int total) {
		if (total <= 0) {
			return (0.0);
		}
		return (Math.round(parte * 1000.0 / total) / 10.0);
	}

	private static String nombreVisible(final String nombre, final int id) {
		if (nombre != null && nombre.trim().length() > 0) {
			return (nombre.trim());
		}
		return (id > 0 ? "Domiciliario " + id : "Sin asignar");
	}

	/**
	 * Los nombres que la tienda no pudo dar -siempre en el datamart, que no tiene
	 * la tabla usuario- se completan de general.empleado, en una sola consulta.
	 */
	private static void completarNombres(final ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> resultados) {
		final Set<Integer> faltan = new HashSet<Integer>();
		for (final ReporteEnrutamientoDAO.ResultadoTienda r : resultados) {
			for (final ReporteEnrutamientoDAO.PorDomiciliario d : r.porDomiciliario) {
				if (d.nombre.length() == 0 && d.idDomiciliario > 0) {
					faltan.add(Integer.valueOf(d.idDomiciliario));
				}
			}
			for (final ReporteEnrutamientoDAO.Soltado s : r.detalleSoltados) {
				if (s.domiciliario.length() == 0 && s.idDomiciliario > 0) {
					faltan.add(Integer.valueOf(s.idDomiciliario));
				}
			}
		}
		if (faltan.isEmpty()) {
			return;
		}
		final HashMap<Integer, String> nombres = ReporteEnrutamientoDAO.nombresDeEmpleados(faltan);
		for (final ReporteEnrutamientoDAO.ResultadoTienda r : resultados) {
			for (final ReporteEnrutamientoDAO.PorDomiciliario d : r.porDomiciliario) {
				if (d.nombre.length() == 0 && nombres.get(Integer.valueOf(d.idDomiciliario)) != null) {
					d.nombre = nombres.get(Integer.valueOf(d.idDomiciliario));
				}
			}
			for (final ReporteEnrutamientoDAO.Soltado s : r.detalleSoltados) {
				if (s.domiciliario.length() == 0 && nombres.get(Integer.valueOf(s.idDomiciliario)) != null) {
					s.domiciliario = nombres.get(Integer.valueOf(s.idDomiciliario));
				}
			}
		}
	}

	// =======================================================================
	// El barrido
	// =======================================================================

	private static ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> barrerTiendas(final int idTienda,
			final String desde, final String hasta, final Logger logger) {
		final ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> resultados =
				new ArrayList<ReporteEnrutamientoDAO.ResultadoTienda>();
		final ArrayList<Tienda> todas = TiendaDAO.obtenerTiendasFuncionales();
		if (todas == null || todas.isEmpty()) {
			return (resultados);
		}
		//Solo las que tienen computador al que preguntarle (hosbd), y si se pidio
		//una sola, solo esa.
		final ArrayList<Tienda> aConsultar = new ArrayList<Tienda>();
		for (final Tienda t : todas) {
			if (t.getHosbd() == null || t.getHosbd().trim().length() == 0) {
				continue;
			}
			if (idTienda != 0 && idTienda != t.getIdTienda()) {
				continue;
			}
			aConsultar.add(t);
		}
		if (aConsultar.isEmpty()) {
			return (resultados);
		}

		final ExecutorService executor = Executors.newFixedThreadPool(HILOS);
		try {
			final List<Callable<ReporteEnrutamientoDAO.ResultadoTienda>> tareas =
					new ArrayList<Callable<ReporteEnrutamientoDAO.ResultadoTienda>>();
			for (final Tienda t : aConsultar) {
				tareas.add(new Callable<ReporteEnrutamientoDAO.ResultadoTienda>() {
					public ReporteEnrutamientoDAO.ResultadoTienda call() {
						return (ReporteEnrutamientoDAO.consultarTienda(t.getIdTienda(), t.getNombreTienda(),
								t.getHosbd().trim(), desde, hasta));
					}
				});
			}
			final List<Future<ReporteEnrutamientoDAO.ResultadoTienda>> futuros =
					executor.invokeAll(tareas, SEGUNDOS_ESPERA, TimeUnit.SECONDS);
			for (int i = 0; i < futuros.size(); i++) {
				try {
					resultados.add(futuros.get(i).get());
				} catch (final Exception e) {
					//Se paso del tiempo (o reviento): NO es que no tuvo pedidos.
					logger.error("ReporteEnrutamientoCtrl: tienda sin respuesta, " + e.toString());
					final ReporteEnrutamientoDAO.ResultadoTienda fallo = new ReporteEnrutamientoDAO.ResultadoTienda();
					fallo.idTienda = aConsultar.get(i).getIdTienda();
					fallo.tienda = aConsultar.get(i).getNombreTienda();
					fallo.estado = ReporteEnrutamientoDAO.ESTADO_TARDO;
					fallo.error = "La tienda no alcanzo a responder en " + SEGUNDOS_ESPERA + " segundos";
					resultados.add(fallo);
				}
			}
		} catch (final Exception e) {
			logger.error("ReporteEnrutamientoCtrl.barrerTiendas: " + e.toString());
		} finally {
			executor.shutdownNow();
		}

		//Las que no contestaron se piden al datamart. Una consulta corta y local al
		//central, asi que va en serie.
		for (int i = 0; i < resultados.size(); i++) {
			final ReporteEnrutamientoDAO.ResultadoTienda vivo = resultados.get(i);
			final boolean noContesto = ReporteEnrutamientoDAO.ESTADO_SIN_CONEXION.equals(vivo.estado)
					|| ReporteEnrutamientoDAO.ESTADO_TARDO.equals(vivo.estado);
			if (!noContesto) {
				continue;
			}
			final ReporteEnrutamientoDAO.ResultadoTienda dm = ReporteEnrutamientoDAO.consultarTiendaDatamart(
					vivo.idTienda, vivo.tienda, desde, hasta, vivo.estado);
			if (ReporteEnrutamientoDAO.ESTADO_OK.equals(dm.estado)) {
				resultados.set(i, dm);
			} else if (dm.datosHasta.length() > 0) {
				//No sirvio para el rango, pero se le dice a la pantalla hasta donde llega.
				vivo.datosHasta = dm.datosHasta;
			}
		}
		return (resultados);
	}
}
