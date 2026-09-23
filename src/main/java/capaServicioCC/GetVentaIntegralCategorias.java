package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.VentaIntegralCtrl;

/**
 * Servlet implementation class GetVentaIntegralCategorias
 * Retorna el catalogo completo de categorias de Venta Integral (activas e
 * inactivas), con sus items de tienda y de contact center, en formato JSON.
 * No toma parametros, mismo patron que GetEspecialidades.
 */
@WebServlet("/GetVentaIntegralCategorias")
public class GetVentaIntegralCategorias extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public GetVentaIntegralCategorias() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			VentaIntegralCtrl ventaIntegralCtrl = new VentaIntegralCtrl();
			String respuesta = ventaIntegralCtrl.listarCategorias();
			PrintWriter out = response.getWriter();
			out.write(respuesta);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
