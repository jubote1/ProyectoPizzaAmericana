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
 * BiometriaNovedadCtrl.cerrarNovedad.
 *
 * Ruta: /ProyectoPizzaAmericana/CerrarNovedadBiometria   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idnovedad (string), estado (string), usuario (string), observacion
 * (string).
 * Devuelve: application/json; claves: detalle, mensaje, resultado.
 * Escribe: general.empleado_evento_novedad (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CerrarNovedadBiometria.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CerrarNovedadBiometria")
public class CerrarNovedadBiometria extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final int MINIMO_OBSERVACION = 10;

	public CerrarNovedadBiometria() {
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
			int idNovedad = 0;
			try {
				idNovedad = Integer.parseInt(texto(request.getParameter("idnovedad")));
			} catch (final Exception e) {
				idNovedad = 0;
			}
			final String estado = texto(request.getParameter("estado")).toUpperCase();
			final String usuario = texto(request.getParameter("usuario"));
			final String observacion = texto(request.getParameter("observacion"));

			if (idNovedad <= 0) {
				out.write(ctrl.error("El id de la novedad es obligatorio."));
				return;
			}
			if (!"ATENDIDA".equals(estado) && !"RECHAZADA".equals(estado)) {
				out.write(ctrl.error("El estado debe ser ATENDIDA o RECHAZADA."));
				return;
			}
			if (usuario.length() == 0) {
				out.write(ctrl.error("Hay que indicar quien resuelve la novedad."));
				return;
			}
			if (observacion.length() < CerrarNovedadBiometria.MINIMO_OBSERVACION) {
				out.write(ctrl.error("La observacion es obligatoria y debe tener al menos "
						+ CerrarNovedadBiometria.MINIMO_OBSERVACION + " caracteres."));
				return;
			}
			out.write(ctrl.cerrarNovedad(idNovedad, estado, usuario, observacion));
		} catch (final Exception e) {
			System.out.println("CerrarNovedadBiometria: " + e.toString());
			out.write(ctrl.error("Error cerrando la novedad."));
		}
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	private String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}
}
