package capaControladorCC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.TiendaDAO;
import capaDAOCC.VentaIntegralCategoriaDAO;
import capaDAOCC.VentaIntegralResumenDAO;
import capaDAOCC.VentaIntegralTiendaDAO;
import capaModeloCC.Tienda;
import capaModeloCC.VentaIntegralCategoria;
import capaModeloCC.VentaIntegralResumenSemana;

/**
 * Capa de controlador de Venta Integral: arma el JSON que consumen las
 * pantallas de Monitoreo (catalogo de categorias, y consulta/analisis del
 * cierre semanal que deja Servicios).
 */
public class VentaIntegralCtrl {

	// ===================================================================
	// Catalogo de categorias
	// ===================================================================

	public String listarCategorias() {
		ArrayList<VentaIntegralCategoria> categorias = VentaIntegralCategoriaDAO.listarCategorias();
		JSONArray listJSON = new JSONArray();
		for (VentaIntegralCategoria cat : categorias) {
			listJSON.add(categoriaAJSON(cat));
		}
		return (listJSON.toJSONString());
	}

	public String retornarCategoria(int idCategoria) {
		VentaIntegralCategoria cat = VentaIntegralCategoriaDAO.retornarCategoria(idCategoria);
		JSONArray listJSON = new JSONArray();
		listJSON.add(categoriaAJSON(cat));
		return (listJSON.toJSONString());
	}

	public String insertarCategoria(String nombre, String abreviatura, String tipoDato, String medicionTienda,
			String excluyeAnuladosTienda, String filtroEstacionTienda, String medicionCC, int orden,
			String itemsTiendaTexto, String itemsContactCenterTexto) {
		VentaIntegralCategoria cat = new VentaIntegralCategoria(0, nombre, abreviatura, tipoDato, medicionTienda,
				excluyeAnuladosTienda, filtroEstacionTienda, medicionCC, "S", orden);
		cat.setItemsTienda(parsearListaEnteros(itemsTiendaTexto));
		cat.setItemsContactCenter(parsearListaEnteros(itemsContactCenterTexto));
		int idCategoriaIns = VentaIntegralCategoriaDAO.insertarCategoria(cat);
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("idcategoria", idCategoriaIns);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	public String editarCategoria(int idCategoria, String nombre, String abreviatura, String tipoDato,
			String medicionTienda, String excluyeAnuladosTienda, String filtroEstacionTienda, String medicionCC,
			int orden, String itemsTiendaTexto, String itemsContactCenterTexto) {
		VentaIntegralCategoria cat = new VentaIntegralCategoria(idCategoria, nombre, abreviatura, tipoDato,
				medicionTienda, excluyeAnuladosTienda, filtroEstacionTienda, medicionCC, "S", orden);
		cat.setItemsTienda(parsearListaEnteros(itemsTiendaTexto));
		cat.setItemsContactCenter(parsearListaEnteros(itemsContactCenterTexto));
		String resultado = VentaIntegralCategoriaDAO.editarCategoria(cat);
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	public String eliminarCategoria(int idCategoria) {
		String resultado = VentaIntegralCategoriaDAO.eliminarCategoria(idCategoria);
		JSONArray listJSON = new JSONArray();
		JSONObject resultadoJSON = new JSONObject();
		resultadoJSON.put("resultado", resultado);
		listJSON.add(resultadoJSON);
		return (listJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private JSONObject categoriaAJSON(VentaIntegralCategoria cat) {
		JSONObject catJSON = new JSONObject();
		catJSON.put("idcategoria", cat.getIdCategoria());
		catJSON.put("nombre", cat.getNombre());
		catJSON.put("abreviatura", cat.getAbreviatura());
		catJSON.put("tipodato", cat.getTipoDato());
		catJSON.put("mediciontienda", cat.getMedicionTienda());
		catJSON.put("excluyeanuladostienda", cat.getExcluyeAnuladosTienda());
		catJSON.put("filtroestaciontienda", cat.getFiltroEstacionTienda());
		catJSON.put("medicioncc", cat.getMedicionCC());
		catJSON.put("activo", cat.getActivo());
		catJSON.put("orden", cat.getOrden());
		catJSON.put("itemstienda", listaAString(cat.getItemsTienda()));
		catJSON.put("itemscontactcenter", listaAString(cat.getItemsContactCenter()));
		return (catJSON);
	}

	private String listaAString(ArrayList<Integer> valores) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < valores.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(valores.get(i));
		}
		return (sb.toString());
	}

	private ArrayList<Integer> parsearListaEnteros(String texto) {
		ArrayList<Integer> valores = new ArrayList<>();
		if (texto == null) {
			return (valores);
		}
		StringTokenizer tokens = new StringTokenizer(texto, ",; \n\r\t");
		while (tokens.hasMoreTokens()) {
			try {
				valores.add(Integer.parseInt(tokens.nextToken().trim()));
			} catch (Exception e) {
				// Se ignora un token que no sea un numero, en vez de tumbar todo el guardado.
			}
		}
		return (valores);
	}

	// ===================================================================
	// Consulta del cierre semanal
	// ===================================================================

	/** Fila intermedia para poder ordenar por total antes de armar el JSON. */
	private static class FilaTienda {
		int idTienda;
		String nombreTienda;
		HashMap<Integer, Double> porCategoria = new HashMap<>();
		double total;
		double cv;
		int categoriasEvaluadas;
	}

	/**
	 * Resumen de Venta Integral para un rango de semanas ya cerradas por el
	 * proceso de Servicios, en el mismo formato que el correo que envia
	 * Servicios: una fila por tienda (ordenadas de mayor a menor total, para
	 * medir de un vistazo la mejor gestion integral) con sus categorias como
	 * columnas, mas una fila aparte de Contact Center (agregado de toda la red,
	 * sin distinguir tienda -no es algo que la tienda haya ejecutado-) y el
	 * gran total de la red. idTienda en 0 trae todas las tiendas.
	 *
	 * El coeficiente de variacion (CV) mide "que tan pareja" es cada tienda
	 * entre categorias: por categoria se saca un indice = cantidad_tienda /
	 * promedio_de_la_red_en_esa_categoria (1.0 = promedio), y el CV es la
	 * desviacion estandar de esos indices sobre su promedio. Se calcula SOLO
	 * con tiendas reales (nunca con Contact Center, que no es una tienda), y el
	 * promedio de la red siempre sale de TODAS las tiendas aunque se este
	 * consultando una sola, para que el indice siga siendo comparable.
	 *
	 * Tambien incluye la alerta de tiendas que aparentemente no trajeron datos
	 * en el rango (ver VentaIntegralResumenDAO.consultarTiendasSinDatosAparentes).
	 */
	@SuppressWarnings("unchecked")
	public String consultarResumenVentaIntegral(int idTienda, String semanaIniISO, String semanaFinISO) {
		ArrayList<VentaIntegralResumenSemana> todos = VentaIntegralResumenDAO.consultarResumen(0, semanaIniISO,
				semanaFinISO);
		ArrayList<VentaIntegralCategoria> categorias = VentaIntegralCategoriaDAO.listarCategoriasActivas();

		// Separar Contact Center (idtienda centinela) de las tiendas reales: el
		// promedio de red y el CV nunca deben mezclar ese numero, porque no lo
		// ejecuto ninguna tienda en particular.
		HashMap<Integer, HashMap<Integer, Double>> porTiendaCategoria = new HashMap<>();
		HashMap<Integer, String> nombrePorTienda = new HashMap<>();
		HashMap<Integer, Double> ccPorCategoria = new HashMap<>();
		for (VentaIntegralResumenSemana res : todos) {
			if (res.getIdTienda() == VentaIntegralResumenDAO.IDTIENDA_CONTACTCENTER) {
				ccPorCategoria.put(res.getIdCategoria(), res.getCantidadContactCenter());
			} else {
				nombrePorTienda.put(res.getIdTienda(), res.getNombreTienda());
				porTiendaCategoria.computeIfAbsent(res.getIdTienda(), k -> new HashMap<>()).put(res.getIdCategoria(),
						res.getCantidadTienda());
			}
		}

		ArrayList<String> alertas = VentaIntegralResumenDAO.consultarTiendasSinDatosAparentes(semanaIniISO,
				semanaFinISO);
		return (this.armarRespuestaVentaIntegral(porTiendaCategoria, nombrePorTienda, ccPorCategoria, categorias,
				idTienda, alertas));
	}

	/**
	 * Venta Integral calculada EN VIVO para un rango de fechas cualquiera.
	 *
	 * consultarResumenVentaIntegral lee lo que el proceso de los domingos ya
	 * dejo cerrado en venta_integral_resumen_semanal; esta va y le pregunta a
	 * cada tienda en el momento. Es lo que permite pedir la semana en curso o
	 * un rango que no empiece en lunes, y es tambien por lo que se demora: son
	 * once conexiones remotas, una por tienda.
	 *
	 * Una tienda que no responda no tumba el reporte. Queda por fuera de la
	 * tabla y aparece en la alerta, que es la misma casilla donde la otra
	 * consulta muestra las tiendas sin datos. Dejarla en cero seria peor: se
	 * veria como una tienda que no vendio.
	 */
	public String consultarVentaIntegralVivo(int idTienda, String fechaIniISO, String fechaFinISO) {
		ArrayList<VentaIntegralCategoria> categorias = VentaIntegralCategoriaDAO.listarCategoriasActivas();
		HashMap<Integer, HashMap<Integer, Double>> porTiendaCategoria = new HashMap<>();
		HashMap<Integer, String> nombrePorTienda = new HashMap<>();
		HashMap<Integer, Double> ccPorCategoria = new HashMap<>();
		ArrayList<String> alertas = new ArrayList<>();

		for (Tienda tienda : TiendaDAO.obtenerTiendas()) {
			//Una tienda sin hosbd no tiene base propia que consultar. No es una
			//falla y no va a la alerta: simplemente no participa.
			if (tienda.getHosbd() == null || tienda.getHosbd().trim().isEmpty()) {
				continue;
			}
			VentaIntegralTiendaDAO.ResultadoTienda resultado = VentaIntegralTiendaDAO
					.consultarTotalesTienda(tienda.getHosbd(), categorias, fechaIniISO, fechaFinISO);
			if (!resultado.respondio) {
				alertas.add(tienda.getNombreTienda() + " (no respondio)");
				continue;
			}
			nombrePorTienda.put(tienda.getIdTienda(), tienda.getNombreTienda());
			porTiendaCategoria.put(tienda.getIdTienda(), resultado.porCategoria);
		}

		//El contact center no se le pregunta a ninguna tienda: vive en el
		//central y es uno solo para toda la red.
		for (VentaIntegralCategoria categoria : categorias) {
			ccPorCategoria.put(categoria.getIdCategoria(),
					VentaIntegralResumenDAO.consultarTotalContactCenter(categoria, fechaIniISO, fechaFinISO));
		}

		return (this.armarRespuestaVentaIntegral(porTiendaCategoria, nombrePorTienda, ccPorCategoria, categorias,
				idTienda, alertas));
	}

	/**
	 * La parte comun de las dos consultas de Venta Integral: el promedio de la
	 * red, el coeficiente de variacion de cada tienda y el JSON que espera la
	 * pantalla.
	 *
	 * Esta separado para que la pestana de semanas cerradas y la de rango en
	 * vivo den exactamente la misma tabla. Lo unico que cambia entre las dos es
	 * de donde salen los numeros; como se comparan y como se pintan, no.
	 */
	@SuppressWarnings("unchecked")
	private String armarRespuestaVentaIntegral(HashMap<Integer, HashMap<Integer, Double>> porTiendaCategoria,
			HashMap<Integer, String> nombrePorTienda, HashMap<Integer, Double> ccPorCategoria,
			ArrayList<VentaIntegralCategoria> categorias, int idTienda, ArrayList<String> alertas) {
		// Promedio de la red por categoria, SOLO con tiendas reales.
		HashMap<Integer, Double> sumaPorCategoria = new HashMap<>();
		for (HashMap<Integer, Double> categoriasDeUnaTienda : porTiendaCategoria.values()) {
			for (HashMap.Entry<Integer, Double> entrada : categoriasDeUnaTienda.entrySet()) {
				sumaPorCategoria.merge(entrada.getKey(), entrada.getValue(), Double::sum);
			}
		}
		int totalTiendas = porTiendaCategoria.size();
		HashMap<Integer, Double> promedioPorCategoria = new HashMap<>();
		for (Integer idCat : sumaPorCategoria.keySet()) {
			promedioPorCategoria.put(idCat, totalTiendas > 0 ? (sumaPorCategoria.get(idCat) / totalTiendas) : 0.0);
		}

		// Filas por tienda (con CV), filtradas a idTienda si se pidio una sola.
		ArrayList<FilaTienda> filas = new ArrayList<>();
		for (Integer idTiendaTemp : porTiendaCategoria.keySet()) {
			if (idTienda != 0 && idTienda != idTiendaTemp) {
				continue;
			}
			FilaTienda fila = new FilaTienda();
			fila.idTienda = idTiendaTemp;
			fila.nombreTienda = nombrePorTienda.get(idTiendaTemp);
			fila.porCategoria = porTiendaCategoria.get(idTiendaTemp);
			ArrayList<Double> indices = new ArrayList<>();
			for (HashMap.Entry<Integer, Double> entrada : fila.porCategoria.entrySet()) {
				fila.total += entrada.getValue();
				double promedioRed = promedioPorCategoria.getOrDefault(entrada.getKey(), 0.0);
				if (promedioRed > 0) {
					indices.add(entrada.getValue() / promedioRed);
				}
			}
			fila.categoriasEvaluadas = indices.size();
			fila.cv = this.coeficienteVariacion(indices);
			filas.add(fila);
		}
		Collections.sort(filas, new Comparator<FilaTienda>() {
			public int compare(FilaTienda a, FilaTienda b) {
				return (Double.compare(b.total, a.total));
			}
		});

		JSONArray categoriasJSON = new JSONArray();
		for (VentaIntegralCategoria cat : categorias) {
			JSONObject catJSON = new JSONObject();
			catJSON.put("idcategoria", cat.getIdCategoria());
			catJSON.put("nombre", cat.getNombre());
			categoriasJSON.add(catJSON);
		}

		JSONArray filasJSON = new JSONArray();
		for (FilaTienda fila : filas) {
			filasJSON.add(this.filaAJSON(fila.idTienda, fila.nombreTienda, fila.porCategoria, fila.total, fila.cv,
					fila.categoriasEvaluadas));
		}

		double totalCC = 0;
		for (Double valor : ccPorCategoria.values()) {
			totalCC += valor;
		}
		JSONObject contactCenterJSON = this.filaAJSON(VentaIntegralResumenDAO.IDTIENDA_CONTACTCENTER,
				"Contact Center", ccPorCategoria, totalCC, 0, 0);

		HashMap<Integer, Double> granTotalPorCategoria = new HashMap<>(sumaPorCategoria);
		for (HashMap.Entry<Integer, Double> entrada : ccPorCategoria.entrySet()) {
			granTotalPorCategoria.merge(entrada.getKey(), entrada.getValue(), Double::sum);
		}
		double granTotal = totalCC;
		for (Double valor : sumaPorCategoria.values()) {
			granTotal += valor;
		}
		JSONObject granTotalJSON = this.filaAJSON(-1, "Total Red", granTotalPorCategoria, granTotal, 0, 0);

		JSONArray alertaJSON = new JSONArray();
		for (String nombreTienda : alertas) {
			alertaJSON.add(nombreTienda);
		}

		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("categorias", categoriasJSON);
		respuestaJSON.put("filas", filasJSON);
		respuestaJSON.put("contactcenter", contactCenterJSON);
		respuestaJSON.put("grantotal", granTotalJSON);
		respuestaJSON.put("alertatiendassindatos", alertaJSON);
		return (respuestaJSON.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private JSONObject filaAJSON(int idTienda, String nombreTienda, HashMap<Integer, Double> porCategoria,
			double total, double cv, int categoriasEvaluadas) {
		JSONObject filaJSON = new JSONObject();
		filaJSON.put("idtienda", idTienda);
		filaJSON.put("nombretienda", nombreTienda);
		JSONObject porCategoriaJSON = new JSONObject();
		for (HashMap.Entry<Integer, Double> entrada : porCategoria.entrySet()) {
			porCategoriaJSON.put(String.valueOf(entrada.getKey()), entrada.getValue());
		}
		filaJSON.put("porcategoria", porCategoriaJSON);
		filaJSON.put("total", total);
		filaJSON.put("coeficientevariacion", cv);
		filaJSON.put("categoriasevaluadas", categoriasEvaluadas);
		return (filaJSON);
	}

	/** Desviacion estandar de los indices sobre su promedio. 0 si no hay indices o el promedio es 0. */
	private double coeficienteVariacion(ArrayList<Double> indices) {
		if (indices.isEmpty()) {
			return (0);
		}
		double promedio = 0;
		for (Double indice : indices) {
			promedio += indice;
		}
		promedio = promedio / indices.size();
		if (promedio == 0) {
			return (0);
		}
		double varianza = 0;
		for (Double indice : indices) {
			varianza += Math.pow(indice - promedio, 2);
		}
		varianza = varianza / indices.size();
		return (Math.sqrt(varianza) / promedio);
	}

}
