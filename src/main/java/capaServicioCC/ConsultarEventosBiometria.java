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
 * BiometriaNovedadCtrl.consultarEventosDelDia.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarEventosBiometria   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fecha (string), id (string).
 * Devuelve: application/json; claves: detalle, fecha, fechahora, idtienda, mensaje, resultado,
 * tipo, usobiometria.
 * Lee: general.empleado_evento.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarEventosBiometria.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarEventosBiometria")
public class ConsultarEventosBiometria extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarEventosBiometria() {
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
			final String fecha = texto(request.getParameter("fecha"));
			int idEmpleado = 0;
			try {
				idEmpleado = Integer.parseInt(texto(request.getParameter("id")));
			} catch (final Exception e) {
				idEmpleado = 0;
			}
			if (idEmpleado <= 0) {
				out.write(ctrl.error("El id del empleado es obligatorio."));
				return;
			}
			if (!fechaValida(fecha)) {
				out.write(ctrl.error("La fecha es obligatoria, en formato aaaa-mm-dd."));
				return;
			}
			out.write(ctrl.consultarEventosDelDia(idEmpleado, fecha));
		} catch (final Exception e) {
			System.out.println("ConsultarEventosBiometria: " + e.toString());
			out.write(ctrl.error("Error consultando la jornada del empleado."));
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
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

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
