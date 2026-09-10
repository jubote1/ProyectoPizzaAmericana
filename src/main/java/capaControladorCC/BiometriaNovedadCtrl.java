package capaControladorCC;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.BiometriaNovedadDAO;

/**
 * Novedades de biometria: arma el JSON que consumen la pantalla del central y
 * el POS.
 *
 * Todas las respuestas tienen la misma forma -resultado, mensaje y lo que
 * corresponda- para que la pantalla no tenga que distinguir estructuras segun
 * si salio bien o mal.
 */
public class BiometriaNovedadCtrl {

	/** Tope de dias del rango, para no traerle al navegador meses de novedades. */
	private static final int MAXIMO_DIAS_RANGO = 92;

	public int maximoDiasRango() {
		return (BiometriaNovedadCtrl.MAXIMO_DIAS_RANGO);
	}

	/**
	 * Novedades de un rango de fechas, con el conteo por estado.
	 *
	 * @param fechaDesde aaaa-mm-dd inclusive
	 * @param fechaHasta aaaa-mm-dd inclusive
	 * @param estado     REPORTADA, ATENDIDA, RECHAZADA, o vacio para todas
	 */
	public String consultarNovedades(final String fechaDesde, final String fechaHasta,
			final String estado) {
		final JSONObject respuesta = new JSONObject();
		final JSONArray detalle = new JSONArray();
		int reportadas = 0;
		int atendidas = 0;
		int rechazadas = 0;
		final List<JSONObject> novedades = BiometriaNovedadDAO.obtenerNovedades(fechaDesde, fechaHasta,
				estado);
		for (int i = 0; i < novedades.size(); i++) {
			final JSONObject novedad = novedades.get(i);
			final String estadoNovedad = String.valueOf(novedad.get("estado"));
			if ("REPORTADA".equals(estadoNovedad)) {
				reportadas++;
			} else if ("ATENDIDA".equals(estadoNovedad)) {
				atendidas++;
			} else if ("RECHAZADA".equals(estadoNovedad)) {
				rechazadas++;
			}
			detalle.add(novedad);
		}
		final JSONObject resumen = new JSONObject();
		resumen.put("total", novedades.size());
		resumen.put("reportadas", reportadas);
		resumen.put("atendidas", atendidas);
		resumen.put("rechazadas", rechazadas);
		respuesta.put("resultado", "OK");
		respuesta.put("mensaje", "");
		respuesta.put("resumen", resumen);
		respuesta.put("detalle", detalle);
		return (respuesta.toJSONString());
	}

	/**
	 * La jornada completa de un empleado en un dia: todos sus registros de
	 * biometria, ordenados por hora. Es lo que el supervisor necesita ver para
	 * decidir, no solo el registro que trae la novedad.
	 */
	public String consultarEventosDelDia(final int idEmpleado, final String fecha) {
		final JSONObject respuesta = new JSONObject();
		final JSONArray detalle = new JSONArray();
		final List<JSONObject> eventos = BiometriaNovedadDAO.obtenerEventosDelDia(idEmpleado, fecha);
		for (int i = 0; i < eventos.size(); i++) {
			detalle.add(eventos.get(i));
		}
		respuesta.put("resultado", "OK");
		respuesta.put("mensaje", "");
		respuesta.put("detalle", detalle);
		return (respuesta.toJSONString());
	}

	/**
	 * Aplica un cambio a la jornada y lo deja en el log. Un cambio por llamada:
	 * asi cada uno es atomico y queda registrado por separado, que es lo que
	 * permite reconstruir despues quien toco que.
	 */
	public String guardarCambio(final int idNovedad, final int idEmpleado, final String fecha,
			final int idTienda, final String accion, final String antesTipo,
			final String antesFechaHora, final String despuesTipo, final String despuesFechaHora,
			final String usuario, final String observacion) {
		final JSONObject respuesta = new JSONObject();
		final String falla = BiometriaNovedadDAO.aplicarCambio(idNovedad, idEmpleado, fecha, idTienda,
				accion, antesTipo, antesFechaHora, despuesTipo, despuesFechaHora, usuario, observacion);
		if (falla.length() > 0) {
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", falla);
		} else {
			respuesta.put("resultado", "OK");
			respuesta.put("mensaje", "Cambio aplicado y registrado en el log.");
		}
		return (respuesta.toJSONString());
	}

	/** Marca la novedad como ATENDIDA o RECHAZADA. */
	public String cerrarNovedad(final int idNovedad, final String estado, final String usuario,
			final String observacion) {
		final JSONObject respuesta = new JSONObject();
		final String falla = BiometriaNovedadDAO.cerrarNovedad(idNovedad, estado, usuario, observacion);
		if (falla.length() > 0) {
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", falla);
		} else {
			respuesta.put("resultado", "OK");
			respuesta.put("mensaje", "Novedad marcada como " + estado + ".");
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Registra una novedad. La llama el POS.
	 *
	 * No cambia nada en la jornada: queda en estado REPORTADA esperando que el
	 * supervisor la revise. Por eso la pantalla del POS puede ser abierta.
	 */
	public String insertarNovedad(final int idEmpleado, final String fecha, final int idTienda,
			final String eventoTipo, final String eventoFechaHora, final String tipoNovedad,
			final String horaReportada, final String observacion, final String reportadoPor,
			final String origen) {
		final JSONObject respuesta = new JSONObject();
		final int idNovedad = BiometriaNovedadDAO.insertarNovedad(idEmpleado, fecha, idTienda,
				eventoTipo, eventoFechaHora, tipoNovedad, horaReportada, observacion, reportadoPor,
				origen);
		if (idNovedad <= 0) {
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", "No se pudo registrar la novedad.");
			respuesta.put("idnovedad", 0);
		} else {
			respuesta.put("resultado", "OK");
			respuesta.put("mensaje", "Novedad registrada. Queda pendiente de revision.");
			respuesta.put("idnovedad", idNovedad);
		}
		return (respuesta.toJSONString());
	}

	/** Respuesta de error con la misma forma que las exitosas. */
	public String error(final String mensaje) {
		final JSONObject respuesta = new JSONObject();
		respuesta.put("resultado", "ERROR");
		respuesta.put("mensaje", mensaje);
		respuesta.put("detalle", new JSONArray());
		return (respuesta.toJSONString());
	}
}
