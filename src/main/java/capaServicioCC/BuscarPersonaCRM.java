package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.Persona360Ctrl;

/**
 * Servlet implementation class BuscarPersonaCRM
 * Busca personas en el maestro del CRM por celular, correo o nombre. Decide
 * sola por donde buscar segun lo que escribieron.
 */
@WebServlet("/BuscarPersonaCRM")
public class BuscarPersonaCRM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		String respuesta = Persona360Ctrl.buscar(request.getParameter("texto"));
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
