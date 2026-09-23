package capaControladorCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.DashboardCargoDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.AsignacionCargoCancelada;
import capaModeloCC.PedidoCargo;

/**
 * Arma la respuesta del Dashboard Cargo: los pedidos que Cargo efectivamente
 * llevo en el rango, un resumen de cumplimiento y tiempos, un ranking por
 * tienda, y las asignaciones a Cargo que se cancelaron antes de quedar en
 * firme.
 *
 * "Cumplido" no es solo "se entrego": es que se entrego dentro de la meta de
 * minutos del negocio (parametro general.parametros MINUTOSCUMPLIMIENTOCARGO,
 * 60 por defecto si el parametro no existe todavia). Un pedido entregado pero
 * fuera de esa meta cuenta aparte, no como cumplido.
 */
public class DashboardCargoCtrl {

	private static final int MINUTOS_CUMPLIMIENTO_DEFECTO = 60;

	@SuppressWarnings("unchecked")
	public static String consultarDashboard(final int idTienda, final String fechaDesde, final String fechaHasta) {
		final ArrayList<PedidoCargo> pedidos = DashboardCargoDAO.consultarPedidosCargo(idTienda, fechaDesde,
				fechaHasta);
		final ArrayList<AsignacionCargoCancelada> canceladas = DashboardCargoDAO
				.consultarAsignacionesCanceladas(idTienda, fechaDesde, fechaHasta);

		final int umbralMinutos = obtenerUmbralMinutos();

		final JSONArray filasPedidos = new JSONArray();
		int cumplidos = 0;
		int fueraDeTiempo = 0;
		int cancelados = 0;
		int sinCierre = 0;
		double sumaMinutosTotal = 0;
		int cuentaMinutosTotal = 0;

		final Map<Integer, ResumenTienda> porTienda = new HashMap<Integer, ResumenTienda>();

		for (int i = 0; i < pedidos.size(); i++) {
			final PedidoCargo p = pedidos.get(i);
			final String estado;
			if (p.isCancelado()) {
				estado = "CANCELADO";
				cancelados++;
			} else if (p.isEntregado()) {
				final boolean dentroDeTiempo = p.getMinutosTotal() != null
						&& p.getMinutosTotal().doubleValue() <= umbralMinutos;
				if (dentroDeTiempo) {
					estado = "CUMPLIDO";
					cumplidos++;
				} else {
					estado = "FUERA DE TIEMPO";
					fueraDeTiempo++;
				}
			} else {
				estado = "SIN CIERRE";
				sinCierre++;
			}
			if (p.getMinutosTotal() != null) {
				sumaMinutosTotal += p.getMinutosTotal().doubleValue();
				cuentaMinutosTotal++;
			}

			final JSONObject f = new JSONObject();
			f.put("idpedido", p.getIdPedido());
			f.put("idtienda", p.getIdTienda());
			f.put("nombretienda", p.getNombreTienda());
			f.put("numposheader", p.getNumPosHeader());
			f.put("fechapedido", p.getFechaPedido());
			f.put("fechainsercion", p.getFechaInsercion());
			f.put("fechaentregado", p.getFechaEntregado());
			f.put("fechacancelacion", p.getFechaCancelacion());
			f.put("estado", estado);
			f.put("minutostotal", p.getMinutosTotal());
			filasPedidos.add(f);

			ResumenTienda rt = porTienda.get(Integer.valueOf(p.getIdTienda()));
			if (rt == null) {
				rt = new ResumenTienda();
				rt.idTienda = p.getIdTienda();
				rt.nombreTienda = p.getNombreTienda();
				porTienda.put(Integer.valueOf(p.getIdTienda()), rt);
			}
			rt.total++;
			if ("CUMPLIDO".equals(estado)) {
				rt.cumplidos++;
			} else if ("FUERA DE TIEMPO".equals(estado)) {
				rt.fueraDeTiempo++;
			} else if ("CANCELADO".equals(estado)) {
				rt.cancelados++;
			} else {
				rt.sinCierre++;
			}
			if (p.getMinutosTotal() != null) {
				rt.sumaMinutos += p.getMinutosTotal().doubleValue();
				rt.cuentaMinutos++;
			}
		}

		final JSONArray filasCanceladas = new JSONArray();
		for (int i = 0; i < canceladas.size(); i++) {
			final AsignacionCargoCancelada a = canceladas.get(i);
			final JSONObject f = new JSONObject();
			f.put("idpedido", a.getIdPedido());
			f.put("idtienda", a.getIdTienda());
			f.put("nombretienda", a.getNombreTienda());
			f.put("numposheader", a.getNumPosHeader());
			f.put("fechapedido", a.getFechaPedido());
			f.put("proveedor", a.getProveedor());
			f.put("ultimoestadocargo", a.getUltimoEstadoCargo());
			f.put("fechaasignacion", a.getFechaAsignacion());
			f.put("fechacancelacioncargo", a.getFechaCancelacionCargo());
			filasCanceladas.add(f);
		}

		final JSONObject resumen = new JSONObject();
		resumen.put("total", pedidos.size());
		resumen.put("cumplidos", cumplidos);
		resumen.put("fueradetiempo", fueraDeTiempo);
		resumen.put("cancelados", cancelados);
		resumen.put("sincierre", sinCierre);
		resumen.put("porcentajecumplimiento", pedidos.isEmpty() ? 0.0 : (cumplidos * 100.0) / pedidos.size());
		resumen.put("minutospromediototal", cuentaMinutosTotal == 0 ? null : sumaMinutosTotal / cuentaMinutosTotal);
		resumen.put("totalasignacionescanceladas", canceladas.size());
		resumen.put("umbralminutos", umbralMinutos);

		final JSONArray filasPorTienda = new JSONArray();
		for (final ResumenTienda rt : porTienda.values()) {
			final JSONObject f = new JSONObject();
			f.put("idtienda", rt.idTienda);
			f.put("nombretienda", rt.nombreTienda);
			f.put("total", rt.total);
			f.put("cumplidos", rt.cumplidos);
			f.put("fueradetiempo", rt.fueraDeTiempo);
			f.put("cancelados", rt.cancelados);
			f.put("sincierre", rt.sinCierre);
			f.put("porcentajecumplimiento", rt.total == 0 ? 0.0 : (rt.cumplidos * 100.0) / rt.total);
			f.put("minutospromediototal", rt.cuentaMinutos == 0 ? null : rt.sumaMinutos / rt.cuentaMinutos);
			filasPorTienda.add(f);
		}
		// Ranking por cumplimiento, de mejor a peor: es lo que permite medir el
		// manejo de Cargo tienda por tienda, no solo el volumen.
		filasPorTienda.sort((a, b) -> {
			final Double pa = (Double) ((JSONObject) a).get("porcentajecumplimiento");
			final Double pb = (Double) ((JSONObject) b).get("porcentajecumplimiento");
			return Double.compare(pb == null ? 0.0 : pb.doubleValue(), pa == null ? 0.0 : pa.doubleValue());
		});

		final JSONObject respuesta = new JSONObject();
		respuesta.put("pedidos", filasPedidos);
		respuesta.put("asignacionescanceladas", filasCanceladas);
		respuesta.put("resumen", resumen);
		respuesta.put("portienda", filasPorTienda);
		return (respuesta.toJSONString());
	}

	/** MINUTOSCUMPLIMIENTOCARGO en general.parametros; 60 si todavia no se ha corrido la migracion. */
	private static int obtenerUmbralMinutos() {
		final int valor = ParametrosDAO.retornarValorNumerico("MINUTOSCUMPLIMIENTOCARGO");
		return valor > 0 ? valor : MINUTOS_CUMPLIMIENTO_DEFECTO;
	}

	private static class ResumenTienda {
		int idTienda;
		String nombreTienda;
		int total;
		int cumplidos;
		int fueraDeTiempo;
		int cancelados;
		int sinCierre;
		double sumaMinutos;
		int cuentaMinutos;
	}

}
