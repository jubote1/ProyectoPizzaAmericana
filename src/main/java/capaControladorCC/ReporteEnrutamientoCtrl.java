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

import capaDAOCC.ReporteEnrutamientoDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * Arma el reporte de uso del enrutamiento.
 *
 * PARA QUE EXISTE
 *
 * El enrutamiento de domiciliarios solo sirve si las tiendas lo usan, y para
 * impulsarlo hay que saber dos cosas: que porcentaje de los domicilios propios
 * salen dentro de una ruta sugerida, y en que momentos del dia se usa y en cuales
 * no. Las dos salen de la base local de cada tienda, no del central, asi que hay
 * que ir a preguntarle a cada una (ver ReporteEnrutamientoDAO para las
 * definiciones exactas).
 *
 * EN PARALELO, COMO DesempenoDomiciliarioCtrl
 *
 * obtenerConexionBDTiendaRemota espera hasta 10 segundos por tienda; en serie,
 * una tienda apagada ya se siente. En paralelo el peor caso es el de la tienda
 * mas lenta y no la suma de todas.
 *
 * LA QUE NO CONTESTA NO ES CERO
 *
 * Una tienda sin conexion, o que se paso del tiempo, sale marcada como tal y NO
 * entra en los totales de la red: contarla como cero domicilios haria ver peor a
 * la red entera por una tienda apagada. Lo mismo con una tienda que todavia no
 * tiene el enrutamiento instalado: sus pedidos se muestran, pero no entran al
 * porcentaje de uso, porque "no puede usarlo" no es lo mismo que "no lo usa".
 */
public class ReporteEnrutamientoCtrl {

	/** Seis a la vez, como el resto de los barridos de tiendas. */
	private static final int HILOS = 6;

	/** Tope para que una tienda trabada no deje la pantalla colgada. */
	private static final int SEGUNDOS_ESPERA = 60;

	/** El rango maximo que se acepta: mas largo que esto es pedirle a las tiendas una consulta enorme. */
	public static final int DIAS_MAXIMOS = 92;

	@SuppressWarnings("unchecked")
	public static String consultar(final int idTienda, final String desde, final String hasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<ReporteEnrutamientoDAO.ResultadoTienda> resultados = barrerTiendas(idTienda, desde, hasta,
				logger);

		//Los totales de la red: solo las tiendas que respondieron Y tienen el
		//enrutamiento instalado. Las otras se muestran, pero no entran a la cuenta.
		int pedidos = 0;
		int enrutados = 0;
		int salidas = 0;
		int salidasEnrutadas = 0;
		int excluidosPlataforma = 0;
		int excluidosTercerizados = 0;
		int tiendasEnLaCuenta = 0;
		final int[] entradas = new int[24];
		final int[] entradasEnrutadas = new int[24];
		final int[] salidasHora = new int[24];
		final int[] salidasHoraEnrutadas = new int[24];

		final JSONArray tiendasJSON = new JSONArray();
		for (final ReporteEnrutamientoDAO.ResultadoTienda r : resultados) {
			final boolean responde = ReporteEnrutamientoDAO.ESTADO_OK.equals(r.estado);
			final boolean entraEnLaCuenta = responde && r.enrutamientoInstalado;
			if (responde) {
				excluidosPlataforma = excluidosPlataforma + r.excluidosPlataforma;
				excluidosTercerizados = excluidosTercerizados + r.excluidosTercerizados;
			}
			if (entraEnLaCuenta) {
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

			final JSONObject t = new JSONObject();
			t.put("idtienda", r.idTienda);
			t.put("tienda", r.tienda);
			t.put("estado", r.estado);
			t.put("error", r.error);
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
			tiendasJSON.add(t);
		}

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

		final JSONObject respuesta = new JSONObject();
		respuesta.put("desde", desde);
		respuesta.put("hasta", hasta);
		respuesta.put("red", red);
		respuesta.put("tiendas", tiendasJSON);
		return (respuesta.toJSONString());
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
		return (resultados);
	}
}
