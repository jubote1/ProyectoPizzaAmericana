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
 * Servlet implementation class RegistrarEmpleadoTemporalDia
 *
 * El POS de cada tienda le avisa al central que un domiciliario temporal ingreso o salio, para que el mapa
 * de domiciliarios sepa si esta dentro. Se llama despues de guardarlo en la base local de la tienda; si
 * este servicio no responde, la tienda sigue trabajando y lo reintenta mas tarde.
 *
 * POST con un JSON: idtienda, idinterno, id, identificacion, nombre, telefono, empresa, idempresa,
 * fechasistema (aaaa-mm-dd), horaingreso, horasalida, observacion, anulado (S/N) y version.
 *
 * AUTORIZACION: el mismo token de las tiendas que usa CodigoPromocional. Si general.parametros.CODIGOSTOKEN
 * tiene valor, cada llamada debe traerlo en el encabezado X-Token-Tienda; si esta vacio no se exige nada, para
 * poder desplegar primero el central y despues el POS.
 *
 * Ver capaControladorCC.EmpleadoTemporalDiaTiendaCtrl y sql/2026_10_07_01_general_empleado_temporal_dia_tienda.sql.
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
