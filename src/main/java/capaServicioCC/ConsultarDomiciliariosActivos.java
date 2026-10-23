package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.DesempenoDomiciliarioCtrl;

/**
 * Servicio de consulta de Domiciliarios y enrutamiento: invoca
 * DesempenoDomiciliarioCtrl.obtenerDomiciliarios.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDomiciliariosActivos   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; charset=UTF-8; claves: domiciliarios, id, nombre, nombrelargo, tipo.
 * Lee: empleado, tipo_empleado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDomiciliariosActivos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDomiciliariosActivos")
public class ConsultarDomiciliariosActivos extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		String respuesta = DesempenoDomiciliarioCtrl.obtenerDomiciliarios();
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
