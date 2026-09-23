package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class ValidarAccesoCRM
 * Dice si el usuario en sesion puede entrar al area de CRM. Lo usan las
 * pantallas del area para no dejar entrar, y los menus para mostrar u ocultar
 * la opcion.
 *
 * Que este servicio diga que si NO es lo que protege: cada servicio del CRM
 * vuelve a preguntar por su cuenta antes de entregar datos.
 */
@WebServlet("/ValidarAccesoCRM")
public class ValidarAccesoCRM extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		String usuario = AccesoCRM.usuarioEnSesion(request);
		boolean haySesion = (usuario != null);
		boolean puede = haySesion && AccesoCRM.tieneAcceso(usuario);
		PrintWriter out = response.getWriter();
		out.write("{\"sesion\":" + haySesion + ",\"acceso\":" + puede + "}");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
