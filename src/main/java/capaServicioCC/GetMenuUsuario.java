package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.MenuCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class GetMenuUsuario
 * Retorna el arbol de menu (modulo -> pantallas) que el usuario logueado
 * puede ver, segun sus roles. Es lo que reemplaza a Menu.html/MenuAdm.html/
 * MenuPQRS.html estaticos: el HTML del menu pasa a ser uno solo, y pinta lo
 * que este servicio traiga. Mismo patron de lectura de sesion que
 * ValidarUsuarioAplicacion.
 */
@WebServlet("/GetMenuUsuario")
public class GetMenuUsuario extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		Logger logger = Logger.getLogger("log_file");
		String respuesta = "[]";
		try {
			HttpSession miSesion = request.getSession();
			Usuario usuario = (Usuario) miSesion.getAttribute("usuario");
			MenuCtrl menuCtrl = new MenuCtrl();
			respuesta = menuCtrl.obtenerMenuUsuario(usuario.getId());
		} catch (Exception e) {
			logger.error(e.toString());
		}
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
