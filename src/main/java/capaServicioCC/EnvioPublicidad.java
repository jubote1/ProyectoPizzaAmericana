package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EnvioPublicidadCtrl;
import capaControladorCC.SegmentacionPersonaCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servlet implementation class EnvioPublicidad
 *
 * Todas las acciones de la pantalla de Envio de Publicidad, en un solo sitio y
 * distinguidas por el parametro "accion".
 *
 * Van juntas y no en siete servlets porque comparten exactamente la misma
 * guarda -acceso al CRM- y el mismo armado de filtro. Partirlas obligaria a
 * repetir esa guarda siete veces, y una guarda repetida siete veces es una
 * guarda que algun dia se olvida en la octava.
 */
@WebServlet("/EnvioPublicidad")
public class EnvioPublicidad extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		final PrintWriter out = response.getWriter();

		//Esta pantalla no solo LEE datos personales: los usa para escribirle a
		//la gente. Con mas razon se valida en el servidor y no solo en el menu.
		if (!AccesoCRM.puede(request)) {
			out.write(AccesoCRM.negado());
			return;
		}

		final String accion = request.getParameter("accion") == null
				? "" : request.getParameter("accion").trim();

		if ("alcance".equals(accion)) {
			out.write(EnvioPublicidadCtrl.alcance(SegmentacionPersonaCtrl.filtroDe(request)));

		} else if ("enviar".equals(accion)) {
			out.write(EnvioPublicidadCtrl.crearYEnviar(
					request.getParameter("nombre"),
					request.getParameter("canal"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo"),
					resumenDeFiltros(request),
					SegmentacionPersonaCtrl.filtroDe(request),
					AccesoCRM.usuarioEnSesion(request)));

		} else if ("avance".equals(accion)) {
			out.write(EnvioPublicidadCtrl.avance(largo(request.getParameter("idcampana"))));

		} else if ("ultimas".equals(accion)) {
			final int cuantas = entero(request.getParameter("cuantas"));
			out.write(EnvioPublicidadCtrl.ultimas(cuantas > 0 ? cuantas : 20));

		} else if ("resultado".equals(accion)) {
			final int horas = entero(request.getParameter("horas"));
			out.write(EnvioPublicidadCtrl.resultado(largo(request.getParameter("idcampana")),
					horas > 0 ? horas : 24));

		} else if ("probar".equals(accion)) {
			out.write(EnvioPublicidadCtrl.probar(
					request.getParameter("canal"),
					request.getParameter("destino"),
					entero(request.getParameter("idplantilla")),
					request.getParameter("asunto"),
					request.getParameter("cuerpo")));

		} else if ("tiendas".equals(accion)) {
			out.write(EnvioPublicidadCtrl.tiendas());

		} else if ("detener".equals(accion)) {
			out.write(EnvioPublicidadCtrl.detener());

		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Guarda como texto el filtro con el que se armo la campana.
	 *
	 * No es adorno: dentro de tres meses, viendo que una campana funciono, lo
	 * primero que alguien va a querer saber es a quien se le mando. Sin esto la
	 * respuesta se perdio.
	 */
	private String resumenDeFiltros(final HttpServletRequest request) {
		final StringBuilder t = new StringBuilder();
		final String[] campos = { "segmentos", "idtienda", "pedidosmin", "valormin",
				"diasmin", "diasmax", "canal", "concorreo", "autorizados", "universo" };
		for (int i = 0; i < campos.length; i++) {
			final String valor = request.getParameter(campos[i]);
			if (valor != null && valor.trim().length() > 0) {
				if (t.length() > 0) {
					t.append(" | ");
				}
				t.append(campos[i]).append("=").append(valor.trim());
			}
		}
		return (t.length() == 0 ? "todo el CRM" : t.toString());
	}

	private int entero(final String valor) {
		try {
			return (Integer.parseInt(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	private long largo(final String valor) {
		try {
			return (Long.parseLong(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}
}
