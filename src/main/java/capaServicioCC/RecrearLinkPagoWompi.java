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
 * Recrea el link de pago de un pedido.
 *
 * Recibe solo el numero del pedido. El monto, la tienda y el cliente se leen de
 * la base: antes el navegador armaba el link con el valor que tuviera la reja en
 * pantalla, y eso se puede cambiar antes de mandarlo.
 *
 * Pide sesion. La clave privada de Wompi se usa aqui adentro y no baja al
 * navegador, que era el problema de fondo de la pantalla anterior.
 */
@WebServlet("/RecrearLinkPagoWompi")
public class RecrearLinkPagoWompi extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public RecrearLinkPagoWompi() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
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
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.recrearLinkPagoWompi(idPedido));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
