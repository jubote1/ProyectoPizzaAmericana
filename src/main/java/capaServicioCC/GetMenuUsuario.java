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
 * Servicio de consulta de Seguridad, usuarios y roles: invoca MenuCtrl.obtenerMenuUsuario.
 *
 * Ruta: /ProyectoPizzaAmericana/GetMenuUsuario   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Lee la sesion pero no la exige.
 * Devuelve: application/json; claves: idmodulo, idpantalla, nombre, pantallas, url_html.
 * Lee: menu_modulo, pantalla, rol, rol_pantalla, usuario_rol.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetMenuUsuario.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
