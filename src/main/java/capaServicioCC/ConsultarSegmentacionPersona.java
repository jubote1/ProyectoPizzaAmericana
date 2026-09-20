package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.SegmentacionPersonaCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class ConsultarSegmentacionPersona
 * Cuenta y lista personas de crm.persona_resumen segun los filtros del CRM.
 */
@WebServlet("/ConsultarSegmentacionPersona")
public class ConsultarSegmentacionPersona extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		//Datos personales de 450 mil personas. Esconder la opcion del menu no
		//protege nada: esta URL se escribe a mano.
		if (!AccesoCRM.puede(request)) {
			PrintWriter negado = response.getWriter();
			negado.write(AccesoCRM.negado());
			return;
		}
		String respuesta = SegmentacionPersonaCtrl.consultar(request);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
