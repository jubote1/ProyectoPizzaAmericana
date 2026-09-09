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
import capaModeloCC.Usuario;

/**
 * Aprueba o rechaza una solicitud de devolucion de puntos.
 *
 * Exige sesion iniciada y toma de ahi el usuario que aprueba. No se acepta el
 * usuario como parametro: quien autoriza una devolucion de puntos tiene que
 * quedar registrado de forma que no se pueda alterar desde el navegador. Si la
 * solicitud se aprueba, la devolucion de los puntos y el cambio de estado van
 * en la misma transaccion.
 *
 * Parametros: idsolicitud, aprobar (S o N), observacion
 */
@WebServlet("/ResolverSolicitudReversa")
public class ResolverSolicitudReversa extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ResolverSolicitudReversa() {
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
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			out.write(fidCtrl.resolverSolicitudReversa(idSolicitud, aprobar, usuario, observacion));
		} catch (Exception e) {
			System.out.println("ResolverSolicitudReversa: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico procesando la solicitud\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
