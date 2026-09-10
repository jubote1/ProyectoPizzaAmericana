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
 * Registra una novedad de biometria. Lo llama el POS.
 *
 * La novedad NO cambia nada en la jornada: queda en estado REPORTADA esperando
 * que el supervisor la revise en el central. Por eso la pantalla del POS puede
 * ser abierta y cualquiera puede reportar: lo que se registra es una version de
 * lo que paso, no una correccion.
 *
 * Parametros:
 *   id            id del empleado al que le pasa la novedad. Obligatorio.
 *   fecha         dia de la jornada, aaaa-mm-dd. Obligatorio.
 *   idtienda      tienda donde se reporta. Obligatorio.
 *   tiponovedad   CORREGIR HORA, FALTA LA SALIDA, FALTA EL INGRESO u OTRA. Obligatorio.
 *   eventotipo    INGRESO o SALIDA del registro que la persona selecciono. Opcional.
 *   eventohora    aaaa-mm-dd hh:mm:ss de ese registro. Opcional.
 *   horareportada aaaa-mm-dd hh:mm:ss, la hora real segun quien reporta. Opcional.
 *   observacion   la explicacion. Obligatoria.
 *   reportadopor  usuario del POS que reporta. Obligatorio.
 *
 * Servlet implementation class InsertarNovedadBiometria
 */
@WebServlet("/InsertarNovedadBiometria")
public class InsertarNovedadBiometria extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final int MINIMO_OBSERVACION = 10;

	public InsertarNovedadBiometria() {
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
			final int idEmpleado = entero(request.getParameter("id"));
			final int idTienda = entero(request.getParameter("idtienda"));
			final String fecha = texto(request.getParameter("fecha"));
			final String tipoNovedad = texto(request.getParameter("tiponovedad")).toUpperCase();
			final String eventoTipo = texto(request.getParameter("eventotipo")).toUpperCase();
			final String eventoHora = texto(request.getParameter("eventohora"));
			final String horaReportada = texto(request.getParameter("horareportada"));
			final String observacion = texto(request.getParameter("observacion"));
			final String reportadoPor = texto(request.getParameter("reportadopor"));
			String origen = texto(request.getParameter("origen")).toUpperCase();
			if (origen.length() == 0) {
				origen = "POS";
			}

			if (idEmpleado <= 0 || idTienda <= 0) {
				out.write(ctrl.error("El id del empleado y la tienda son obligatorios."));
				return;
			}
			if (!fechaValida(fecha)) {
				out.write(ctrl.error("La fecha de la jornada es obligatoria, en formato aaaa-mm-dd."));
				return;
			}
			if (!tipoNovedadValido(tipoNovedad)) {
				out.write(ctrl.error("El tipo de novedad debe ser CORREGIR HORA, FALTA LA SALIDA, "
						+ "FALTA EL INGRESO u OTRA."));
				return;
			}
			if (reportadoPor.length() == 0) {
				out.write(ctrl.error("Hay que indicar quien reporta la novedad."));
				return;
			}
			if (observacion.length() < InsertarNovedadBiometria.MINIMO_OBSERVACION) {
				out.write(ctrl.error("La explicacion es obligatoria y debe tener al menos "
						+ InsertarNovedadBiometria.MINIMO_OBSERVACION + " caracteres."));
				return;
			}
			if (eventoTipo.length() > 0 && !"INGRESO".equals(eventoTipo)
					&& !"SALIDA".equals(eventoTipo)) {
				out.write(ctrl.error("El tipo del registro seleccionado debe ser INGRESO o SALIDA."));
				return;
			}
			if (eventoHora.length() > 0 && !fechaHoraValida(eventoHora)) {
				out.write(ctrl.error("La hora del registro seleccionado va en aaaa-mm-dd hh:mm:ss."));
				return;
			}
			if (horaReportada.length() > 0 && !fechaHoraValida(horaReportada)) {
				out.write(ctrl.error("La hora real debe ir en formato aaaa-mm-dd hh:mm:ss."));
				return;
			}
			//Si dice la hora real, tiene que ser del mismo dia de la jornada.
			if (horaReportada.length() > 0 && !horaReportada.substring(0, 10).equals(fecha)) {
				out.write(ctrl.error("La hora real tiene que ser del mismo dia de la jornada ("
						+ fecha + ")."));
				return;
			}
			out.write(ctrl.insertarNovedad(idEmpleado, fecha, idTienda, eventoTipo, eventoHora,
					tipoNovedad, horaReportada, observacion, reportadoPor, origen));
		} catch (final Exception e) {
			System.out.println("InsertarNovedadBiometria: " + e.toString());
			out.write(ctrl.error("Error registrando la novedad."));
		}
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	private boolean tipoNovedadValido(final String tipo) {
		return ("CORREGIR HORA".equals(tipo) || "FALTA LA SALIDA".equals(tipo)
				|| "FALTA EL INGRESO".equals(tipo) || "OTRA".equals(tipo));
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
