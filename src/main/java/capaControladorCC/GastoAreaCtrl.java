package capaControladorCC;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.GastoAreaDAO;

/**
 * Pantalla de gastos por area: publicidad, administrativa, logistica y contact center.
 *
 * Las cuatro areas se reparten entre las tiendas con el mismo calculo -costo del area
 * por la participacion de la tienda en la venta-, asi que una sola pantalla las maneja.
 * Se empieza por publicidad.
 *
 * El mes importa en TODA consulta: un gasto fijo solo cuenta si estaba vigente ese mes,
 * y un gasto variable vale lo que se haya digitado para ese mes.
 */
public class GastoAreaCtrl {

	/** Las areas que hoy se reparten. Si entra una nueva, va aqui y en la pantalla. */
	public static final String[] AREAS = {"PUBLICIDAD", "ADMINISTRATIVA", "LOGISTICA", "CONTACT"};

	public String listarAreas() {
		final JSONArray lista = new JSONArray();
		for (final String area : AREAS) {
			final JSONObject j = new JSONObject();
			j.put("area", area);
			lista.add(j);
		}
		return lista.toJSONString();
	}

	/** Los gastos del area con el valor que les corresponde al mes pedido, y el total. */
	public String listar(final String area, final int anio, final int mes) {
		final JSONArray lista = new JSONArray();
		final ArrayList<GastoAreaDAO.Gasto> gastos = GastoAreaDAO.listar(area, anio, mes);
		for (final GastoAreaDAO.Gasto g : gastos) {
			final JSONObject j = new JSONObject();
			j.put("idgasto", g.idGasto);
			j.put("area", g.area);
			j.put("concepto", g.concepto);
			j.put("tipo", g.tipo);
			j.put("valormes", g.valorMes);
			j.put("vigenciadesde", g.vigenciaDesde);
			j.put("vigenciahasta", g.vigenciaHasta);
			j.put("activo", g.activo ? "S" : "N");
			// Para los variables: distingue "ese mes valio 0" de "ese mes no se ha digitado".
			j.put("tienevalordelmes", g.tieneValorDelMes ? "S" : "N");
			j.put("vigente", estaVigente(g, anio, mes) ? "S" : "N");
			lista.add(j);
		}
		return lista.toJSONString();
	}

	/**
	 * El resumen del mes: cuanto cuesta el area, cuanto es por semana y cuanto le toca a
	 * cada tienda.
	 *
	 * El reparto por tienda que se muestra aqui es el PAREJO, entre las 11, y es solo
	 * una referencia para cuadrar contra el Excel de hoy. El reparto de verdad va por
	 * participacion en la venta de cada tienda, y ese calculo vive en la pantalla de
	 * porcentaje, que es la que conoce las ventas del periodo.
	 */
	public String resumen(final String area, final int anio, final int mes, final int semanas, final int tiendas) {
		final double total = GastoAreaDAO.totalDelMes(area, anio, mes);
		final int semanasUsadas = semanas > 0 ? semanas : 4;
		final int tiendasUsadas = tiendas > 0 ? tiendas : 11;
		final JSONObject j = new JSONObject();
		j.put("area", area);
		j.put("anio", anio);
		j.put("mes", mes);
		j.put("total", total);
		j.put("semanas", semanasUsadas);
		j.put("tiendas", tiendasUsadas);
		j.put("totalsemanal", total / semanasUsadas);
		j.put("totalsemanalportienda", total / semanasUsadas / tiendasUsadas);
		return j.toJSONString();
	}

	public String guardar(final GastoAreaDAO.Gasto g) {
		final JSONObject j = new JSONObject();
		final String problema = validar(g);
		if (problema != null) {
			j.put("respuesta", "N");
			j.put("mensaje", problema);
			return j.toJSONString();
		}
		boolean exito;
		if (g.idGasto > 0) {
			exito = GastoAreaDAO.actualizar(g);
		} else {
			final int idNuevo = GastoAreaDAO.insertar(g);
			g.idGasto = idNuevo;
			exito = idNuevo > 0;
		}
		j.put("respuesta", exito ? "S" : "N");
		j.put("idgasto", g.idGasto);
		j.put("mensaje", exito ? "" : "No se pudo guardar el gasto.");
		return j.toJSONString();
	}

	public String eliminar(final int idGasto) {
		final JSONObject j = new JSONObject();
		final boolean exito = GastoAreaDAO.eliminar(idGasto);
		j.put("respuesta", exito ? "S" : "N");
		j.put("mensaje", exito ? "" : "No se pudo eliminar el gasto.");
		return j.toJSONString();
	}

	public String guardarValorDelMes(final int idGasto, final int anio, final int mes, final double valor,
			final String usuario) {
		final JSONObject j = new JSONObject();
		if (anio < 2000 || mes < 1 || mes > 12) {
			j.put("respuesta", "N");
			j.put("mensaje", "El mes no es valido.");
			return j.toJSONString();
		}
		if (valor < 0) {
			j.put("respuesta", "N");
			j.put("mensaje", "El valor no puede ser negativo.");
			return j.toJSONString();
		}
		final boolean exito = GastoAreaDAO.guardarValorDelMes(idGasto, anio, mes, valor, usuario);
		j.put("respuesta", exito ? "S" : "N");
		j.put("mensaje", exito ? "" : "No se pudo guardar el valor del mes.");
		return j.toJSONString();
	}

	/** Devuelve el primer problema que encuentre, o null si el gasto esta bien. */
	private String validar(final GastoAreaDAO.Gasto g) {
		if (g.concepto == null || g.concepto.trim().length() == 0) {
			return "El concepto no puede quedar vacio.";
		}
		if (g.area == null || g.area.trim().length() == 0) {
			return "Falta el area.";
		}
		if (g.vigenciaDesde == null || g.vigenciaDesde.trim().length() == 0) {
			return "La vigencia tiene que decir desde cuando corre el gasto.";
		}
		if (g.vigenciaHasta != null && g.vigenciaHasta.trim().length() > 0
				&& g.vigenciaHasta.compareTo(g.vigenciaDesde) < 0) {
			return "La vigencia termina antes de empezar.";
		}
		if (!g.esVariable() && g.valorMes <= 0) {
			return "Un gasto fijo tiene que tener un valor mensual mayor que cero.";
		}
		return null;
	}

	/** Si el gasto corre en ese mes: activo y con la vigencia tocando el mes. */
	private boolean estaVigente(final GastoAreaDAO.Gasto g, final int anio, final int mes) {
		if (!g.activo) {
			return false;
		}
		final String primero = String.format("%04d-%02d-01", Integer.valueOf(anio), Integer.valueOf(mes));
		final String ultimo = String.format("%04d-%02d-31", Integer.valueOf(anio), Integer.valueOf(mes));
		if (g.vigenciaDesde.length() >= 10 && g.vigenciaDesde.substring(0, 10).compareTo(ultimo) > 0) {
			return false;
		}
		if (g.vigenciaHasta != null && g.vigenciaHasta.length() >= 10
				&& g.vigenciaHasta.substring(0, 10).compareTo(primero) < 0) {
			return false;
		}
		return true;
	}
}
