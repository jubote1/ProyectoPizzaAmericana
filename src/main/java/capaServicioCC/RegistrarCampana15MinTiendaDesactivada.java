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
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca
 * Campana15MinCtrl.registrarTiendaDesactivada.
 *
 * Ruta: /ProyectoPizzaAmericana/RegistrarCampana15MinTiendaDesactivada   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer), fecha (string), motivo (string), usuarioautoriza
 * (string), usuariodesactiva (string).
 * Devuelve: application/json; claves: respuesta.
 * Escribe: campana_15min_tienda_desactivada_dia (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RegistrarCampana15MinTiendaDesactivada.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RegistrarCampana15MinTiendaDesactivada")
public class RegistrarCampana15MinTiendaDesactivada extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinTiendaDesactivada() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
			String fecha = request.getParameter("fecha");
			String motivo = request.getParameter("motivo");
			String usuarioAutoriza = request.getParameter("usuarioautoriza");
			String usuarioDesactiva = request.getParameter("usuariodesactiva");
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarTiendaDesactivada(idTienda, fecha, motivo, usuarioAutoriza, usuarioDesactiva));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
