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

import capaControladorCC.TableroPuntosCtrl;
import capaModeloCC.Usuario;

/**
 * Tablero del plan de puntos (Puntos Pizza): afiliados, puntos asignados, vigentes,
 * por vencer, redenciones, y rankings de clientes y empleados.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarTableroPuntos   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: RESTRINGIDO a los roles "Tablero de Puntos", "Auditoria de Fidelizacion" o
 *       "Superadministrador". Los tres ven todo, incluidos los rankings de EMPLEADOS
 *       (EMPLEADOS_REDIMEN y EMPLEADOS_ACUMULAN). La regla vive en TableroPuntosCtrl.puedeVerEmpleados.
 * Recibe (query/form): accion (RESUMEN por defecto, RANKING, PORVENCER o CSVPORVENCER),
 *                      fechainicial, fechafinal (aaaa-mm-dd; sin ellas, los ultimos 30 dias),
 *                      diasactivo (90 por defecto), tipo (CLIENTES_SALDO | CLIENTES_REDIMEN |
 *                      EMPLEADOS_REDIMEN | EMPLEADOS_ACUMULAN), idtienda (0 = todas),
 *                      limite, dias (15 o 60, para PORVENCER y CSVPORVENCER).
 * Devuelve: application/json con resultado OK o ERROR (mensaje); CSVPORVENCER devuelve text/csv.
 * Lee: fidelizacion_transaccion, fidelizacion_redencion, cliente_fidelizacion, tienda,
 *      general.empleado, pizzaamericana.usuario, crm.persona_resumen. No escribe nada.
 *
 * La restriccion de acceso se revisa AQUI, leyendo el rol de la base en cada peticion:
 * SeguridadFilter esta en modo LOG (audita pero no bloquea), asi que esconder la opcion del menu
 * no impide que alguien pida la URL a mano. Mismo criterio de ConsultarAuditoriaFidelizacion.
 */
@WebServlet("/ConsultarTableroPuntos")
public class ConsultarTableroPuntos extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Mas que esto es una descarga, no una revision. */
	private static final int MAXIMO_DIAS = 366;

	public ConsultarTableroPuntos() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		final String accion = texto(request.getParameter("accion")).toUpperCase();
		final boolean csv = "CSVPORVENCER".equals(accion);
		response.setCharacterEncoding("UTF-8");
		if (csv) {
			response.setContentType("text/csv");
		} else {
			response.setContentType("application/json");
		}
		final PrintWriter out = response.getWriter();
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion == null || !(sesion.getAttribute("usuario") instanceof Usuario)) {
				out.write(error("Sesion no valida. Vuelva a ingresar."));
				return;
			}
			final Usuario usuario = (Usuario) sesion.getAttribute("usuario");
			if (!TableroPuntosCtrl.puedeVer(usuario.getId())) {
				//Se responde lo mismo que si no existiera.
				out.write(error("No tiene permiso para ver esta informacion."));
				return;
			}

			final int idTienda = entero(request.getParameter("idtienda"));

			if ("PORVENCER".equals(accion) || csv) {
				final int dias = entero(request.getParameter("dias"));
				if (dias < 1 || dias > 365) {
					out.write(error("Los dias deben estar entre 1 y 365."));
					return;
				}
				if (csv) {
					response.setHeader("Content-Disposition",
							"attachment; filename=\"puntos_por_vencer_" + dias + "_dias.csv\"");
					out.write(TableroPuntosCtrl.csvPorVencer(dias, idTienda));
					return;
				}
				out.write(TableroPuntosCtrl.porVencer(dias, idTienda, entero(request.getParameter("limite")))
						.toJSONString());
				return;
			}

			//RESUMEN y RANKING comparten el rango de fechas. Sin fechas, los ultimos 30 dias.
			String desde = texto(request.getParameter("fechainicial"));
			String hasta = texto(request.getParameter("fechafinal"));
			if (desde.length() == 0 || hasta.length() == 0) {
				final String[] r = TableroPuntosCtrl.ultimos30Dias();
				desde = r[0];
				hasta = r[1];
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

			if ("RANKING".equals(accion)) {
				final String tipo = texto(request.getParameter("tipo")).toUpperCase();
				//Los rankings de empleados senalan personas de la nomina: solo la auditoria
				//y el Superadministrador. El rol "Tablero de Puntos" no los ve.
				if (tipo.startsWith("EMPLEADOS") && !TableroPuntosCtrl.puedeVerEmpleados(usuario.getId())) {
					out.write(error("No tiene permiso para ver los rankings de empleados."));
					return;
				}
				final JSONObject r = TableroPuntosCtrl.ranking(tipo, desde, hasta, idTienda,
						entero(request.getParameter("limite")));
				out.write(r.toJSONString());
				return;
			}

			final JSONObject r = TableroPuntosCtrl.resumen(entero(request.getParameter("diasactivo")), desde, hasta);
			//La pantalla esconde las consultas de empleados a quien no puede verlas.
			r.put("puede_empleados", Boolean.valueOf(TableroPuntosCtrl.puedeVerEmpleados(usuario.getId())));
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
