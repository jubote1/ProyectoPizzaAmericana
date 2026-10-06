package capaControladorCC;

import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.FacturacionElectronicaDAO;
import capaDAOCC.FacturacionElectronicaDAO.Cifras;
import capaDAOCC.FacturacionElectronicaDAO.NotaDetalle;
import capaDAOCC.FacturacionElectronicaDAO.ResumenTienda;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Tienda;

/**
 * El reporte mensual de facturacion electronica: por tienda, cuanto se facturo, cuantas notas credito hubo y cual es
 * el neto facturado, para conciliar contra lo que tiene la contadora.
 *
 * Todo sale del datamart (ver FacturacionElectronicaDAO). Si la replica no ha copiado todos los dias del mes de una
 * tienda, el reporte lo dice con "dias_con_datos": un mes que parece completo y no lo es seria peor que no tener
 * reporte.
 */
public class FacturacionElectronicaCtrl {

	/** @param mes yyyy-MM */
	public static String consultar(final String mes) {
		final JSONObject respuesta = new JSONObject();
		if (!mesValido(mes)) {
			respuesta.put("error", "Mes invalido: use el formato aaaa-mm");
			return respuesta.toJSONString();
		}
		final String desde = mes + "-01";
		final String hasta = mes + "-31";
		final int diasDelMes = diasDelMes(mes);
		final Map<Integer, ResumenTienda> resumen = FacturacionElectronicaDAO.resumen(desde, hasta);
		if (resumen == null) {
			respuesta.put("error", "No se pudo consultar el datamart. Si la tabla factura_electronica_generada no existe, "
					+ "falta correr la migracion 2026_10_06_02_datamart_facturacion.sql.");
			return respuesta.toJSONString();
		}
		final ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		final JSONArray filas = new JSONArray();
		final Cifras totalFact = new Cifras();
		final Cifras totalNotas = new Cifras();
		int sinCobertura = 0;
		for (final Tienda t : tiendas) {
			final ResumenTienda r = resumen.containsKey(Integer.valueOf(t.getIdTienda()))
					? resumen.get(Integer.valueOf(t.getIdTienda()))
					: new ResumenTienda();
			final JSONObject f = new JSONObject();
			f.put("idtienda", t.getIdTienda());
			f.put("nombre", t.getNombreTienda());
			f.put("facturas", cifras(r.facturas));
			f.put("notas", cifras(r.notas));
			f.put("neto_base", r.facturas.base - r.notas.base);
			f.put("neto_impuesto", r.facturas.impuesto - r.notas.impuesto);
			f.put("neto_total", r.facturas.total - r.notas.total);
			f.put("facturas_no_aceptadas", r.facturasNoAceptadas);
			f.put("notas_no_aceptadas", r.notasNoAceptadas);
			f.put("dias_con_datos", r.diasConDatos);
			filas.add(f);
			if (r.diasConDatos < diasDelMes) {
				sinCobertura++;
			}
			sumar(totalFact, r.facturas);
			sumar(totalNotas, r.notas);
		}
		respuesta.put("mes", mes);
		respuesta.put("dias_del_mes", diasDelMes);
		respuesta.put("tiendas", filas);
		respuesta.put("tiendas_sin_cobertura_completa", sinCobertura);
		final JSONObject totales = new JSONObject();
		totales.put("facturas", cifras(totalFact));
		totales.put("notas", cifras(totalNotas));
		totales.put("neto_base", totalFact.base - totalNotas.base);
		totales.put("neto_impuesto", totalFact.impuesto - totalNotas.impuesto);
		totales.put("neto_total", totalFact.total - totalNotas.total);
		respuesta.put("totales", totales);

		// Las notas credito una por una, con la fecha de la factura que anulan: las de facturas de meses
		// anteriores son las que mas explican una diferencia contra el contador.
		final JSONArray notas = new JSONArray();
		for (final NotaDetalle n : FacturacionElectronicaDAO.notasDelMes(desde, hasta)) {
			final JSONObject o = new JSONObject();
			o.put("tienda", nombreTienda(tiendas, n.idTienda));
			o.put("fecha", n.fecha);
			o.put("hora", n.hora);
			o.put("nota", n.documentoNota);
			o.put("factura", n.documentoFactura);
			o.put("fecha_factura", n.fechaFactura);
			o.put("pedido", n.idPedidoTienda);
			o.put("motivo", n.motivo);
			o.put("total", n.total);
			o.put("factura_de_otro_mes", n.fechaFactura.length() > 0 && !n.fechaFactura.startsWith(mes));
			notas.add(o);
		}
		respuesta.put("notas_detalle", notas);
		return respuesta.toJSONString();
	}

	/** El CSV del mes con todos los documentos, separado por punto y coma para que Excel en espanol lo abra bien. */
	public static String csv(final String mes) {
		if (!mesValido(mes)) {
			return "Mes invalido";
		}
		final ArrayList<Tienda> tiendas = TiendaDAO.obtenerTiendas();
		final StringBuilder sb = new StringBuilder();
		boolean primera = true;
		for (final String[] fila : FacturacionElectronicaDAO.documentosDelMes(mes + "-01", mes + "-31")) {
			for (int i = 0; i < fila.length; i++) {
				String celda = fila[i] == null ? "" : fila[i];
				// La columna idtienda sale con el nombre de la tienda.
				if (!primera && i == 1) {
					try {
						celda = nombreTienda(tiendas, Integer.parseInt(celda));
					} catch (final Exception e) {
						// Se deja el numero.
					}
				}
				if (i > 0) {
					sb.append(';');
				}
				sb.append('"').append(celda.replace("\"", "\"\"")).append('"');
			}
			sb.append("\r\n");
			primera = false;
		}
		return sb.toString();
	}

	private static JSONObject cifras(final Cifras c) {
		final JSONObject o = new JSONObject();
		o.put("cantidad", c.cantidad);
		o.put("base", c.base);
		o.put("impuesto", c.impuesto);
		o.put("total", c.total);
		o.put("sin_valor", c.sinValor);
		return o;
	}

	private static void sumar(final Cifras destino, final Cifras c) {
		destino.cantidad += c.cantidad;
		destino.base += c.base;
		destino.impuesto += c.impuesto;
		destino.total += c.total;
		destino.sinValor += c.sinValor;
	}

	private static String nombreTienda(final ArrayList<Tienda> tiendas, final int idTienda) {
		for (final Tienda t : tiendas) {
			if (t.getIdTienda() == idTienda) {
				return t.getNombreTienda();
			}
		}
		return String.valueOf(idTienda);
	}

	private static boolean mesValido(final String mes) {
		return mes != null && mes.matches("^[0-9]{4}-(0[1-9]|1[0-2])$");
	}

	private static int diasDelMes(final String mes) {
		return java.time.YearMonth.of(Integer.parseInt(mes.substring(0, 4)), Integer.parseInt(mes.substring(5, 7)))
				.lengthOfMonth();
	}
}
