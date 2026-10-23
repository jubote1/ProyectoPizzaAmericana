package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.FidelizacionCtrl;

/**
 * Servicio de consulta de Reportes y estadisticas: invoca
 * FidelizacionCtrl.obtenerSolicitudesReversa.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarSolicitudesReversa   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): estado (string), fechadesde (string), fechahasta (string).
 * Devuelve: application/json; claves: correo, estado, estadoredencion, fecharevision,
 * fechasolicitud, idpedidotienda, idredencion, idsolicitud, idtienda, motivo, observacion,
 * observacionrevision, ....
 * Lee: fidelizacion_redencion, fidelizacion_reversa_solicitud, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarSolicitudesReversa.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarSolicitudesReversa")
public class ConsultarSolicitudesReversa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConsultarSolicitudesReversa() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		try {
			HttpSession sesion = request.getSession(false);
			if (sesion == null || sesion.getAttribute("usuario") == null) {
				out.write("[]");
				return;
			}
			String estado = request.getParameter("estado");
			String fechaDesde = request.getParameter("fechadesde");
			String fechaHasta = request.getParameter("fechahasta");
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			out.write(fidCtrl.obtenerSolicitudesReversa(estado, fechaDesde, fechaHasta));
		} catch (Exception e) {
			System.out.println("ConsultarSolicitudesReversa: " + e.toString());
			out.write("[]");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
