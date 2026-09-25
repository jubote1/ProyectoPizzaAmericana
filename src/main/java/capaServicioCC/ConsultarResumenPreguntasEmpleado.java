package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PreguntaEmpleadoResultadoCtrl;

/**
 * Servlet implementation class ConsultarResumenPreguntasEmpleado
 * Los empleados activos con cuantas preguntas del huellero contestaron en el
 * rango y su porcentaje de acierto. Parametros: fechadesde y fechahasta, en
 * yyyy-MM-dd o dd/MM/yyyy.
 */
@WebServlet("/ConsultarResumenPreguntasEmpleado")
public class ConsultarResumenPreguntasEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		final String desde = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechadesde"));
		final String hasta = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechahasta"));
		final PrintWriter out = response.getWriter();
		out.write(PreguntaEmpleadoResultadoCtrl.consultarResumen(desde, hasta));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
