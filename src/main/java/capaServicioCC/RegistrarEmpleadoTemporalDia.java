package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CodigoPromoCtrl;
import capaControladorCC.EmpleadoTemporalDiaTiendaCtrl;

/**
 * Servicio que modifica datos de Tiendas y operacion: invoca CodigoPromoCtrl.autorizado,
 * CodigoPromoCtrl.sinAutorizacion.
 *
 * Ruta: /ProyectoPizzaAmericana/RegistrarEmpleadoTemporalDia   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Exige token de tienda.
 * Recibe: cuerpo de la peticion y cabecera X-Token-Tienda.
 * Devuelve: application/json; charset=UTF-8; claves: mensaje, respuesta.
 * Lee: parametros.
 * Escribe: empleado_temporal_dia_tienda (INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RegistrarEmpleadoTemporalDia.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RegistrarEmpleadoTemporalDia")
public class RegistrarEmpleadoTemporalDia extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** Un registro mide menos de 1 KB; el tope evita que alguien llene la memoria con un cuerpo enorme. */
	private static final int MAXIMO_CUERPO = 8192;

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (!CodigoPromoCtrl.autorizado(request.getHeader("X-Token-Tienda"))) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write(CodigoPromoCtrl.sinAutorizacion());
			return;
		}
		final StringBuilder cuerpo = new StringBuilder();
		try (BufferedReader lector = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"))) {
			final char[] buffer = new char[1024];
			int leidos;
			while ((leidos = lector.read(buffer)) != -1) {
				cuerpo.append(buffer, 0, leidos);
				if (cuerpo.length() > MAXIMO_CUERPO) {
					response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
					out.write("{\"respuesta\":\"INVALIDO\",\"mensaje\":\"Cuerpo demasiado grande.\"}");
					return;
				}
			}
		}
		out.write(EmpleadoTemporalDiaTiendaCtrl.registrar(cuerpo.toString()));
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.setContentType("application/json; charset=UTF-8");
		response.getWriter().write("{\"respuesta\":\"INVALIDO\",\"mensaje\":\"Use POST.\"}");
	}
}
