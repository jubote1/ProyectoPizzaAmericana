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
 * Servicio de consulta de Tiendas y operacion: invoca BiometriaNovedadCtrl.error,
 * BiometriaNovedadCtrl.maximoDiasRango.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarNovedadesBiometria   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechadesde (string), fechahasta (string), estado (string).
 * Devuelve: application/json; claves: atendidas, cambios, detalle, estado, eventohora, eventotipo,
 * fecha, fechareporte, fecharesolucion, horareportada, id, idnovedad, ....
 * Lee: general.empleado, general.empleado_evento_log, general.empleado_evento_novedad.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarNovedadesBiometria.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarNovedadesBiometria")
public class ConsultarNovedadesBiometria extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarNovedadesBiometria() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		final BiometriaNovedadCtrl ctrl = new BiometriaNovedadCtrl();
		try {
			final String fechaDesde = texto(request.getParameter("fechadesde"));
			final String fechaHasta = texto(request.getParameter("fechahasta"));
			final String estado = texto(request.getParameter("estado"));

			if (!fechaValida(fechaDesde) || !fechaValida(fechaHasta)) {
				out.write(ctrl.error("fechadesde y fechahasta son obligatorias, en formato aaaa-mm-dd."));
				return;
			}
			if (fechaDesde.compareTo(fechaHasta) > 0) {
				out.write(ctrl.error("La fecha inicial no puede ser mayor que la final."));
				return;
			}
			if (diasEntre(fechaDesde, fechaHasta) > ctrl.maximoDiasRango()) {
				out.write(ctrl.error("El rango no puede pasar de " + ctrl.maximoDiasRango() + " dias."));
				return;
			}
			if (estado.length() > 0 && !"REPORTADA".equals(estado) && !"ATENDIDA".equals(estado)
					&& !"RECHAZADA".equals(estado)) {
				out.write(ctrl.error("El estado debe ser REPORTADA, ATENDIDA o RECHAZADA."));
				return;
			}
			out.write(ctrl.consultarNovedades(fechaDesde, fechaHasta, estado));
		} catch (final Exception e) {
			System.out.println("ConsultarNovedadesBiometria: " + e.toString());
			out.write(ctrl.error("Error consultando las novedades de biometria."));
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * Valida que la fecha tenga forma aaaa-mm-dd y sea real. Se valida aqui y no
	 * solo en la pantalla porque el servicio se puede llamar directo por URL.
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

	private long diasEntre(final String desde, final String hasta) {
		return (java.time.temporal.ChronoUnit.DAYS.between(
				java.time.LocalDate.parse(desde), java.time.LocalDate.parse(hasta)));
	}

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
