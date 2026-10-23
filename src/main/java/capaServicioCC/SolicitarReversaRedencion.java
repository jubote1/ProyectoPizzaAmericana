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
 * Servicio que modifica datos de Ofertas y fidelizacion (administracion): invoca
 * Usuario.getNombreUsuario, FidelizacionCtrl.solicitarReversaRedencion.
 *
 * Ruta: /ProyectoPizzaAmericana/SolicitarReversaRedencion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idtienda (integer), idpedidotienda (integer), puntos (number), motivo
 * (string), observacion (string), usuario (string).
 * Devuelve: application/json; claves: detalle, idsolicitud, puntossolicitados, respuesta.
 * Lee: fidelizacion_redencion, fidelizacion_reversa_solicitud.
 * Escribe: fidelizacion_reversa_solicitud (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha SolicitarReversaRedencion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/SolicitarReversaRedencion")
public class SolicitarReversaRedencion extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public SolicitarReversaRedencion() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		try {
			int idTienda = 0;
			int idPedidoTienda = 0;
			double puntos = 0;
			try {
				idTienda = Integer.parseInt(request.getParameter("idtienda"));
			} catch (Exception e) {
				idTienda = 0;
			}
			try {
				idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
			} catch (Exception e) {
				idPedidoTienda = 0;
			}
			try {
				puntos = Double.parseDouble(request.getParameter("puntos"));
			} catch (Exception e) {
				puntos = 0;
			}
			String motivo = request.getParameter("motivo");
			if (motivo == null || motivo.trim().equals("")) {
				motivo = "ANULACION";
			}
			String observacion = request.getParameter("observacion");
			if (observacion == null) {
				observacion = "";
			}

			String usuario = "";
			String origen = "";
			HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
				usuario = usuarioSesion.getNombreUsuario();
				origen = "CC";
			} else {
				usuario = request.getParameter("usuario");
				if (usuario == null) {
					usuario = "";
				}
				origen = "POS";
			}

			if (idTienda <= 0 || idPedidoTienda <= 0) {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Falta la tienda o el pedido\"}");
				return;
			}
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			out.write(fidCtrl.solicitarReversaRedencion(idTienda, idPedidoTienda, puntos, motivo, observacion, usuario,
					origen));
		} catch (Exception e) {
			System.out.println("SolicitarReversaRedencion: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico creando la solicitud\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
