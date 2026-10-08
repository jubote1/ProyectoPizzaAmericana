package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.Campana15MinCtrl;

/**
 * Lista los casos de incumplimiento de la campana 15 minutos para la pantalla
 * de revision. Parametros: estado, fechadesde, fechahasta (todos opcionales).
 */
@WebServlet("/ConsultarIncumplimientos15Min")
public class ConsultarIncumplimientos15Min extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarIncumplimientos15Min() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("[]");
			return;
		}

		String estado = request.getParameter("estado");
		String fechaDesde = request.getParameter("fechadesde");
		String fechaHasta = request.getParameter("fechahasta");
		final Campana15MinCtrl ctrl = new Campana15MinCtrl();
		out.write(ctrl.obtenerIncumplimientos(estado, fechaDesde, fechaHasta));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
