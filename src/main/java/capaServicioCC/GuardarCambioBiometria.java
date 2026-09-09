package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.BiometriaNovedadCtrl;

/**
 * Aplica UN cambio a la jornada de un empleado y lo deja registrado en el log.
 *
 * Un cambio por llamada a proposito: cada uno es atomico -o queda el cambio y
 * su fila de log, o no queda nada- y la pantalla llama tantas veces como
 * cambios haya hecho el supervisor. Si uno falla, los anteriores ya quedaron
 * registrados y se sabe exactamente donde se quedo.
 *
 * OJO: aqui se escribe sobre general.empleado_evento, que es de donde sale el
 * calculo de horas. No hay autenticacion todavia; el usuario que llega en el
 * parametro es el que queda en el log.
 *
 * Parametros:
 *   idnovedad     novedad que motiva el cambio. 0 si el supervisor corrige por
 *                 su cuenta algo que nadie reporto.
 *   id            id del empleado. Obligatorio.
 *   fecha         dia de la jornada, aaaa-mm-dd. Obligatorio.
 *   idtienda      tienda del evento. Obligatorio.
 *   accion        MODIFICA, AGREGA o ELIMINA. Obligatorio.
 *   antestipo     INGRESO o SALIDA del evento original. Para MODIFICA y ELIMINA.
 *   anteshora     aaaa-mm-dd hh:mm:ss del evento original. Para MODIFICA y ELIMINA.
 *   despuestipo   INGRESO o SALIDA del evento nuevo. Para MODIFICA y AGREGA.
 *   despueshora   aaaa-mm-dd hh:mm:ss del evento nuevo. Para MODIFICA y AGREGA.
 *   usuario       quien hace el cambio. Obligatorio.
 *   observacion   por que se hizo. Obligatoria.
 *
 * Servlet implementation class GuardarCambioBiometria
 */
@WebServlet("/GuardarCambioBiometria")
public class GuardarCambioBiometria extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Minimo de caracteres de la observacion. Un cambio de horas sin explicacion no sirve de log. */
	private static final int MINIMO_OBSERVACION = 10;

	public GuardarCambioBiometria() {
		super();
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		final BiometriaNovedadCtrl ctrl = new BiometriaNovedadCtrl();
		try {
			final int idNovedad = entero(request.getParameter("idnovedad"));
			final int idEmpleado = entero(request.getParameter("id"));
			final int idTienda = entero(request.getParameter("idtienda"));
			final String fecha = texto(request.getParameter("fecha"));
			final String accion = texto(request.getParameter("accion")).toUpperCase();
			final String antesTipo = texto(request.getParameter("antestipo")).toUpperCase();
			final String antesHora = texto(request.getParameter("anteshora"));
			final String despuesTipo = texto(request.getParameter("despuestipo")).toUpperCase();
			final String despuesHora = texto(request.getParameter("despueshora"));
			final String usuario = texto(request.getParameter("usuario"));
			final String observacion = texto(request.getParameter("observacion"));

			if (idEmpleado <= 0 || idTienda <= 0) {
				out.write(ctrl.error("El id del empleado y la tienda son obligatorios."));
				return;
			}
			if (!fechaValida(fecha)) {
				out.write(ctrl.error("La fecha de la jornada es obligatoria, en formato aaaa-mm-dd."));
				return;
			}
			if (usuario.length() == 0) {
				out.write(ctrl.error("Hay que indicar quien hace el cambio."));
				return;
			}
			if (observacion.length() < GuardarCambioBiometria.MINIMO_OBSERVACION) {
				out.write(ctrl.error("La observacion es obligatoria y debe tener al menos "
						+ GuardarCambioBiometria.MINIMO_OBSERVACION + " caracteres."));
				return;
			}
			if (!"MODIFICA".equals(accion) && !"AGREGA".equals(accion) && !"ELIMINA".equals(accion)) {
				out.write(ctrl.error("La accion debe ser MODIFICA, AGREGA o ELIMINA."));
				return;
			}
			final boolean tocaBorrar = "MODIFICA".equals(accion) || "ELIMINA".equals(accion);
			final boolean tocaInsertar = "MODIFICA".equals(accion) || "AGREGA".equals(accion);
			if (tocaBorrar && (!tipoValido(antesTipo) || !fechaHoraValida(antesHora))) {
				out.write(ctrl.error("Para " + accion + " hay que indicar el evento original, "
						+ "con tipo INGRESO o SALIDA y hora aaaa-mm-dd hh:mm:ss."));
				return;
			}
			if (tocaInsertar && (!tipoValido(despuesTipo) || !fechaHoraValida(despuesHora))) {
				out.write(ctrl.error("Para " + accion + " hay que indicar el evento nuevo, "
						+ "con tipo INGRESO o SALIDA y hora aaaa-mm-dd hh:mm:ss."));
				return;
			}
			//La hora nueva tiene que caer en el mismo dia de la jornada: si no,
			//el evento quedaria en otra fecha mientras la novedad dice otra cosa.
			if (tocaInsertar && !despuesHora.substring(0, 10).equals(fecha)) {
				out.write(ctrl.error("La hora nueva tiene que ser del mismo dia de la jornada ("
						+ fecha + ")."));
				return;
			}
			out.write(ctrl.guardarCambio(idNovedad, idEmpleado, fecha, idTienda, accion, antesTipo,
					antesHora, despuesTipo, despuesHora, usuario, observacion));
		} catch (final Exception e) {
			System.out.println("GuardarCambioBiometria: " + e.toString());
			out.write(ctrl.error("Error aplicando el cambio."));
		}
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	private boolean tipoValido(final String tipo) {
		return ("INGRESO".equals(tipo) || "SALIDA".equals(tipo));
	}

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

	private boolean fechaHoraValida(final String fechaHora) {
		if (fechaHora == null || fechaHora.length() != 19) {
			return (false);
		}
		try {
			java.time.LocalDateTime.parse(fechaHora.replace(' ', 'T'));
			return (true);
		} catch (final Exception e) {
			return (false);
		}
	}

	private int entero(final String valor) {
		try {
			return (Integer.parseInt(texto(valor)));
		} catch (final Exception e) {
			return (0);
		}
	}

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
