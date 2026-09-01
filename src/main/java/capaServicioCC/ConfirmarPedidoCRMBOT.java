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
 * Servlet encargado de recibir la solicitud de confirmación / cotización de un pedido
 * proveniente del CRM BOT (Kommo) antes de que se realice la inserción en la base de datos.
 *
 * Mapeado a las URLs:
 * - /ConfirmarPedidoCRMBOT
 * - /ConfirmacionPedidoCRMBOT
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
