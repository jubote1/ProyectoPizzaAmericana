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
 * Lista las solicitudes de devolucion de puntos para la pantalla de revision.
 *
 * Solo responde con sesion iniciada: es una pantalla administrativa y los
 * datos incluyen el correo del cliente.
 *
 * Parametros: estado (PENDIENTE, APROBADA, RECHAZADA o vacio), fechadesde,
 *             fechahasta
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
