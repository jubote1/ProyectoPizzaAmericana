package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EncuestaMercadeoCtrl;

/**
 * Servicio que retorna en formato JSON el reporte de respuestas de las encuestas de
 * mercadeo tomadas en el POS, en dos vistas a la vez: el resumen agrupado por
 * pregunta y respuesta con su porcentaje, y el detalle fila por fila.
 *
 * Parametros:
 *   idtienda      tienda a filtrar. 0 o ausente = todas.
 *   fechainicial  fecha inicial inclusive, formato aaaa-mm-dd. Obligatoria.
 *   fechafinal    fecha final inclusive, formato aaaa-mm-dd. Obligatoria.
 *   idpregunta    pregunta a filtrar. 0 o ausente = todas.
 *
 * Servlet implementation class ConsultarEncuestaMercadeo
 */
@WebServlet("/ConsultarEncuestaMercadeo")
public class ConsultarEncuestaMercadeo extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ConsultarEncuestaMercadeo() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final int idTienda = entero(request.getParameter("idtienda"));
			final int idPregunta = entero(request.getParameter("idpregunta"));
			final String fechaInicial = texto(request.getParameter("fechainicial"));
			final String fechaFinal = texto(request.getParameter("fechafinal"));
			if (fechaInicial.length() == 0 || fechaFinal.length() == 0) {
				out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"fechainicial y fechafinal son obligatorias.\","
						+ "\"resumen\":[],\"detalle\":[]}");
				return;
			}
			final EncuestaMercadeoCtrl encCtrl = new EncuestaMercadeoCtrl();
			out.write(encCtrl.consultarReporte(idTienda, fechaInicial, fechaFinal, idPregunta));
		} catch (final Exception e) {
			System.out.println("ConsultarEncuestaMercadeo: " + e.toString());
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Error consultando el reporte.\","
					+ "\"resumen\":[],\"detalle\":[]}");
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Convierte a entero sin lanzar excepcion.
	 *
	 * @param valor texto a convertir
	 * @return el entero, o 0 si no se puede convertir
	 */
	private int entero(final String valor) {
		int resultado = 0;
		if (valor != null) {
			try {
				resultado = Integer.parseInt(valor.trim());
			} catch (final Exception e) {
				resultado = 0;
			}
		}
		return (resultado);
	}

	/**
	 * Retorna el texto sin espacios, o cadena vacia si viene null.
	 *
	 * @param valor texto crudo
	 * @return el texto, nunca null
	 */
	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
