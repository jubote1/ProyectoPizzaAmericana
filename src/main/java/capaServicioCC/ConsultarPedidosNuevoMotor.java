package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import capaDAOCC.ConsultaPedidosNuevoMotorDAO;

/**
 * Endpoint API para la Consulta Unificada de Pedidos del Nuevo Modelo / Multicanal.
 *
 * Mapeo: /ConsultarPedidosNuevoMotor y /api/pedidos/nuevo-motor
 * Metodos: GET, POST
 * Acciones:
 *  - 'consultar' (default): devuelve el listado consolidado y los KPIs.
 *  - 'detalle': devuelve el detalle de productos, mitades y auditoria sombra de un pedido.
 */
@WebServlet({ "/ConsultarPedidosNuevoMotor", "/api/pedidos/nuevo-motor" })
public class ConsultarPedidosNuevoMotor extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarPedidosNuevoMotor() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		procesarPeticion(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		procesarPeticion(request, response);
	}

	private void procesarPeticion(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");

		String accion = request.getParameter("accion");
		if (accion == null || accion.trim().isEmpty()) {
			accion = "consultar";
		}
		accion = accion.trim().toLowerCase();

		PrintWriter out = response.getWriter();

		try {
			if ("detalle".equals(accion)) {
				int idPedido = 0;
				try {
					idPedido = Integer.parseInt(request.getParameter("idpedido"));
				} catch (Exception e) {
				}

				if (idPedido <= 0) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					out.write("{\"error\":true,\"mensaje\":\"Parametro 'idpedido' invalido.\"}");
					return;
				}

				JSONObject detalle = ConsultaPedidosNuevoMotorDAO.consultarDetallePedido(idPedido);
				response.setStatus(HttpServletResponse.SC_OK);
				out.write(detalle.toJSONString());

			} else {
				// Accion consultar listado
				String fechaInicial = request.getParameter("fechainicial");
				String fechaFinal = request.getParameter("fechafinal");
				String canal = request.getParameter("canal");
				String modo = request.getParameter("modo");
				String numeroPedido = request.getParameter("numeropedido");

				int idTienda = 0;
				try {
					idTienda = Integer.parseInt(request.getParameter("idtienda"));
				} catch (Exception e) {
				}

				int idEstadoPedido = 0;
				try {
					idEstadoPedido = Integer.parseInt(request.getParameter("idestadopedido"));
				} catch (Exception e) {
				}

				JSONObject resultado = ConsultaPedidosNuevoMotorDAO.consultarPedidosUnificados(
						fechaInicial, fechaFinal, idTienda, canal, modo, idEstadoPedido, numeroPedido);

				response.setStatus(HttpServletResponse.SC_OK);
				out.write(resultado.toJSONString());
			}

		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"error\":true,\"mensaje\":\"" + e.getMessage() + "\"}");
		}
	}
}
