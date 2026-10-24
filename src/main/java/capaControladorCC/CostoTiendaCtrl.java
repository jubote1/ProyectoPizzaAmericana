package capaControladorCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.CostoTiendaDAO;

/**
 * El costo de las tiendas por concepto y por semana: lo que hoy es la pestana
 * PORCENTAJE del libro de Excel.
 *
 * Arma la matriz que pinta la pantalla: por cada tienda, cada concepto y cada semana,
 * cuanto costo y que porcentaje de la venta de esa tienda en esa semana fue.
 *
 * LO QUE ESTA PANTALLA NO VA A HACER ES PARECERSE AL EXCEL
 *
 * Los trece valores de cada tienda en el Excel estan digitados; medidos contra los
 * datos reales, cada tienda cuadra con un corte distinto. Aqui las once salen del
 * mismo periodo, asi que las cifras van a diferir de la pestana vieja, y esa
 * diferencia es la ganancia, no un error.
 *
 * CADA CONCEPTO DICE DE DONDE SALIO
 *
 * El origen viaja hasta la pantalla en cada fila. Un cero de un concepto DIGITADO que
 * nadie ha digitado no es lo mismo que un cero de un concepto EGRESO que de verdad no
 * tuvo movimientos, y quien lee el reporte tiene que poder distinguirlos.
 */
public class CostoTiendaCtrl {

	/** Lo que se devuelve cuando el rango pedido no tiene ni una semana con venta. */
	private static final String SIN_SEMANAS =
			"{\"respuesta\":\"N\",\"mensaje\":\"Ese rango no tiene semanas con venta registrada.\"}";

	/**
	 * La matriz de costos del rango.
	 *
	 * Las fechas van en aaaa-mm-dd. idTienda en 0 trae todas las tiendas.
	 *
	 * El rango se recorta a las semanas que tengan venta: son las unicas contra las que
	 * se puede sacar un porcentaje, y si se dejaran las otras saldrian conceptos con
	 * costo y sin denominador.
	 */
	public String matriz(final String desde, final String hasta, final int idTienda) {
		final ArrayList<String> semanas = CostoTiendaDAO.semanasConVenta(desde, hasta);
		if (semanas.isEmpty()) {
			return SIN_SEMANAS;
		}
		final ArrayList<CostoTiendaDAO.Concepto> conceptos = CostoTiendaDAO.listarConceptos();

		// idconcepto -> origen, para marcar cada celda con de donde salio.
		final HashMap<Integer, String> origenPorConcepto = new HashMap<Integer, String>();
		int idMensajeria = 0;
		int idDatafono = 0;
		for (final CostoTiendaDAO.Concepto c : conceptos) {
			origenPorConcepto.put(Integer.valueOf(c.idConcepto), c.origen);
			if (CostoTiendaDAO.ORIGEN_TEMPORAL.equals(c.origen)) {
				idMensajeria = c.idConcepto;
			} else if (CostoTiendaDAO.ORIGEN_DATAFONO.equals(c.origen)) {
				idDatafono = c.idConcepto;
			}
		}

		final double tarifa = CostoTiendaDAO.tarifaDatafono();

		// tienda|concepto|semana -> valor. Se junta todo en un solo mapa porque las
		// fuentes son distintas pero la celda es la misma.
		final LinkedHashMap<String, Double> celdas = new LinkedHashMap<String, Double>();
		acumular(celdas, CostoTiendaDAO.egresosPorSemana(desde, hasta, idTienda));
		if (idMensajeria > 0) {
			acumular(celdas, CostoTiendaDAO.mensajeriaPorSemana(desde, hasta, idTienda, idMensajeria));
		}
		if (idDatafono > 0) {
			acumular(celdas, CostoTiendaDAO.datafonoPorSemana(desde, hasta, idTienda, idDatafono, tarifa));
		}
		acumular(celdas, CostoTiendaDAO.digitadoPorSemana(desde, hasta, idTienda));

		// tienda|semana -> venta, el denominador.
		final HashMap<String, Double> ventas = new HashMap<String, Double>();
		for (final CostoTiendaDAO.Valor v : CostoTiendaDAO.ventaPorSemana(desde, hasta, idTienda)) {
			ventas.put(v.idTienda + "|" + v.semana, Double.valueOf(v.valor));
		}

		final JSONArray filas = new JSONArray();
		for (final String clave : celdas.keySet()) {
			final String[] partes = clave.split("\\|");
			final JSONObject j = new JSONObject();
			final double valor = celdas.get(clave).doubleValue();
			final Double venta = ventas.get(partes[0] + "|" + partes[2]);
			j.put("idtienda", Integer.valueOf(partes[0]));
			j.put("idconcepto", Integer.valueOf(partes[1]));
			j.put("semana", partes[2]);
			j.put("valor", Double.valueOf(valor));
			j.put("venta", venta == null ? Double.valueOf(0) : venta);
			// Sin venta no hay porcentaje. Mandar 0 seria decir que no costo nada.
			if (venta != null && venta.doubleValue() > 0) {
				j.put("porcentaje", Double.valueOf(valor * 100.0D / venta.doubleValue()));
			}
			j.put("origen", origenPorConcepto.get(Integer.valueOf(partes[1])));
			filas.add(j);
		}

		final JSONArray jsonConceptos = new JSONArray();
		for (final CostoTiendaDAO.Concepto c : conceptos) {
			final JSONObject j = new JSONObject();
			j.put("idconcepto", Integer.valueOf(c.idConcepto));
			j.put("nombre", c.nombre);
			j.put("orden", Integer.valueOf(c.orden));
			j.put("origen", c.origen);
			jsonConceptos.add(j);
		}

		final JSONArray jsonSemanas = new JSONArray();
		for (final String s : semanas) {
			jsonSemanas.add(s);
		}

		// Las tiendas van siempre todas las que tengan venta, aunque se haya pedido una
		// sola: es lo que llena el selector de la pantalla.
		final JSONArray jsonTiendas = new JSONArray();
		final LinkedHashMap<Integer, String> tiendas = CostoTiendaDAO.tiendasConVenta(desde, hasta);
		for (final Integer id : tiendas.keySet()) {
			final JSONObject j = new JSONObject();
			j.put("idtienda", id);
			j.put("nombre", tiendas.get(id));
			jsonTiendas.add(j);
		}

		final JSONObject salida = new JSONObject();
		salida.put("respuesta", "S");
		salida.put("tiendas", jsonTiendas);
		salida.put("conceptos", jsonConceptos);
		salida.put("semanas", jsonSemanas);
		salida.put("filas", filas);
		salida.put("tarifadatafono", Double.valueOf(tarifa));
		// La pantalla necesita saberlo para no mostrar un cero enganoso.
		salida.put("datafonoparametrizado", tarifa > 0 ? "S" : "N");
		return salida.toJSONString();
	}

	/**
	 * El detalle de un concepto en una tienda y un rango: de donde salio el numero.
	 *
	 * Cada origen trae cosas distintas -los egresos traen movimientos, la mensajeria
	 * trae turnos, el datafono trae cierres diarios- pero todos caben en fecha,
	 * descripcion, referencia y valor, que es lo que pinta la pantalla.
	 *
	 * Un concepto MIXTO trae las dos cosas: lo digitado del mes y los egresos que le
	 * correspondan. Por eso no es un if-else sino dos agregados.
	 */
	public String detalle(final String desde, final String hasta, final int idTienda,
			final int idConcepto) {
		String origen = "";
		for (final CostoTiendaDAO.Concepto c : CostoTiendaDAO.listarConceptos()) {
			if (c.idConcepto == idConcepto) {
				origen = c.origen;
				break;
			}
		}

		final ArrayList<CostoTiendaDAO.Detalle> renglones = new ArrayList<CostoTiendaDAO.Detalle>();
		if (CostoTiendaDAO.ORIGEN_EGRESO.equals(origen) || CostoTiendaDAO.ORIGEN_MIXTO.equals(origen)) {
			renglones.addAll(CostoTiendaDAO.detalleEgresos(desde, hasta, idTienda, idConcepto));
		}
		if (CostoTiendaDAO.ORIGEN_DIGITADO.equals(origen) || CostoTiendaDAO.ORIGEN_MIXTO.equals(origen)) {
			renglones.addAll(CostoTiendaDAO.detalleDigitado(desde, hasta, idTienda, idConcepto));
		}
		if (CostoTiendaDAO.ORIGEN_TEMPORAL.equals(origen)) {
			renglones.addAll(CostoTiendaDAO.detalleTemporales(desde, hasta, idTienda));
		}
		if (CostoTiendaDAO.ORIGEN_DATAFONO.equals(origen)) {
			final double tarifa = CostoTiendaDAO.tarifaDatafono();
			if (tarifa > 0) {
				renglones.addAll(CostoTiendaDAO.detalleDatafono(desde, hasta, idTienda, tarifa));
			}
		}

		final JSONArray lista = new JSONArray();
		double total = 0;
		for (final CostoTiendaDAO.Detalle d : renglones) {
			final JSONObject j = new JSONObject();
			j.put("fecha", d.fecha);
			j.put("descripcion", d.descripcion);
			j.put("referencia", d.referencia);
			j.put("valor", Double.valueOf(d.valor));
			lista.add(j);
			total += d.valor;
		}

		final JSONObject salida = new JSONObject();
		salida.put("respuesta", "S");
		salida.put("origen", origen);
		salida.put("renglones", lista);
		salida.put("total", Double.valueOf(total));
		// La mensajeria se recalcula aqui para mostrarla, pero el total de la matriz lo
		// escribio Servicios con las tarifas de ese dia. Pueden no dar igual, y la
		// pantalla tiene que avisarlo en vez de dejar que alguien crea que hay un error.
		salida.put("totalaproximado", CostoTiendaDAO.ORIGEN_TEMPORAL.equals(origen) ? "S" : "N");
		// Lo digitado es del MES completo: si el rango corta el mes, el detalle muestra
		// el mes entero mientras la matriz muestra solo las semanas del rango.
		salida.put("digitadoesdelmes",
				CostoTiendaDAO.ORIGEN_DIGITADO.equals(origen)
						|| CostoTiendaDAO.ORIGEN_MIXTO.equals(origen) ? "S" : "N");
		return salida.toJSONString();
	}

	/** Los conceptos y de donde sale cada uno, para la pantalla de configuracion. */
	public String listarConceptos() {
		final JSONArray lista = new JSONArray();
		for (final CostoTiendaDAO.Concepto c : CostoTiendaDAO.listarConceptos()) {
			final JSONObject j = new JSONObject();
			j.put("idconcepto", Integer.valueOf(c.idConcepto));
			j.put("nombre", c.nombre);
			j.put("orden", Integer.valueOf(c.orden));
			j.put("origen", c.origen);
			lista.add(j);
		}
		return lista.toJSONString();
	}

	/** Guarda uno de los conceptos que toca digitar. */
	public String guardarDigitado(final int idTienda, final int idConcepto, final int anio,
			final int mes, final double valor, final String usuario) {
		final JSONObject j = new JSONObject();
		final String problema = validarDigitado(idTienda, idConcepto, anio, mes, valor);
		if (problema != null) {
			j.put("respuesta", "N");
			j.put("mensaje", problema);
			return j.toJSONString();
		}
		final boolean exito = CostoTiendaDAO.guardarDigitado(idTienda, idConcepto, anio, mes, valor,
				usuario);
		j.put("respuesta", exito ? "S" : "N");
		j.put("mensaje", exito ? "" : "No se pudo guardar el valor.");
		return j.toJSONString();
	}

	/** Devuelve el primer problema que encuentre, o null si el valor esta bien. */
	private String validarDigitado(final int idTienda, final int idConcepto, final int anio,
			final int mes, final double valor) {
		if (idTienda <= 0) {
			return "Falta la tienda.";
		}
		if (idConcepto <= 0) {
			return "Falta el concepto.";
		}
		if (anio < 2000 || mes < 1 || mes > 12) {
			return "El mes no es valido.";
		}
		if (valor < 0) {
			return "El valor no puede ser negativo.";
		}
		return null;
	}

	/** Suma los valores de una fuente en el mapa de celdas. */
	private void acumular(final LinkedHashMap<String, Double> celdas,
			final ArrayList<CostoTiendaDAO.Valor> valores) {
		for (final CostoTiendaDAO.Valor v : valores) {
			final String clave = v.idTienda + "|" + v.idConcepto + "|" + v.semana;
			final Double previo = celdas.get(clave);
			celdas.put(clave, Double.valueOf(previo == null ? v.valor : previo.doubleValue() + v.valor));
		}
	}
}
