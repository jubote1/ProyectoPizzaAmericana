package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.RuletaCtrl;
import capaModeloCC.Usuario;

/**
 * Servicio que dispersa los premios pendientes de la ruleta de encuestas en un
 * rango de fechas: a cada ganador le asigna la oferta que corresponde a su
 * premio, le genera el codigo promocional y le envia el correo.
 *
 * A diferencia de las consultas, este servicio EXIGE sesion. Son dos razones:
 * asigna beneficios reales a clientes y manda correos, asi que no puede quedar
 * expuesto a que lo llamen por URL; y ademas hay que dejar registrado quien lo
 * ejecuto, que es lo que va a usuario_dispersion.
 *
 * Parametros:
 *   fechadesde  fecha inicial inclusive, formato aaaa-mm-dd. Obligatoria.
 *   fechahasta  fecha final inclusive, formato aaaa-mm-dd. Obligatoria.
 *
 * Servlet implementation class DispersarPremiosRuleta
 */
@WebServlet("/DispersarPremiosRuleta")
public class DispersarPremiosRuleta extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Tope de dias del rango, igual que en la consulta.
	 */
	private static final int MAXIMO_DIAS_RANGO = 92;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DispersarPremiosRuleta() {
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			// Quien dispersa. Se toma de la sesion y nunca de un parametro: si
			// llegara por parametro, cualquiera podria firmar la dispersion con el
			// nombre de otro y el registro de usuario_dispersion no serviria de nada.
			final String usuario = usuarioSesion(request);
			if (usuario.length() == 0) {
				out.write(error("Su sesion expiro. Vuelva a ingresar para dispersar los premios."));
				return;
			}

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

			// REENVIAR solo reintenta el correo de los que quedaron sin avisar; no
			// asigna ofertas. Van en el mismo servicio porque comparten la validacion
			// de sesion y de fechas, y porque son la misma decision de negocio vista
			// desde dos momentos.
			final RuletaCtrl ruletaCtrl = new RuletaCtrl();
			if ("REENVIAR".equalsIgnoreCase(texto(request.getParameter("accion")))) {
				out.write(ruletaCtrl.reenviarCorreos(fechaDesde, fechaHasta, usuario));
			} else {
				out.write(ruletaCtrl.dispersarPremios(fechaDesde, fechaHasta, usuario));
			}

		} catch (final Exception e) {
			System.out.println("DispersarPremiosRuleta: " + e.toString());
			out.write(error("Error dispersando los premios. Vuelva a consultar para ver que "
					+ "quedo dispersado antes de reintentar."));
		}
	}

	/**
	 * Se responde igual que el POST para no dejar dos comportamientos distintos,
	 * pero la pantalla siempre llama por POST.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * Nombre del usuario logueado, o cadena vacia si no hay sesion.
	 *
	 * @param request peticion
	 * @return el nombre de usuario, nunca null
	 */
	private String usuarioSesion(final HttpServletRequest request) {
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				final Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
				return (texto(usuarioSesion.getNombreUsuario()));
			}
		} catch (final Exception e) {
			System.out.println("DispersarPremiosRuleta, leyendo la sesion: " + e.toString());
		}
		return ("");
	}

	/**
	 * Respuesta de error con la misma forma que la exitosa, para que la pantalla
	 * no tenga que distinguir dos estructuras.
	 *
	 * @param mensaje texto a mostrar
	 * @return JSON de error
	 */
	private String error(final String mensaje) {
		return ("{\"resultado\":\"ERROR\",\"mensaje\":\"" + mensaje + "\",\"dispersados\":0,"
				+ "\"correos_enviados\":0,\"clientes_creados\":0,\"con_error\":0,"
				+ "\"pendientes_sin_procesar\":0,\"detalle_errores\":[]}");
	}

	/**
	 * Valida que la fecha tenga forma aaaa-mm-dd y sea real.
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
