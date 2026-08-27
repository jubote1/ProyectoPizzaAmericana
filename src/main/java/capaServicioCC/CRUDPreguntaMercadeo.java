package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaControladorCC.EncuestaMercadeoCtrl;
import capaModeloCC.OpcionMercadeo;
import capaModeloCC.PreguntaMercadeo;

/**
 * Servicio de administracion de las preguntas de encuesta de mercadeo. Lo consume el
 * maestro de configuracion del POS.
 *
 * Sigue la nomenclatura CRUD que ya existe en capaServicioCC (CRUDOferta, CRUDTienda).
 *
 * Acciones, en el parametro accion:
 *   LISTAR           GET.  Retorna todas las preguntas, vigentes o no.
 *   CREAR            POST. Cuerpo JSON con la pregunta y sus opciones.
 *   ACTUALIZAR       POST. Cuerpo JSON con idpregunta y los campos a cambiar.
 *   INACTIVAR        GET o POST con idpregunta.
 *   INACTIVAROPCION  GET o POST con idopcion.
 *
 * Las preguntas y opciones no se borran fisicamente: se inactivan, porque las
 * respuestas historicas de encuesta_mercadeo guardan idpregunta e idopcion.
 *
 * Servlet implementation class CRUDPreguntaMercadeo
 */
@WebServlet("/CRUDPreguntaMercadeo")
public class CRUDPreguntaMercadeo extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(CRUDPreguntaMercadeo.class.getName());
	private EncuestaMercadeoCtrl encuestaCtrl;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CRUDPreguntaMercadeo() {
		super();
	}

	@Override
	public void init() throws ServletException {
		this.encuestaCtrl = new EncuestaMercadeoCtrl();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		String accion = request.getParameter("accion");
		if (accion == null) {
			accion = "LISTAR";
		}
		accion = accion.trim().toUpperCase();
		try {
			if (accion.equals("LISTAR")) {
				out.write(this.encuestaCtrl.obtenerTodasLasPreguntas());
			} else if (accion.equals("INACTIVAR")) {
				final int idPregunta = entero(request.getParameter("idpregunta"));
				if (idPregunta <= 0) {
					out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"idpregunta es obligatorio.\"}");
				} else {
					out.write(this.encuestaCtrl.inactivarPregunta(idPregunta));
				}
			} else if (accion.equals("INACTIVAROPCION")) {
				final int idOpcion = entero(request.getParameter("idopcion"));
				if (idOpcion <= 0) {
					out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"idopcion es obligatorio.\"}");
				} else {
					out.write(this.encuestaCtrl.inactivarOpcion(idOpcion));
				}
			} else if (accion.equals("CREAR") || accion.equals("ACTUALIZAR")) {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"CREAR y ACTUALIZAR requieren POST con cuerpo JSON.\"}");
			} else {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Accion no reconocida.\"}");
			}
		} catch (final Exception e) {
			logger.severe("CRUDPreguntaMercadeo doGet: " + e.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Error procesando la solicitud.\"}");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		String accion = request.getParameter("accion");
		if (accion == null) {
			accion = "";
		}
		accion = accion.trim().toUpperCase();
		// Las acciones sin cuerpo JSON se atienden igual que por GET
		if (!accion.equals("CREAR") && !accion.equals("ACTUALIZAR")) {
			doGet(request, response);
			return;
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final String json = leerCuerpo(request);
			if (json.trim().length() == 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Cuerpo vacio.\"}");
				return;
			}
			final JSONParser parser = new JSONParser();
			final JSONObject cuerpo = (JSONObject) parser.parse(json);
			final PreguntaMercadeo pregunta = preguntaDeJson(cuerpo);
			if (pregunta.getDescripcion() == null || pregunta.getDescripcion().trim().length() == 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"La descripcion de la pregunta es obligatoria.\"}");
				return;
			}
			if (accion.equals("CREAR")) {
				out.write(this.encuestaCtrl.crearPregunta(pregunta));
			} else {
				if (pregunta.getIdPregunta() <= 0) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"idpregunta es obligatorio para ACTUALIZAR.\"}");
					return;
				}
				out.write(this.encuestaCtrl.actualizarPregunta(pregunta));
			}
		} catch (final Exception e) {
			logger.severe("CRUDPreguntaMercadeo doPost: " + e.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Error procesando la solicitud.\"}");
		}
	}

	/**
	 * Lee el cuerpo completo de la peticion como texto.
	 *
	 * @param request peticion
	 * @return el cuerpo como String, nunca null
	 */
	private String leerCuerpo(final HttpServletRequest request) throws IOException {
		final StringBuilder constructor = new StringBuilder();
		String linea;
		final BufferedReader lector = request.getReader();
		try {
			while ((linea = lector.readLine()) != null) {
				constructor.append(linea);
			}
		} finally {
			lector.close();
		}
		return (constructor.toString());
	}

	/**
	 * Construye una pregunta a partir del JSON recibido.
	 *
	 * @param cuerpo objeto JSON de la pregunta
	 * @return la pregunta con sus opciones
	 */
	private PreguntaMercadeo preguntaDeJson(final JSONObject cuerpo) {
		final PreguntaMercadeo pregunta = new PreguntaMercadeo();
		pregunta.setIdPregunta(entero(cuerpo.get("idpregunta")));
		pregunta.setTitulo(texto(cuerpo.get("titulo")));
		pregunta.setDescripcion(texto(cuerpo.get("descripcion")));
		pregunta.setTipo(indicador(cuerpo.get("tipo"), PreguntaMercadeo.TIPO_BOOLEANA));
		pregunta.setFechaInicio(texto(cuerpo.get("fechainicio")));
		pregunta.setFechaFin(texto(cuerpo.get("fechafin")));
		pregunta.setOrden(entero(cuerpo.get("orden")));
		pregunta.setObligatoria(indicador(cuerpo.get("obligatoria"), "N"));
		pregunta.setAplicaPV(indicador(cuerpo.get("aplicapv"), "S"));
		pregunta.setAplicaDomicilio(indicador(cuerpo.get("aplicadomicilio"), "N"));
		pregunta.setActivo(indicador(cuerpo.get("activo"), "S"));
		pregunta.setUsuarioCreacion(texto(cuerpo.get("usuariocreacion")));
		final Object crudoOpciones = cuerpo.get("opciones");
		final ArrayList<OpcionMercadeo> opciones = new ArrayList<OpcionMercadeo>();
		if (crudoOpciones instanceof JSONArray) {
			final JSONArray arreglo = (JSONArray) crudoOpciones;
			for (int i = 0; i < arreglo.size(); i++) {
				final JSONObject item = (JSONObject) arreglo.get(i);
				final OpcionMercadeo opcion = new OpcionMercadeo();
				opcion.setIdOpcion(entero(item.get("idopcion")));
				opcion.setDescripcion(texto(item.get("descripcion")));
				opcion.setOrden(entero(item.get("orden")));
				opcion.setActivo(indicador(item.get("activo"), "S"));
				if (opcion.getDescripcion().trim().length() > 0) {
					opciones.add(opcion);
				}
			}
		}
		pregunta.setOpciones(opciones);
		return (pregunta);
	}

	/**
	 * Convierte a entero un valor de JSON, tolerando null, Long y String.
	 *
	 * @param valor valor crudo
	 * @return el entero, o 0 si no se puede convertir
	 */
	private int entero(final Object valor) {
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

	/**
	 * Convierte a String un valor de JSON, retornando cadena vacia si viene null.
	 *
	 * @param valor valor crudo
	 * @return el texto, nunca null
	 */
	private String texto(final Object valor) {
		return (valor == null ? "" : valor.toString());
	}

	/**
	 * Normaliza un indicador de un caracter, aplicando un valor por defecto.
	 *
	 * @param valor       valor crudo
	 * @param porDefecto  valor a usar si viene vacio
	 * @return el indicador en mayuscula
	 */
	private String indicador(final Object valor, final String porDefecto) {
		final String texto = texto(valor).trim();
		return (texto.length() == 0 ? porDefecto : texto.substring(0, 1).toUpperCase());
	}
}
