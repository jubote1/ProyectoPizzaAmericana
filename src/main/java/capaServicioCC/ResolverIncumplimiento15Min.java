package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.Campana15MinCtrl;
import capaModeloCC.Usuario;

/**
 * Aprueba o rechaza un caso de incumplimiento de "15 minutos o gratis". Exige
 * sesion iniciada y toma de ahi el usuario que decide, igual que
 * ResolverSolicitudReversa. Al aprobar el sistema NO devuelve dinero, solo
 * deja el caso con el monto ya calculado para que Servicio al Cliente lo
 * ejecute por fuera.
 *
 * Parametros: idsolicitud, aprobar (S o N), observacion
 */
@WebServlet("/ResolverIncumplimiento15Min")
public class ResolverIncumplimiento15Min extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ResolverIncumplimiento15Min() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		try {
			HttpSession sesion = request.getSession(false);
			if (sesion == null || sesion.getAttribute("usuario") == null) {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion para aprobar o rechazar\"}");
				return;
			}
			Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
			String usuario = usuarioSesion.getNombreUsuario();

			int idSolicitud = 0;
			try {
				idSolicitud = Integer.parseInt(request.getParameter("idsolicitud"));
			} catch (Exception e) {
				idSolicitud = 0;
			}
			boolean aprobar = "S".equalsIgnoreCase(request.getParameter("aprobar"));
			String observacion = request.getParameter("observacion");
			if (observacion == null) {
				observacion = "";
			}
			if (idSolicitud <= 0) {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Falta el idsolicitud\"}");
				return;
			}
			Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.resolverIncumplimiento(idSolicitud, aprobar, usuario, observacion));
		} catch (Exception e) {
			System.out.println("ResolverIncumplimiento15Min: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico procesando la solicitud\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
