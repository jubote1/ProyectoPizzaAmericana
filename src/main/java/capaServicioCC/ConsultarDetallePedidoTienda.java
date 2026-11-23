package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import capaDAOCC.ConsultaPedidoTiendaDAO;
import capaDAOCC.TiendaDAO;

/**
 * Servicio para consultar el detalle completo de un pedido utilizando el número
 * de pedido generado en la tienda (idpedidotienda) y la tienda seleccionada.
 * 
 * Ruta: /ProyectoPizzaAmericana/ConsultarDetallePedidoTienda
 * Parámetros:
 * - idtienda: ID numérico de la tienda o nombre de la tienda
 * - idpedidotienda: Número de pedido registrado en la tienda
 * 
 * Retorna JSON con:
 * - Datos del pedido (fecha, tipo, totales, tiempo, observaciones)
 * - Datos del cliente (nombre, teléfono, dirección, zona)
 * - Formas de pago aplicadas
 * - Lista de productos (descripción, tamaño, especialidad/sabores, cantidad, valores)
 */
@WebServlet("/ConsultarDetallePedidoTienda")
public class ConsultarDetallePedidoTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger("log_file");

	public ConsultarDetallePedidoTienda() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		procesarSolicitud(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		procesarSolicitud(request, response);
	}

	@SuppressWarnings("unchecked")
	private void procesarSolicitud(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");

		String tiendaParam = request.getParameter("idtienda");
		String pedidoParam = request.getParameter("idpedidotienda");

		// Compatibilidad de nombres de parámetro
		if (pedidoParam == null || pedidoParam.trim().isEmpty()) {
			pedidoParam = request.getParameter("numeropedido");
		}
		if (tiendaParam == null || tiendaParam.trim().isEmpty()) {
			tiendaParam = request.getParameter("tienda");
		}

		int idTienda = 0;
		int idPedidoTienda = 0;

		try {
			if (tiendaParam != null && !tiendaParam.trim().isEmpty()) {
				try {
					idTienda = Integer.parseInt(tiendaParam.trim());
				} catch (NumberFormatException e) {
					idTienda = TiendaDAO.obteneridTienda(tiendaParam.trim());
				}
			}
		} catch (Exception e) {
			logger.warn("Error resolviendo idTienda: " + tiendaParam);
		}

		try {
			if (pedidoParam != null && !pedidoParam.trim().isEmpty()) {
				idPedidoTienda = Integer.parseInt(pedidoParam.trim());
			}
		} catch (Exception e) {
			logger.warn("Error resolviendo idPedidoTienda: " + pedidoParam);
		}

		JSONObject res;
		if (idTienda <= 0 || idPedidoTienda <= 0) {
			res = new JSONObject();
			res.put("exito", false);
			res.put("mensaje", "Parámetros inválidos. Debe proporcionar la tienda y el número de pedido.");
		} else {
			res = ConsultaPedidoTiendaDAO.consultarDetallePedidoTienda(idTienda, idPedidoTienda);
		}

		try (PrintWriter out = response.getWriter()) {
			out.write(res.toJSONString());
			out.flush();
		}
	}
}
