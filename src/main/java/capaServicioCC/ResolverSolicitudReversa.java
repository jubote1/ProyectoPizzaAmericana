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
 * Servicio que modifica datos de Sin clasificar (revisar a mano): invoca Usuario.getNombreUsuario,
 * FidelizacionCtrl.resolverSolicitudReversa.
 *
 * Ruta: /ProyectoPizzaAmericana/ResolverSolicitudReversa   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idsolicitud (integer), aprobar (string), observacion (string).
 * Devuelve: application/json; claves: detalle, respuesta.
 * Lee: fidelizacion_redencion, fidelizacion_redencion_detalle, fidelizacion_reversa_solicitud.
 * Escribe: cliente_fidelizacion (UPDATE), fidelizacion_redencion (UPDATE),
 * fidelizacion_redencion_detalle (UPDATE), fidelizacion_reversa_solicitud (UPDATE),
 * fidelizacion_transaccion (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ResolverSolicitudReversa.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
