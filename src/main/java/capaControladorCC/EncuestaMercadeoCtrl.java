package capaControladorCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.EncuestaMercadeoDAO;
import capaDAOCC.PreguntaMercadeoDAO;
import capaModeloCC.EncuestaMercadeo;
import capaModeloCC.OpcionMercadeo;
import capaModeloCC.PreguntaMercadeo;

/**
 * Controlador de las encuestas de mercadeo que se aplican en el POS al finalizar
 * un pedido. Expone metodos que retornan JSON como String, tal como lo esperan los
 * servlets de capaServicioCC.
 */
public class EncuestaMercadeoCtrl {

	/** Longitud maxima de la columna respuesta en encuesta_mercadeo. */
	private static final int MAX_LARGO_RESPUESTA = 300;

	public EncuestaMercadeoCtrl() {
		super();
	}

	/**
	 * Retorna las preguntas vigentes hoy para el canal indicado.
	 *
	 * Cuando no hay preguntas vigentes retorna un arreglo vacio en preguntas. El POS
	 * usa esta misma llamada como prueba de conectividad: si falla o viene vacia, no
	 * muestra nada y el pedido sigue su curso.
	 *
	 * @param esPuntoVenta true para pedidos de punto de venta, false para domicilio
	 * @return JSON con la lista de preguntas y sus opciones
	 */
	@SuppressWarnings("unchecked")
	public String obtenerPreguntasVigentes(final boolean esPuntoVenta) {
		final JSONObject respuesta = new JSONObject();
		final JSONArray arregloPreguntas = new JSONArray();
		try {
			final ArrayList<PreguntaMercadeo> preguntas = PreguntaMercadeoDAO
					.obtenerPreguntasVigentes(esPuntoVenta);
			for (final PreguntaMercadeo pregunta : preguntas) {
				arregloPreguntas.add(preguntaAJson(pregunta));
			}
			respuesta.put("resultado", "OK");
			respuesta.put("preguntas", arregloPreguntas);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("obtenerPreguntasVigentes: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
			respuesta.put("preguntas", arregloPreguntas);
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Retorna todas las preguntas configuradas, vigentes o no, para el maestro de
	 * configuracion del POS.
	 *
	 * @return JSON con la lista completa de preguntas y sus opciones
	 */
	@SuppressWarnings("unchecked")
	public String obtenerTodasLasPreguntas() {
		final JSONObject respuesta = new JSONObject();
		final JSONArray arregloPreguntas = new JSONArray();
		try {
			final ArrayList<PreguntaMercadeo> preguntas = PreguntaMercadeoDAO.obtenerTodasLasPreguntas();
			for (final PreguntaMercadeo pregunta : preguntas) {
				arregloPreguntas.add(preguntaAJson(pregunta));
			}
			respuesta.put("resultado", "OK");
			respuesta.put("preguntas", arregloPreguntas);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("obtenerTodasLasPreguntas: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
			respuesta.put("preguntas", arregloPreguntas);
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Registra las respuestas de una encuesta tomada en el POS.
	 *
	 * @param idTienda       tienda donde se tomo la encuesta
	 * @param idPedidoTienda consecutivo del pedido en la tienda
	 * @param idCliente      cliente de la tienda
	 * @param idUsuario      usuario que opero el POS
	 * @param jsonRespuestas arreglo JSON con las respuestas
	 * @return JSON con resultado OK o ERROR
	 */
	@SuppressWarnings("unchecked")
	public String registrarRespuestas(final int idTienda, final int idPedidoTienda, final int idCliente,
			final int idUsuario, final String jsonRespuestas) {
		final JSONObject respuesta = new JSONObject();
		try {
			if (jsonRespuestas == null || jsonRespuestas.trim().length() == 0) {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se recibieron respuestas.");
				return (respuesta.toJSONString());
			}
			final JSONParser parser = new JSONParser();
			final JSONArray arreglo = (JSONArray) parser.parse(jsonRespuestas);
			final ArrayList<EncuestaMercadeo> respuestas = new ArrayList<EncuestaMercadeo>();
			for (int i = 0; i < arreglo.size(); i++) {
				final JSONObject item = (JSONObject) arreglo.get(i);
				final EncuestaMercadeo registro = new EncuestaMercadeo();
				registro.setIdPregunta(enteroDeJson(item.get("idpregunta")));
				registro.setIdOpcion(enteroDeJson(item.get("idopcion")));
				String texto = item.get("respuesta") == null ? "" : item.get("respuesta").toString();
				if (texto.length() > MAX_LARGO_RESPUESTA) {
					texto = texto.substring(0, MAX_LARGO_RESPUESTA);
				}
				registro.setRespuesta(texto);
				registro.setIdPedidoTienda(idPedidoTienda);
				registro.setIdTienda(idTienda);
				registro.setIdCliente(idCliente);
				registro.setIdUsuario(idUsuario);
				if (registro.getIdPregunta() > 0) {
					respuestas.add(registro);
				}
			}
			if (respuestas.isEmpty()) {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "Las respuestas recibidas no tienen idpregunta valido.");
				return (respuesta.toJSONString());
			}
			final int insertadas = EncuestaMercadeoDAO.insertarRespuestas(respuestas);
			if (insertadas > 0) {
				respuesta.put("resultado", "OK");
				respuesta.put("insertadas", Integer.valueOf(insertadas));
			} else {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se pudieron insertar las respuestas.");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("registrarRespuestas: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Indica si un pedido ya tiene encuesta registrada.
	 *
	 * @param idPedidoTienda consecutivo del pedido en la tienda
	 * @param idTienda       tienda del pedido
	 * @return true si ya existe encuesta para ese pedido
	 */
	public boolean existeEncuestaPedido(final int idPedidoTienda, final int idTienda) {
		return (EncuestaMercadeoDAO.existeEncuestaPedido(idPedidoTienda, idTienda));
	}

	/**
	 * Arma el reporte de respuestas de encuestas de mercadeo, en sus dos vistas.
	 *
	 * Retorna un solo JSON con:
	 *   resumen: cuantas veces se dio cada respuesta por pregunta, con su porcentaje
	 *            dentro de la pregunta.
	 *   detalle: una fila por respuesta, para revisar caso por caso y para leer las
	 *            respuestas de las preguntas abiertas.
	 *   totales: cantidad de respuestas y de encuestas distintas del periodo.
	 *
	 * @param idTienda     tienda a filtrar; 0 para todas
	 * @param fechaInicial fecha inicial inclusive, formato aaaa-mm-dd
	 * @param fechaFinal   fecha final inclusive, formato aaaa-mm-dd
	 * @param idPregunta   pregunta a filtrar; 0 para todas
	 * @return JSON del reporte
	 */
	@SuppressWarnings("unchecked")
	public String consultarReporte(final int idTienda, final String fechaInicial, final String fechaFinal,
			final int idPregunta) {
		final JSONObject respuesta = new JSONObject();
		final JSONArray arregloResumen = new JSONArray();
		final JSONArray arregloDetalle = new JSONArray();
		try {
			if (!fechaValida(fechaInicial) || !fechaValida(fechaFinal)) {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "Las fechas deben venir en formato aaaa-mm-dd.");
				respuesta.put("resumen", arregloResumen);
				respuesta.put("detalle", arregloDetalle);
				return (respuesta.toJSONString());
			}
			if (fechaFinal.compareTo(fechaInicial) < 0) {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "La fecha final no puede ser anterior a la inicial.");
				respuesta.put("resumen", arregloResumen);
				respuesta.put("detalle", arregloDetalle);
				return (respuesta.toJSONString());
			}

			final ArrayList<JSONObject> resumen = EncuestaMercadeoDAO.obtenerResumen(idTienda, fechaInicial,
					fechaFinal, idPregunta);
			// Total por pregunta, para poder calcular el porcentaje de cada respuesta
			// dentro de su propia pregunta y no sobre el total general
			final HashMap<Integer, Integer> totalPorPregunta = new HashMap<Integer, Integer>();
			for (final JSONObject fila : resumen) {
				final Integer idPreg = (Integer) fila.get("idpregunta");
				final Integer cantidad = (Integer) fila.get("cantidad");
				final Integer acumulado = totalPorPregunta.get(idPreg);
				totalPorPregunta.put(idPreg,
						Integer.valueOf((acumulado == null ? 0 : acumulado.intValue()) + cantidad.intValue()));
			}
			int totalRespuestas = 0;
			for (final JSONObject fila : resumen) {
				final Integer idPreg = (Integer) fila.get("idpregunta");
				final int cantidad = ((Integer) fila.get("cantidad")).intValue();
				final int totalPreg = totalPorPregunta.get(idPreg).intValue();
				final double porcentaje = totalPreg == 0 ? 0d
						: Math.round(cantidad * 1000d / totalPreg) / 10d;
				fila.put("totalpregunta", Integer.valueOf(totalPreg));
				fila.put("porcentaje", Double.valueOf(porcentaje));
				arregloResumen.add(fila);
				totalRespuestas += cantidad;
			}

			final ArrayList<JSONObject> detalle = EncuestaMercadeoDAO.obtenerDetalle(idTienda, fechaInicial,
					fechaFinal, idPregunta);
			// Una encuesta es el conjunto de respuestas de un mismo pedido en una tienda
			final HashSet<String> encuestas = new HashSet<String>();
			for (final JSONObject fila : detalle) {
				arregloDetalle.add(fila);
				encuestas.add(fila.get("idtienda") + "-" + fila.get("idpedidotienda"));
			}

			final JSONObject totales = new JSONObject();
			totales.put("respuestas", Integer.valueOf(totalRespuestas));
			totales.put("encuestas", Integer.valueOf(encuestas.size()));
			respuesta.put("resultado", "OK");
			respuesta.put("totales", totales);
			respuesta.put("resumen", arregloResumen);
			respuesta.put("detalle", arregloDetalle);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("consultarReporte: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
			respuesta.put("resumen", arregloResumen);
			respuesta.put("detalle", arregloDetalle);
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Valida que una fecha venga en formato aaaa-mm-dd y sea real.
	 *
	 * @param fecha texto a validar
	 * @return true si es valida
	 */
	private boolean fechaValida(final String fecha) {
		boolean valida = false;
		if (fecha != null && fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
			try {
				java.time.LocalDate.parse(fecha);
				valida = true;
			} catch (final Exception e) {
				valida = false;
			}
		}
		return (valida);
	}

	/**
	 * Inserta una pregunta con las opciones que traiga.
	 *
	 * @param pregunta pregunta a crear
	 * @return JSON con el idpregunta creado
	 */
	@SuppressWarnings("unchecked")
	public String crearPregunta(final PreguntaMercadeo pregunta) {
		final JSONObject respuesta = new JSONObject();
		try {
			final int idPregunta = PreguntaMercadeoDAO.insertarPregunta(pregunta);
			if (idPregunta > 0) {
				for (final OpcionMercadeo opcion : pregunta.getOpciones()) {
					opcion.setIdPregunta(idPregunta);
					PreguntaMercadeoDAO.insertarOpcion(opcion);
				}
				respuesta.put("resultado", "OK");
				respuesta.put("idpregunta", Integer.valueOf(idPregunta));
			} else {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se pudo crear la pregunta. Revise que el titulo no este repetido.");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("crearPregunta: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Actualiza una pregunta. Las opciones que vengan con idopcion mayor a cero se
	 * actualizan; las que vengan en cero se crean.
	 *
	 * @param pregunta pregunta a actualizar
	 * @return JSON con resultado OK o ERROR
	 */
	@SuppressWarnings("unchecked")
	public String actualizarPregunta(final PreguntaMercadeo pregunta) {
		final JSONObject respuesta = new JSONObject();
		try {
			final boolean actualizada = PreguntaMercadeoDAO.actualizarPregunta(pregunta);
			if (actualizada) {
				for (final OpcionMercadeo opcion : pregunta.getOpciones()) {
					if (opcion.getIdOpcion() > 0) {
						PreguntaMercadeoDAO.actualizarOpcion(opcion);
					} else {
						opcion.setIdPregunta(pregunta.getIdPregunta());
						PreguntaMercadeoDAO.insertarOpcion(opcion);
					}
				}
				respuesta.put("resultado", "OK");
			} else {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se pudo actualizar la pregunta.");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("actualizarPregunta: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Inactiva una pregunta.
	 *
	 * @param idPregunta pregunta a inactivar
	 * @return JSON con resultado OK o ERROR
	 */
	@SuppressWarnings("unchecked")
	public String inactivarPregunta(final int idPregunta) {
		final JSONObject respuesta = new JSONObject();
		try {
			if (PreguntaMercadeoDAO.inactivarPregunta(idPregunta)) {
				respuesta.put("resultado", "OK");
			} else {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se pudo inactivar la pregunta.");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("inactivarPregunta: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Inactiva una opcion de respuesta.
	 *
	 * @param idOpcion opcion a inactivar
	 * @return JSON con resultado OK o ERROR
	 */
	@SuppressWarnings("unchecked")
	public String inactivarOpcion(final int idOpcion) {
		final JSONObject respuesta = new JSONObject();
		try {
			if (PreguntaMercadeoDAO.inactivarOpcion(idOpcion)) {
				respuesta.put("resultado", "OK");
			} else {
				respuesta.put("resultado", "ERROR");
				respuesta.put("mensaje", "No se pudo inactivar la opcion.");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("inactivarOpcion: " + e.toString());
			respuesta.put("resultado", "ERROR");
			respuesta.put("mensaje", e.toString());
		}
		return (respuesta.toJSONString());
	}

	/**
	 * Convierte una pregunta y sus opciones a JSON.
	 *
	 * @param pregunta pregunta a convertir
	 * @return objeto JSON de la pregunta
	 */
	@SuppressWarnings("unchecked")
	private JSONObject preguntaAJson(final PreguntaMercadeo pregunta) {
		final JSONObject item = new JSONObject();
		item.put("idpregunta", Integer.valueOf(pregunta.getIdPregunta()));
		item.put("titulo", pregunta.getTitulo());
		item.put("descripcion", pregunta.getDescripcion());
		item.put("tipo", pregunta.getTipo());
		item.put("fechainicio", pregunta.getFechaInicio());
		item.put("fechafin", pregunta.getFechaFin());
		item.put("orden", Integer.valueOf(pregunta.getOrden()));
		item.put("obligatoria", pregunta.getObligatoria());
		item.put("aplicapv", pregunta.getAplicaPV());
		item.put("aplicadomicilio", pregunta.getAplicaDomicilio());
		item.put("activo", pregunta.getActivo());
		final JSONArray arregloOpciones = new JSONArray();
		for (final OpcionMercadeo opcion : pregunta.getOpciones()) {
			final JSONObject itemOpcion = new JSONObject();
			itemOpcion.put("idopcion", Integer.valueOf(opcion.getIdOpcion()));
			itemOpcion.put("descripcion", opcion.getDescripcion());
			itemOpcion.put("orden", Integer.valueOf(opcion.getOrden()));
			itemOpcion.put("activo", opcion.getActivo());
			arregloOpciones.add(itemOpcion);
		}
		item.put("opciones", arregloOpciones);
		return (item);
	}

	/**
	 * Convierte a entero un valor que viene de un JSON, tolerando null, Long y String.
	 *
	 * @param valor valor crudo del JSON
	 * @return el entero, o 0 si no se puede convertir
	 */
	private int enteroDeJson(final Object valor) {
		int resultado = 0;
		if (valor != null) {
			try {
				resultado = Integer.parseInt(valor.toString().trim());
			} catch (final Exception e) {
				resultado = 0;
			}
		}
		return (resultado);
	}
}
