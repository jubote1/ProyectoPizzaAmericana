package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
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

/**
 * Servicio que registra las respuestas de una encuesta de mercadeo tomada en el POS
 * al finalizar un pedido.
 *
 * Recibe el cuerpo como JSON, no como parametros, porque las respuestas abiertas
 * pueden ser largas y romperian el limite de longitud de una URL.
 *
 * Cuerpo esperado:
 * {
 *   "idtienda": 3,
 *   "idpedidotienda": 99213,
 *   "idcliente": 1852,
 *   "idusuario": 17,
 *   "respuestas": [
 *     { "idpregunta": 11, "idopcion": 0,  "respuesta": "S" },
 *     { "idpregunta": 12, "idopcion": 32, "respuesta": "Un amigo" },
 *     { "idpregunta": 13, "idopcion": 0,  "respuesta": "Mas sillas en el segundo piso" }
 *   ]
 * }
 *
 * OJO: idpedidotienda es el consecutivo del pedido en la TIENDA, no el idpedido de
 * pizzaamericana, que es una secuencia distinta.
 *
 * Servlet implementation class InsertarRespuestaEncuestaMercadeo
 */
@WebServlet("/InsertarRespuestaEncuestaMercadeo")
public class InsertarRespuestaEncuestaMercadeo extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger
			.getLogger(InsertarRespuestaEncuestaMercadeo.class.getName());
	private EncuestaMercadeoCtrl encuestaCtrl;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public InsertarRespuestaEncuestaMercadeo() {
		super();
	}

	@Override
	public void init() throws ServletException {
		this.encuestaCtrl = new EncuestaMercadeoCtrl();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final StringBuilder constructorJson = new StringBuilder();
			String linea;
			final BufferedReader lector = request.getReader();
			try {
				while ((linea = lector.readLine()) != null) {
					constructorJson.append(linea);
				}
			} finally {
				lector.close();
			}
			final String json = constructorJson.toString();
			if (json.trim().length() == 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Cuerpo vacio.\"}");
				return;
			}
			final JSONParser parser = new JSONParser();
			final JSONObject cuerpo = (JSONObject) parser.parse(json);
			final int idTienda = entero(cuerpo.get("idtienda"));
			final int idPedidoTienda = entero(cuerpo.get("idpedidotienda"));
			final int idCliente = entero(cuerpo.get("idcliente"));
			final int idUsuario = entero(cuerpo.get("idusuario"));
			final Object crudoRespuestas = cuerpo.get("respuestas");
			if (crudoRespuestas == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Falta el arreglo respuestas.\"}");
				return;
			}
			if (idTienda <= 0 || idPedidoTienda <= 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"idtienda e idpedidotienda son obligatorios.\"}");
				return;
			}
			final String jsonRespuestas = ((JSONArray) crudoRespuestas).toJSONString();
			final String resultado = this.encuestaCtrl.registrarRespuestas(idTienda, idPedidoTienda, idCliente,
					idUsuario, jsonRespuestas);
			logger.info("InsertarRespuestaEncuestaMercadeo tienda=" + idTienda + " pedido=" + idPedidoTienda
					+ " resultado=" + resultado);
			out.write(resultado);
		} catch (final Exception e) {
			logger.severe("InsertarRespuestaEncuestaMercadeo: " + e.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"No se pudo registrar la encuesta.\"}");
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.getWriter().write("{\"resultado\":\"ERROR\",\"mensaje\":\"Use POST con cuerpo JSON.\"}");
	}

	/**
	 * Convierte a entero un valor que viene de un JSON, tolerando null, Long y String.
	 *
	 * @param valor valor crudo del JSON
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
}
