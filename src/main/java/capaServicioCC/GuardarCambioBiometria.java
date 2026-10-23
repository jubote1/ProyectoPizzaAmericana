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
 * Servicio que modifica datos de Tiendas y operacion: invoca BiometriaNovedadCtrl.error,
 * BiometriaNovedadCtrl.guardarCambio.
 *
 * Ruta: /ProyectoPizzaAmericana/GuardarCambioBiometria   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idnovedad (string), id (string), idtienda (string), fecha (string), accion
 * (string), antestipo (string), anteshora (string), despuestipo (string), despueshora (string),
 * usuario (string), observacion (string).
 * Devuelve: application/json; claves: detalle, mensaje, resultado.
 * Lee: general.empleado_evento.
 * Escribe: general.empleado_evento (DELETE/INSERT), general.empleado_evento_log (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GuardarCambioBiometria.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
