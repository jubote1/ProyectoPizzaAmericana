package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import capaControladorCC.Campana15MinCtrl;

/**
 * Horario por dia de semana de la campana 15 minutos. GET idoperacion=4
 * lista los 7 dias de una campana. POST guarda las 7 filas de una sola vez,
 * con el arreglo en el cuerpo JSON (no caben comodamente como parametros de
 * URL). Exige sesion, es pantalla de administracion.
 */
@WebServlet("/CRUDCampana15MinHorario")
public class CRUDCampana15MinHorario extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDCampana15MinHorario() {
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
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}

		int idCampana = 0;
		try {
			idCampana = Integer.parseInt(request.getParameter("idcampana"));
		} catch (Exception e) {
			idCampana = 0;
		}
		final Campana15MinCtrl ctrl = new Campana15MinCtrl();
		out.write(ctrl.obtenerHorarioSemana(idCampana));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}

		try {
			int idCampana = Integer.parseInt(request.getParameter("idcampana"));
			final StringBuilder constructor = new StringBuilder();
			final BufferedReader lector = request.getReader();
			String linea;
			while ((linea = lector.readLine()) != null) {
				constructor.append(linea);
			}
			final JSONParser parser = new JSONParser();
			final JSONArray dias = (JSONArray) parser.parse(constructor.toString());
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.guardarHorarioSemana(idCampana, dias));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico\"}");
		}
	}
}
