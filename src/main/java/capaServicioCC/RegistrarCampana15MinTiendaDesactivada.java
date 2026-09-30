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
 * El POS llama esto, best-effort, cuando un administrador de tienda desactiva
 * la campana 15 minutos por hoy (horno danado, falta de personal, etc.). El
 * POS ya aplico la desactivacion localmente antes de llamar aqui: esto es
 * solo para que quede visible en el central. Si falla, no importa -- el POS
 * no depende de esta respuesta.
 *
 * Parametros: idtienda, fecha (aaaa-mm-dd), motivo, usuarioautoriza,
 * usuariodesactiva
 */
@WebServlet("/RegistrarCampana15MinTiendaDesactivada")
public class RegistrarCampana15MinTiendaDesactivada extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinTiendaDesactivada() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
			String fecha = request.getParameter("fecha");
			String motivo = request.getParameter("motivo");
			String usuarioAutoriza = request.getParameter("usuarioautoriza");
			String usuarioDesactiva = request.getParameter("usuariodesactiva");
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarTiendaDesactivada(idTienda, fecha, motivo, usuarioAutoriza, usuarioDesactiva));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
