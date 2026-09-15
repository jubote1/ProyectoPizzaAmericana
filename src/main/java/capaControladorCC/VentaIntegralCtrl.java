package capaControladorCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.VentaIntegralCategoriaDAO;
import capaDAOCC.VentaIntegralResumenDAO;
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

	/**
	 * Resumen de Venta Integral para un rango de semanas ya cerradas por el
	 * proceso de Servicios. idTienda en 0 trae todas las tiendas.
	 *
	 * Ademas del detalle tienda x categoria, calcula por tienda un coeficiente
	 * de variacion (CV) de "que tan pareja" es entre categorias: por categoria
	 * se saca un indice = cantidad_tienda / promedio_de_la_red_en_esa_categoria
	 * (1.0 = promedio), y el CV es la desviacion estandar de esos indices sobre
	 * su promedio. CV bajo = desempeno parejo entre categorias; CV alto =
	 * destaca en unas y flojea en otras. El promedio de la red y el CV siempre
	 * se calculan contra TODAS las tiendas, aunque se este consultando una sola,
	 * para que el indice siga siendo comparable.
	 */
	@SuppressWarnings("unchecked")
	public String consultarResumenVentaIntegral(int idTienda, String semanaIniISO, String semanaFinISO) {
		ArrayList<VentaIntegralResumenSemana> todasLasTiendas = VentaIntegralResumenDAO.consultarResumen(0,
				semanaIniISO, semanaFinISO);

		HashMap<Integer, Double> sumaPorCategoria = new HashMap<>();
		HashMap<Integer, Integer> conteoPorCategoria = new HashMap<>();
		for (VentaIntegralResumenSemana res : todasLasTiendas) {
			sumaPorCategoria.merge(res.getIdCategoria(), res.getCantidadTotal(), Double::sum);
			conteoPorCategoria.merge(res.getIdCategoria(), 1, Integer::sum);
		}
		HashMap<Integer, Double> promedioPorCategoria = new HashMap<>();
		for (Integer idCat : sumaPorCategoria.keySet()) {
			promedioPorCategoria.put(idCat, sumaPorCategoria.get(idCat) / conteoPorCategoria.get(idCat));
		}

		JSONArray detalleJSON = new JSONArray();
		HashMap<Integer, ArrayList<Double>> indicesPorTienda = new HashMap<>();
		HashMap<Integer, String> nombrePorTienda = new HashMap<>();
		for (VentaIntegralResumenSemana res : todasLasTiendas) {
			nombrePorTienda.put(res.getIdTienda(), res.getNombreTienda());
			double promedioRed = promedioPorCategoria.getOrDefault(res.getIdCategoria(), 0.0);
			if (promedioRed > 0) {
				indicesPorTienda.computeIfAbsent(res.getIdTienda(), k -> new ArrayList<>())
						.add(res.getCantidadTotal() / promedioRed);
			}
			if (idTienda == 0 || idTienda == res.getIdTienda()) {
				JSONObject filaJSON = new JSONObject();
				filaJSON.put("idtienda", res.getIdTienda());
				filaJSON.put("nombretienda", res.getNombreTienda());
				filaJSON.put("idcategoria", res.getIdCategoria());
				filaJSON.put("nombrecategoria", res.getNombreCategoria());
				filaJSON.put("cantidadtienda", res.getCantidadTienda());
				filaJSON.put("cantidadcontactcenter", res.getCantidadContactCenter());
				filaJSON.put("cantidadtotal", res.getCantidadTotal());
				filaJSON.put("indicereddecategoria", promedioRed > 0 ? (res.getCantidadTotal() / promedioRed) : 0);
				detalleJSON.add(filaJSON);
			}
		}

		JSONArray dispersionJSON = new JSONArray();
		for (Integer idTiendaTemp : indicesPorTienda.keySet()) {
			if (idTienda != 0 && idTienda != idTiendaTemp) {
				continue;
			}
			ArrayList<Double> indices = indicesPorTienda.get(idTiendaTemp);
			double promedioIndices = 0;
			for (Double indice : indices) {
				promedioIndices += indice;
			}
			promedioIndices = promedioIndices / indices.size();
			double varianza = 0;
			for (Double indice : indices) {
				varianza += Math.pow(indice - promedioIndices, 2);
			}
			varianza = varianza / indices.size();
			double desviacion = Math.sqrt(varianza);
			double cv = promedioIndices > 0 ? (desviacion / promedioIndices) : 0;
			JSONObject filaJSON = new JSONObject();
			filaJSON.put("idtienda", idTiendaTemp);
			filaJSON.put("nombretienda", nombrePorTienda.get(idTiendaTemp));
			filaJSON.put("categoriasevaluadas", indices.size());
			filaJSON.put("coeficientevariacion", cv);
			dispersionJSON.add(filaJSON);
		}

		JSONObject respuestaJSON = new JSONObject();
		respuestaJSON.put("detalle", detalleJSON);
		respuestaJSON.put("dispersion", dispersionJSON);
		return (respuestaJSON.toJSONString());
	}

}
