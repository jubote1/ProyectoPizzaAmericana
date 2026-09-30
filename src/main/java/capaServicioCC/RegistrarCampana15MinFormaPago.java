package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.Campana15MinCtrl;

/**
 * El POS llama esto al cerrar el pago de un pedido marcado con la campana,
 * para completar si el medio de pago fue virtual (datafono/QR) y asi poder
 * calcular la retencion del 5% si mas tarde hay que devolver el valor.
 *
 * Parametros: idpedidotienda, idtienda, esmediovirtual (S/N)
 */
@WebServlet("/RegistrarCampana15MinFormaPago")
public class RegistrarCampana15MinFormaPago extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinFormaPago() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			int idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
			boolean esMedioVirtual = "S".equalsIgnoreCase(request.getParameter("esmediovirtual"));
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarFormaPago(idPedidoTienda, idTienda, esMedioVirtual));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
