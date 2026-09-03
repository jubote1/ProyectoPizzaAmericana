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
 * Devuelve los puntos de un pedido que NO se finalizo, sin pasar por revision.
 *
 * Este es el caso en que no hubo entrega: el cajero abandono el pedido, se
 * cerro la pantalla de pago, fallo una validacion posterior. No hay nada que
 * revisar y devolver los puntos es lo unico correcto.
 *
 * El pedido que SI se finalizo y luego se anula NO va por aqui: va por
 * SolicitarReversaRedencion, porque ahi el cliente pudo haber recibido el
 * producto y la devolucion necesita aprobacion.
 *
 * Parametros: idtienda, idpedidotienda, puntos (0 = todo), usuario, motivo
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
