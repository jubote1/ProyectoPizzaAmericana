package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import capaControladorCC.AutenticacionCtrl;

/**
 * Servicio de consulta de Seguridad, usuarios y roles: invoca
 * AutenticacionCtrl.obtenerUsuarioActivo.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerUsuariosActivos   Metodos: GET.
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: error.
 * Lee: usuario.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerUsuariosActivos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerUsuariosActivos")
public class ObtenerUsuariosActivos extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			
			AutenticacionCtrl usuCtrl = new AutenticacionCtrl();
			String respuesta = usuCtrl.obtenerUsuarioActivo();

			PrintWriter out = response.getWriter();
			out.write(respuesta);
			out.flush(); 
			out.close(); 

		} catch (Exception e) {
			System.out.println("Error al obtener usuarios activos: " + e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\":\"Error en el servidor\"}");
		}
	}
}
