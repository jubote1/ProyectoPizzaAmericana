package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utilidadesCC.AccesoCRM;
import capaControladorCC.Persona360Ctrl;

/**
 * Servlet implementation class ConsultarResumenCRM
 * El estado del maestro de personas, para la pagina de inicio del CRM.
 */
@WebServlet("/ConsultarResumenCRM")
public class ConsultarResumenCRM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		//El CRM entrega datos personales de 450 mil personas. Esconder la
		//opcion del menu no protege nada: esta URL se puede escribir a mano.
		if (!AccesoCRM.puede(request)) {
			PrintWriter negado = response.getWriter();
			negado.write(AccesoCRM.negado());
			return;
		}
		String respuesta = Persona360Ctrl.resumen();
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
