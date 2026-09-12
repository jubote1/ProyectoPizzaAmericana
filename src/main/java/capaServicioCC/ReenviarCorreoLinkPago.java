package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Reenvia por correo el link de pago, con la opcion de otro correo.
 *
 * Cuando el cliente dice que no le llego, lo primero que hay que poder hacer es
 * mandarselo otra vez; y lo segundo, mandarlo a otra direccion, porque la causa
 * mas frecuente es que el correo que tenemos este mal escrito.
 *
 * Pide sesion: manda correo a nombre de la empresa y toca la ficha del cliente.
 */
@WebServlet("/ReenviarCorreoLinkPago")
public class ReenviarCorreoLinkPago extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ReenviarCorreoLinkPago() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		HttpSession sesion = request.getSession(false);
		Usuario usuario = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");
		if (usuario == null) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"La sesion se vencio. Vuelva a entrar.\"}");
			return;
		}
		int idPedido;
		try {
			idPedido = Integer.parseInt(request.getParameter("idpedido"));
		} catch (Exception e) {
			idPedido = 0;
		}
		if (idPedido <= 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"No se indico el pedido.\"}");
			return;
		}
		String correo = request.getParameter("correo");
		String actualizar = request.getParameter("actualizar");
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.reenviarCorreoLinkPago(idPedido, correo, actualizar));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
