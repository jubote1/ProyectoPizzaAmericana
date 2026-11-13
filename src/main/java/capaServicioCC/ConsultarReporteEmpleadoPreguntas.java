package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PreguntaEmpleadoResultadoCtrl;
import capaModeloCC.Usuario;

/**
 * Reporte de UN empleado en un rango: como le ha ido en las preguntas que el huellero le hace al dar ingreso.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarReporteEmpleadoPreguntas   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: pide sesion iniciada (no revisa rol: es la misma informacion de ResultadoPreguntasEmpleado).
 * Recibe (query/form): idempleado (integer), fechadesde, fechahasta (yyyy-MM-dd o dd/MM/yyyy).
 * Devuelve: application/json; charset=UTF-8; claves: error, empleado, desde, hasta, kpis, comparativo, semanas,
 * porpregunta, respuestas, pendientes.
 * Lee: empleado, tipo_empleado, respuesta_empleado, pregunta_empleado, pregunta_empleado_tipo, opcion_respuesta,
 * opcion_respuesta_pregunta (esquema general).
 */
@WebServlet("/ConsultarReporteEmpleadoPreguntas")
public class ConsultarReporteEmpleadoPreguntas extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		final HttpSession sesion = request.getSession(false);
		if (sesion == null || !(sesion.getAttribute("usuario") instanceof Usuario)) {
			out.write("{\"error\":\"Debe iniciar sesion.\"}");
			return;
		}
		int idEmpleado = 0;
		try {
			idEmpleado = Integer.parseInt(request.getParameter("idempleado").trim());
		} catch (final Exception e) {
			idEmpleado = 0;
		}
		final String desde = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechadesde"));
		final String hasta = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechahasta"));
		out.write(PreguntaEmpleadoResultadoCtrl.consultarReporte(idEmpleado, desde, hasta));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
