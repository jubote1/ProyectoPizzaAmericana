package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.RuletaCtrl;

/**
 * Servicio que retorna en formato JSON los premios ganados en la ruleta de
 * encuestas dentro de un rango de fechas, con el estado de cada uno: si esta
 * pendiente de dispersar, si ya se disperso, si el cliente no tiene correo o si
 * al premio le falta configurar la oferta.
 *
 * Alimenta la pantalla de dispersion de premios.
 *
 * Parametros:
 *   fechadesde  fecha inicial inclusive, formato aaaa-mm-dd. Obligatoria.
 *   fechahasta  fecha final inclusive, formato aaaa-mm-dd. Obligatoria.
 *
 * Servlet implementation class ConsultarPremiosRuleta
 */
@WebServlet("/ConsultarPremiosRuleta")
public class ConsultarPremiosRuleta extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Tope de dias del rango. Un rango abierto sobre una tabla que crece a diario
	 * traeria miles de filas al navegador sin que nadie las necesite.
	 */
	private static final int MAXIMO_DIAS_RANGO = 92;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ConsultarPremiosRuleta() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final String fechaDesde = texto(request.getParameter("fechadesde"));
			final String fechaHasta = texto(request.getParameter("fechahasta"));

			if (!fechaValida(fechaDesde) || !fechaValida(fechaHasta)) {
				out.write(error("fechadesde y fechahasta son obligatorias, en formato aaaa-mm-dd."));
				return;
			}
			if (fechaDesde.compareTo(fechaHasta) > 0) {
				out.write(error("La fecha inicial no puede ser mayor que la final."));
				return;
			}
			if (diasEntre(fechaDesde, fechaHasta) > MAXIMO_DIAS_RANGO) {
				out.write(error("El rango no puede pasar de " + MAXIMO_DIAS_RANGO + " dias."));
				return;
			}

			final RuletaCtrl ruletaCtrl = new RuletaCtrl();
			out.write(ruletaCtrl.consultarPremios(fechaDesde, fechaHasta));
		} catch (final Exception e) {
			System.out.println("ConsultarPremiosRuleta: " + e.toString());
			out.write(error("Error consultando los premios."));
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Respuesta de error con la misma forma que la exitosa, para que la pantalla
	 * no tenga que distinguir dos estructuras.
	 *
	 * @param mensaje texto a mostrar
	 * @return JSON de error
	 */
	private String error(final String mensaje) {
		return ("{\"resultado\":\"ERROR\",\"mensaje\":\"" + mensaje + "\","
				+ "\"resumen\":{\"total\":0,\"pendientes\":0,\"dispersados\":0,"
				+ "\"sin_correo\":0,\"sin_oferta\":0},\"detalle\":[]}");
	}

	/**
	 * Valida que la fecha tenga forma aaaa-mm-dd y sea real. Se valida aqui y no
	 * solo en la pantalla porque el servicio se puede llamar directo por URL.
	 *
	 * @param fecha texto a validar
	 * @return true si la fecha sirve
	 */
	private boolean fechaValida(final String fecha) {
		if (fecha == null || fecha.length() != 10) {
			return (false);
		}
		try {
			java.time.LocalDate.parse(fecha);
			return (true);
		} catch (final Exception e) {
			return (false);
		}
	}

	/**
	 * Dias entre dos fechas ya validadas.
	 *
	 * @param desde fecha inicial
	 * @param hasta fecha final
	 * @return cantidad de dias
	 */
	private long diasEntre(final String desde, final String hasta) {
		return (java.time.temporal.ChronoUnit.DAYS.between(
				java.time.LocalDate.parse(desde), java.time.LocalDate.parse(hasta)));
	}

	/**
	 * Retorna el texto sin espacios, o cadena vacia si viene null.
	 *
	 * @param valor texto crudo
	 * @return el texto, nunca null
	 */
	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
