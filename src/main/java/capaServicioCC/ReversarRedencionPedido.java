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
 * Devuelve los puntos de una redencion cuando el pedido no se llego a hacer.
 *
 * Ruta: /ProyectoPizzaAmericana/ReversarRedencionPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idtienda (integer), idpedidotienda (integer), puntos (number), motivo
 * (string), usuario (string), idredencion (integer).
 * Devuelve: application/json; claves: detalle, idredencion, puntosdevueltos, respuesta.
 * Lee: fidelizacion_redencion, fidelizacion_redencion_detalle.
 * Escribe: cliente_fidelizacion (UPDATE), fidelizacion_redencion (UPDATE),
 * fidelizacion_redencion_detalle (UPDATE), fidelizacion_transaccion (UPDATE).
 *
 * Trampas y riesgos conocidos:
 * - Devolver puntos es dinero: cualquiera que conozca idredencion podria reversar. Revisar que
 *   el servicio verifique el estado de la redencion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ReversarRedencionPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ReversarRedencionPedido")
public class ReversarRedencionPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ReversarRedencionPedido() {
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
			if (motivo == null) {
				motivo = "Pedido no finalizado";
			}

			// Igual que en la redencion: el contact center llega con sesion y de
			// ahi se toma el usuario, que es la fuente confiable. El POS no tiene
			// sesion y lo envia como parametro.
			String usuario = "";
			HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
				usuario = usuarioSesion.getNombreUsuario();
			} else {
				usuario = request.getParameter("usuario");
				if (usuario == null) {
					usuario = "";
				}
			}

			/*
			 * Se puede ubicar la redencion de dos formas. Por idredencion, que es
			 * el que devolvio la propia llamada de redencion, o por tienda y
			 * pedido. Se prefiere el idredencion: cuando se cancela un pedido, el
			 * pedido se borra y despues ya no hay con que buscarla.
			 */
			int idRedencion = 0;
			try {
				idRedencion = Integer.parseInt(request.getParameter("idredencion"));
			} catch (Exception e) {
				idRedencion = 0;
			}
			if (idRedencion > 0) {
				FidelizacionCtrl fidCtrlId = new FidelizacionCtrl();
				out.write(fidCtrlId.reversarRedencionPorId(idRedencion, puntos, usuario, motivo));
				return;
			}

			if (idTienda <= 0 || idPedidoTienda <= 0) {
				out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Falta la tienda o el pedido\"}");
				return;
			}
			FidelizacionCtrl fidCtrl = new FidelizacionCtrl();
			out.write(fidCtrl.reversarRedencionPedido(idTienda, idPedidoTienda, puntos, usuario, motivo));
		} catch (Exception e) {
			System.out.println("ReversarRedencionPedido: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico reversando la redencion\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
