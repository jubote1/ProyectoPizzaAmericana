package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.SegmentacionClienteCtrl;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca
 * SegmentacionClienteCtrl.obtenerCatalogoSegmentacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerCatalogoSegmentacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: especialidades, id, nombre, productos.
 * Lee: especialidad, producto.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerCatalogoSegmentacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerCatalogoSegmentacion")
public class ObtenerCatalogoSegmentacion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ObtenerCatalogoSegmentacion() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			out.write(SegmentacionClienteCtrl.obtenerCatalogoSegmentacion());
		} catch (final Exception e) {
			System.out.println("ObtenerCatalogoSegmentacion: " + e);
			out.write("{\"productos\":[],\"especialidades\":[]}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
