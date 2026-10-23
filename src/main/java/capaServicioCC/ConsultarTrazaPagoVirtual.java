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
 * Servicio de consulta de Pagos virtuales y conciliacion: invoca
 * PedidoCtrl.consultarTrazaPagoVirtual.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarTrazaPagoVirtual   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: application/json; charset=UTF-8; claves: detalle, estado, fechahora, fuente.
 * Lee: log_evento_wompi, pedido_gestion_link, pedido_pago_virtual.
 * Sistemas externos: Wompi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarTrazaPagoVirtual.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
