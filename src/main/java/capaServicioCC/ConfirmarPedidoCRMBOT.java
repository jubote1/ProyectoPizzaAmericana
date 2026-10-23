package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ConfirmacionPedidoCRMBOTCtrl;

/**
 * Calcula, SIN guardar nada, cuanto cuesta el pedido que el bot armo en el lead: valora cada
 * producto con su precio real, las especialidades, los modificadores, las adiciones, las bebidas,
 * los acompanantes, los productos incluidos y el valor del domicilio, y devuelve el subtotal y el
 * total.
 *
 * Ruta: /ProyectoPizzaAmericana/ConfirmarPedidoCRMBOT   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: application/json;charset=UTF-8; claves: aclaracion, barrio, cantidad, cliente,
 * descripcion, direccion, esDomicilio, fechaPedido, formaPago, horaPedido, incluido, items, ....
 * Lee: especialidad, especialidad_excepcion, excepcion_precio,
 * homologacion_producto_pedidovirtual, integracion_crm, parametros, producto, producto_incluido,
 * sabor_x_tipo_liquido.
 * Escribe: log_pedido_crmbot (INSERT/UPDATE).
 * Sistemas externos: Correo SMTP, GloriaFood/Kuno, Gmail, Kommo (CRM).
 *
 * Trampas y riesgos conocidos:
 * - Depende de Kommo: lee el lead por API y vuelve a escribir en el.
 * - El precio sale de la homologacion por nombre: un texto distinto al esperado deja el renglon
 *   sin valor o genera una observacion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConfirmarPedidoCRMBOT.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet({"/ConfirmarPedidoCRMBOT", "/ConfirmacionPedidoCRMBOT"})
public class ConfirmarPedidoCRMBOT extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ConfirmarPedidoCRMBOT() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");

		String data = "";
		String authHeader = "";

		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(request.getInputStream(), "UTF-8")
			);

			String line;
			StringBuilder sb = new StringBuilder();

			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			data = sb.toString();

			authHeader = request.getHeader("authorization");
			if (authHeader == null) {
				authHeader = "HTTP Authorization header: No authorization header";
			}

			// Procesar la confirmación y cotización
			ConfirmacionPedidoCRMBOTCtrl confirmacionCtrl = new ConfirmacionPedidoCRMBOTCtrl();
			String respuestaJSON = confirmacionCtrl.confirmarPedidoCRMBOT(data, authHeader);

			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(respuestaJSON);
			response.getWriter().flush();

		} catch (Exception e) {
			System.out.println("Error procesando ConfirmarPedidoCRMBOT: " + e.getMessage());
			e.printStackTrace();

			if (!response.isCommitted()) {
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().write("{\"success\":false,\"mensaje\":\"Error al procesar la confirmación del pedido.\"}");
				response.getWriter().flush();
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
