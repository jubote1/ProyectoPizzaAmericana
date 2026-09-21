package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.DesempenoDomiciliarioCtrl;

/**
 * Servlet implementation class ConsultarDomiciliariosActivos
 * Los domiciliarios activos, para el selector del tablero de desempeno. Sale
 * de general.empleado cruzado con la marca tipo_empleado.es_domiciliario.
 */
@WebServlet("/ConsultarDomiciliariosActivos")
public class ConsultarDomiciliariosActivos extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		String respuesta = DesempenoDomiciliarioCtrl.obtenerDomiciliarios();
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
