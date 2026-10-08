package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.OfertaReglasCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Reglas de uso de una oferta y su bitacora.
 *
 *   accion=obtener   idoferta
 *   accion=guardar   idoferta, montominimo, topedescuento, tiendas, aplicaa, maxusoscliente, maxemision
 *   accion=bitacora  idoferta
 *
 * A diferencia de CRUDOferta, pide sesion: cambiar las reglas de una oferta viva cambia lo que las tiendas
 * aceptan como descuento, y tiene que quedar a nombre de alguien.
 */
@WebServlet("/OfertaReglas")
public class OfertaReglas extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final String usuario = AccesoCRM.usuarioEnSesion(request);
		if (usuario == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write("{\"error\":\"Debe iniciar sesion.\"}");
			return;
		}
		final String accion = texto(request.getParameter("accion"));
		final int idOferta = entero(request.getParameter("idoferta"));
		if (idOferta <= 0) {
			out.write("{\"error\":\"Falta la oferta.\"}");
			return;
		}
		if ("obtener".equals(accion)) {
			out.write(OfertaReglasCtrl.obtener(idOferta));
		} else if ("guardar".equals(accion)) {
			out.write(OfertaReglasCtrl.guardar(idOferta, decimal(request.getParameter("montominimo")),
					decimal(request.getParameter("topedescuento")), texto(request.getParameter("tiendas")),
					texto(request.getParameter("aplicaa")), entero(request.getParameter("maxusoscliente")),
					entero(request.getParameter("maxemision")), usuario));
		} else if ("bitacora".equals(accion)) {
			out.write(OfertaReglasCtrl.bitacora(idOferta));
		} else {
			out.write("{\"error\":\"Accion no reconocida.\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private static String texto(final String v) {
		return v == null ? "" : v.trim();
	}

	private static int entero(final String v) {
		try {
			return Integer.parseInt(v.trim());
		} catch (final Exception e) {
			return 0;
		}
	}

	private static double decimal(final String v) {
		try {
			return Double.parseDouble(v.trim());
		} catch (final Exception e) {
			return 0;
		}
	}
}
