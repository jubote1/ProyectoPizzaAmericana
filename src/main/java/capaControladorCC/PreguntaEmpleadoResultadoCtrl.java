package capaControladorCC;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.PreguntaEmpleadoResultadoDAO;

/**
 * Arma el JSON de la pantalla de resultados de las preguntas a empleados:
 * primero el resumen de todos los empleados activos en un rango, y luego, al
 * elegir uno, su detalle por pregunta y respuesta por respuesta.
 *
 * El porcentaje de acierto se calcula aqui y va ya redondeado, para que la
 * pantalla y cualquier otro consumidor digan lo mismo. Sin respuestas no hay
 * porcentaje (null), que es distinto de 0%: el 0% es haber contestado y fallado
 * todo, y confundirlos le pondria en rojo a quien simplemente no ha ingresado.
 */
public class PreguntaEmpleadoResultadoCtrl {

	@SuppressWarnings("unchecked")
	public static String consultarResumen(final String desde, final String hasta) {
		final JSONObject respuesta = new JSONObject();
		respuesta.put("desde", desde);
		respuesta.put("hasta", hasta);
		if (desde == null || desde.length() == 0 || hasta == null || hasta.length() == 0) {
			respuesta.put("error", "Falta el rango de fechas.");
			return (respuesta.toJSONString());
		}
		try {
			final JSONArray lista = new JSONArray();
			for (final PreguntaEmpleadoResultadoDAO.ResumenEmpleado e : PreguntaEmpleadoResultadoDAO
					.obtenerResumen(desde, hasta)) {
				final JSONObject o = new JSONObject();
				o.put("id", e.id);
				o.put("nombre", e.nombre);
				o.put("nombrelargo", e.nombreLargo);
				o.put("cargo", e.cargo);
				o.put("respondidas", e.respondidas);
				o.put("correctas", e.correctas);
				o.put("porcentaje", porcentaje(e.correctas, e.respondidas));
				o.put("ultima", e.ultima);
				lista.add(o);
			}
			respuesta.put("empleados", lista);
		} catch (final Exception e) {
			respuesta.put("error", mensajeDeError(e));
		}
		return (respuesta.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String consultarDetalle(final int idEmpleado, final String desde, final String hasta) {
		final JSONObject respuesta = new JSONObject();
		respuesta.put("idempleado", idEmpleado);
		respuesta.put("desde", desde);
		respuesta.put("hasta", hasta);
		if (idEmpleado <= 0 || desde == null || desde.length() == 0 || hasta == null || hasta.length() == 0) {
			respuesta.put("error", "Falta el empleado o el rango de fechas.");
			return (respuesta.toJSONString());
		}
		try {
			final JSONArray porPregunta = new JSONArray();
			for (final PreguntaEmpleadoResultadoDAO.PorPregunta p : PreguntaEmpleadoResultadoDAO
					.obtenerPorPregunta(idEmpleado, desde, hasta)) {
				final JSONObject o = new JSONObject();
				o.put("idpregunta", p.idPregunta);
				o.put("pregunta", p.descripcion);
				o.put("respuestacorrecta", p.respuestaCorrecta);
				o.put("veces", p.veces);
				o.put("correctas", p.correctas);
				o.put("porcentaje", porcentaje(p.correctas, p.veces));
				porPregunta.add(o);
			}
			respuesta.put("porpregunta", porPregunta);

			final JSONArray respuestas = new JSONArray();
			for (final PreguntaEmpleadoResultadoDAO.Respuesta r : PreguntaEmpleadoResultadoDAO
					.obtenerRespuestas(idEmpleado, desde, hasta)) {
				final JSONObject o = new JSONObject();
				o.put("fecha", r.fecha);
				o.put("pregunta", r.pregunta);
				o.put("respuesta", r.respuesta);
				o.put("respuestacorrecta", r.respuestaCorrecta);
				o.put("correcta", r.correcta);
				o.put("idtienda", r.idTienda);
				respuestas.add(o);
			}
			respuesta.put("respuestas", respuestas);
		} catch (final Exception e) {
			respuesta.put("error", mensajeDeError(e));
		}
		return (respuesta.toJSONString());
	}

	/** Entero de 0 a 100, o null si no hay nada que medir. */
	private static Object porcentaje(final int correctas, final int total) {
		if (total <= 0) {
			return (null);
		}
		return (Long.valueOf(Math.round(correctas * 100.0 / total)));
	}

	/**
	 * El mensaje que ve el usuario. Una columna que no existe casi siempre quiere
	 * decir que falta el script sql/preguntas_empleado_biometria.sql, y eso se
	 * dice tal cual.
	 */
	private static String mensajeDeError(final Exception e) {
		final String m = e.getMessage() == null ? e.toString() : e.getMessage();
		if (m.contains("Unknown column") || m.contains("doesn't exist")) {
			return ("La base general no tiene todavia las columnas de las respuestas. "
					+ "Falta correr sql/preguntas_empleado_biometria.sql. (" + m + ")");
		}
		return ("No se pudo consultar: " + m);
	}
}
