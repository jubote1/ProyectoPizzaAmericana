package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.Persona360Ctrl;

/**
 * Servlet implementation class ConsultarPersona360
 * Todo lo de una persona: sus filas de cliente, pedidos, ofertas y PQRS.
 */
@WebServlet("/ConsultarPersona360")
public class ConsultarPersona360 extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		long idPersona = 0;
		try {
			idPersona = Long.parseLong(request.getParameter("idpersona"));
		} catch (Exception e) {
			idPersona = 0;
		}
		logger.info("consultar persona 360 idpersona=" + idPersona);
		String respuesta = Persona360Ctrl.consultar(idPersona);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
