package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.RolCtrl;

/**
 * Servlet implementation class GetUsuariosConRoles
 * Retorna los usuarios activos con sus roles actuales, para la pantalla de
 * asignacion de roles a usuario.
 */
@WebServlet("/GetUsuariosConRoles")
public class GetUsuariosConRoles extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			RolCtrl rolCtrl = new RolCtrl();
			PrintWriter out = response.getWriter();
			out.write(rolCtrl.listarUsuariosConRoles());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
