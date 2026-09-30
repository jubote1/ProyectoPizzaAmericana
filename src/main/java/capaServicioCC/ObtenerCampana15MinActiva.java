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
 * Devuelve la campana "15 minutos o gratis" activa ahora mismo (fecha, dia y
 * hora dentro de ventana), con sus exclusiones. El POS la llama al abrir turno
 * y la cachea; si esta llamada falla o no hay campana activa, el POS sigue
 * normal sin mostrar el interceptor.
 */
@WebServlet("/ObtenerCampana15MinActiva")
public class ObtenerCampana15MinActiva extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ObtenerCampana15MinActiva() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final Campana15MinCtrl ctrl = new Campana15MinCtrl();
		final PrintWriter out = response.getWriter();
		out.write(ctrl.obtenerCampanaActiva());
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
