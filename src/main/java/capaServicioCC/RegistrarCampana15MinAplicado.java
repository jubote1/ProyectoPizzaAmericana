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
 * El POS llama esto en el momento exacto en que envia el pedido a cocina, si
 * quedo marcado como elegible para "15 minutos o gratis". Best-effort: si
 * falla, el POS ya marco el pedido localmente y sigue, el caso simplemente no
 * entra a la cola central de revision.
 *
 * Parametros: idpedidotienda, idtienda, idcampana, fechahorainicio
 * (aaaa-mm-dd hh:mm:ss), valorbasepizza, nombrecliente, celularcliente
 */
@WebServlet("/RegistrarCampana15MinAplicado")
public class RegistrarCampana15MinAplicado extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinAplicado() {
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
			int idCampana = Integer.parseInt(request.getParameter("idcampana"));
			String fechaHoraInicio = request.getParameter("fechahorainicio");
			double valorBasePizza = Double.parseDouble(request.getParameter("valorbasepizza"));
			String nombreCliente = request.getParameter("nombrecliente");
			String celularCliente = request.getParameter("celularcliente");
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarAplicado(idPedidoTienda, idTienda, idCampana, fechaHoraInicio, valorBasePizza,
					nombreCliente, celularCliente));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
