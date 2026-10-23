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
 * Servicio de consulta de Seguridad, usuarios y roles: invoca AccesoCRM.usuarioEnSesion,
 * AccesoCRM.tieneAcceso.
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarAccesoCRM   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; charset=UTF-8; claves: acceso, sesion.
 * Lee: usuario.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarAccesoCRM.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
