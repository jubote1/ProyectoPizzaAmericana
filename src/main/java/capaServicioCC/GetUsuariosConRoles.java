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
 * Servicio de consulta de Seguridad, usuarios y roles: invoca RolCtrl.listarUsuariosConRoles.
 *
 * Ruta: /ProyectoPizzaAmericana/GetUsuariosConRoles   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: idusuario, nombrelargo, nombreusuario, roles.
 * Lee: rol, usuario, usuario_rol.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetUsuariosConRoles.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
