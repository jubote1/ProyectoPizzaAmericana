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
 * Las ubicaciones de los pedidos de domicilio de una tienda en un rango de fechas, para el mapa de calor
 * (MapaCalorDomicilios.html). Ver PedidoCtrl.consultarDireccionesDomicilio.
 *
 * Parametros: fechainicial, fechafinal (dd/MM/yyyy), idtienda.
 */
@WebServlet("/ConsultarDireccionesDomicilio")
public class ConsultarDireccionesDomicilio extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
		int idTienda;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}
		PrintWriter out = response.getWriter();
		if (idTienda <= 0 || fechaInicial == null || fechaFinal == null) {
			out.write("[]");
			return;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.consultarDireccionesDomicilio(fechaInicial, fechaFinal, idTienda));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
