package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.RestriccionTiendaCtrl;

/**
 * Servicio que dice si el usuario de la sesion solo puede ver una tienda.
 *
 * Lo usan los reportes para dejar en el combo unicamente la tienda que le
 * corresponde al administrador de punto de venta. Es solo para la pantalla:
 * la restriccion de verdad la aplica cada servicio de consulta, porque un
 * combo recortado no detiene a nadie que arme la URL a mano.
 *
 * Ruta: /ProyectoPizzaAmericana/GetTiendaPermitida   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion de usuario.
 * Recibe: nada.
 * Devuelve: application/json; charset=UTF-8; claves: restringido, idtienda, tienda, origen.
 * Lee: usuario, tienda (en pizzaamericana), empleado, horario_trabajado (en general).
 */
@WebServlet("/GetTiendaPermitida")
public class GetTiendaPermitida extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"error\":\"Debe iniciar sesion\"}");
			return;
		}
		out.write(RestriccionTiendaCtrl.aJson(RestriccionTiendaCtrl.de(request)));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
