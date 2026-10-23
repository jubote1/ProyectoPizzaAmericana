package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.Campana15MinCtrl;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * Campana15MinCtrl.obtenerCampanaActiva.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerCampana15MinActiva   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: exclusiones, hayCampana, idcampana, mensajefactura,
 * mensajeoperario, minutospromesa, nombre, porcentajeretencionmediovirtual.
 * Lee: campana_15min_config, campana_15min_exclusion, campana_15min_horario_dia.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerCampana15MinActiva.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerCampana15MinActiva")
public class ObtenerCampana15MinActiva extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ObtenerCampana15MinActiva() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final Campana15MinCtrl ctrl = new Campana15MinCtrl();
		final PrintWriter out = response.getWriter();
		out.write(ctrl.obtenerCampanaActiva());
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
