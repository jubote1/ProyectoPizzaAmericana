package capaServicioCC;



import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.EncuestaCtrl;
import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.TiendaCtrl;


@WebServlet("/ObtenerMonitoreoEstadoPedido")
public class ObtenerMonitoreoEstadoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		response.setContentType("application/json");

		try (PrintWriter out = response.getWriter()) {
			TiendaCtrl tiendaCtrl = new TiendaCtrl();
			String respuesta = tiendaCtrl.obtenerMonitoreoEstadoLocal();
			out.write(respuesta);
			out.flush();
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try (PrintWriter out = response.getWriter()) {
				out.write("{\"error\": \"Error interno: " + e.getMessage().replace("\"", "'") + "\"}");
				out.flush();
			}
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}


