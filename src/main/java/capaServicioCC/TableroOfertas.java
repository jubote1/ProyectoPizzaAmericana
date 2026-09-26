package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.TableroOfertasCtrl;
import utilidadesCC.AccesoCRM;

/** Tablero de control de ofertas y codigos. Pide sesion. Ver TableroOfertasCtrl. */
@WebServlet("/TableroOfertas")
public class TableroOfertas extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (AccesoCRM.usuarioEnSesion(request) == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write("{\"error\":\"Debe iniciar sesion.\"}");
			return;
		}
		int dias = 30;
		try {
			dias = Integer.parseInt(request.getParameter("dias").trim());
		} catch (final Exception e) {
			dias = 30;
		}
		out.write(TableroOfertasCtrl.tablero(dias));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
