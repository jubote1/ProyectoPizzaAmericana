package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import capaControladorCC.AuditoriaFidelizacionCtrl;
import capaModeloCC.Usuario;

/**
 * Auditoria de fidelizacion: quien redimio puntos y el historial de cada
 * persona. Invoca AuditoriaFidelizacionCtrl.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarAuditoriaFidelizacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: RESTRINGIDO al rol "Auditoria de Fidelizacion" o "Superadministrador".
 * Recibe (query/form): accion (SEMANA por defecto, ACUMULADORES o CLIENTE), fechainicial,
 *                      fechafinal, idtienda (0 = todas), senal (TODAS | EMPLEADO | AUTO |
 *                      REVERSADA), correo, minimopedidos.
 * Devuelve: application/json; para SEMANA claves resultado, desde, hasta, totales, redenciones;
 *           para ACUMULADORES resultado, desde, hasta, totales, acumuladores;
 *           para CLIENTE resultado, resumen, atienden, acumulaciones, redenciones;
 *           o resultado = ERROR con mensaje.
 * Lee: fidelizacion_redencion, fidelizacion_transaccion, cliente_fidelizacion, tienda,
 *      general.empleado, pizzaamericana.usuario, crm.persona_resumen.
 *
 * POR QUE LA RESTRICCION VA AQUI Y NO EN EL FILTRO
 *
 * general.parametros.SEGURIDADFILTROMODO esta en LOG: SeguridadFilter audita
 * pero deja pasar todo. Si este servlet se confiara de el, cualquiera con
 * sesion podria pedir la URL a mano y ver quien redime puntos y cuanto gasta,
 * que son datos de clientes y senalamientos sobre empleados.
 *
 * El rol se lee de la base en cada peticion, no de la sesion: el objeto Usuario
 * se congela al ingresar y quitarle el rol a alguien no tendria efecto hasta
 * que cerrara sesion.
 */
@WebServlet("/ConsultarAuditoriaFidelizacion")
public class ConsultarAuditoriaFidelizacion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Cuantos dias se dejan pedir de una. Mas que esto es una descarga, no una revision. */
	private static final int MAXIMO_DIAS = 186;

	public ConsultarAuditoriaFidelizacion() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion == null || !(sesion.getAttribute("usuario") instanceof Usuario)) {
				out.write(error("Sesion no valida. Vuelva a ingresar."));
				return;
			}
			final Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			if (!AuditoriaFidelizacionCtrl.puedeVer(usuario.getId())) {
				//Se responde lo mismo que si no existiera: no hay por que
				//confirmarle a quien no debe entrar que la pantalla existe.
				out.write(error("No tiene permiso para ver esta informacion."));
				return;
			}

			final String accion = texto(request.getParameter("accion")).toUpperCase();

			if ("CLIENTE".equals(accion)) {
				final String correo = texto(request.getParameter("correo"));
				if (correo.length() == 0) {
					out.write(error("Falta el correo de la persona."));
					return;
				}
				final JSONObject r = AuditoriaFidelizacionCtrl.consultarCliente(correo);
				out.write(r.toJSONString());
				return;
			}

			//SEMANA y ACUMULADORES comparten el rango de fechas; CLIENTE no lo usa.
			//Sin fechas, la semana pasada de lunes a domingo.
			String desde = texto(request.getParameter("fechainicial"));
			String hasta = texto(request.getParameter("fechafinal"));
			if (desde.length() == 0 || hasta.length() == 0) {
				final String[] semana = AuditoriaFidelizacionCtrl.semanaPasada();
				desde = semana[0];
				hasta = semana[1];
			}
			if (!esFecha(desde) || !esFecha(hasta)) {
				out.write(error("Las fechas deben venir en formato aaaa-mm-dd."));
				return;
			}
			if (desde.compareTo(hasta) > 0) {
				out.write(error("La fecha inicial es posterior a la final."));
				return;
			}
			if (dias(desde, hasta) > MAXIMO_DIAS) {
				out.write(error("El rango no puede pasar de " + MAXIMO_DIAS + " dias."));
				return;
			}

			if ("ACUMULADORES".equals(accion)) {
				final JSONObject r = AuditoriaFidelizacionCtrl.consultarAcumuladores(desde, hasta,
						entero(request.getParameter("idtienda")),
						entero(request.getParameter("minimopedidos")));
				out.write(r.toJSONString());
				return;
			}

			final JSONObject r = AuditoriaFidelizacionCtrl.consultarSemana(desde, hasta,
					entero(request.getParameter("idtienda")), senal(request.getParameter("senal")));
			out.write(r.toJSONString());

		} catch (final Exception e) {
			out.write(error("No se pudo consultar: " + e.toString()));
		} finally {
			out.flush();
			out.close();
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/** Solo se aceptan las senales conocidas; cualquier otra cosa es "todas". */
	private static String senal(final String valor) {
		final String s = texto(valor).toUpperCase();
		if ("EMPLEADO".equals(s) || "AUTO".equals(s) || "REVERSADA".equals(s)) {
			return (s);
		}
		return ("TODAS");
	}

	private static boolean esFecha(final String valor) {
		if (valor == null || valor.length() != 10) {
			return (false);
		}
		try {
			java.time.LocalDate.parse(valor);
			return (true);
		} catch (final Exception e) {
			return (false);
		}
	}

	private static long dias(final String desde, final String hasta) {
		try {
			return (java.time.temporal.ChronoUnit.DAYS.between(
					java.time.LocalDate.parse(desde), java.time.LocalDate.parse(hasta)));
		} catch (final Exception e) {
			return (0);
		}
	}

	private static String error(final String mensaje) {
		return ("{\"resultado\":\"ERROR\",\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}");
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}

	private static int entero(final String valor) {
		try {
			return (Integer.parseInt(texto(valor)));
		} catch (final Exception e) {
			return (0);
		}
	}
}
