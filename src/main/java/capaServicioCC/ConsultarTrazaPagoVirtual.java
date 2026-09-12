package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * La historia de un pago virtual: avisos al cliente, eventos de Wompi y gestion.
 *
 * Reemplaza en la pantalla de monitoreo a ConsultarLogEventoWompi, que solo
 * mostraba la parte de Wompi. Con los eventos solos no se entiende nada: no se
 * sabe si al cliente se le alcanzo a mandar el link, ni a que correo, ni si
 * alguien lo llamo.
 */
@WebServlet("/ConsultarTrazaPagoVirtual")
public class ConsultarTrazaPagoVirtual extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConsultarTrazaPagoVirtual() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		int idPedido;
		try {
			idPedido = Integer.parseInt(request.getParameter("idpedido"));
		} catch (Exception e) {
			idPedido = 0;
		}
		PrintWriter out = response.getWriter();
		if (idPedido <= 0) {
			out.write("[]");
			return;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.consultarTrazaPagoVirtual(idPedido));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
