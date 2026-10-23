package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CodigoPromoCtrl;
import utilidadesCC.LimitadorIntentos;

/**
 * Servicio que modifica datos de Ofertas y fidelizacion (administracion): invoca
 * CodigoPromoCtrl.autorizado, CodigoPromoCtrl.sinAutorizacion.
 *
 * Ruta: /ProyectoPizzaAmericana/CodigoPromocional   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Exige token de tienda.
 * Recibe (query/form): accion (string), idtienda (string), codigo (string), usuario (string),
 * total (string), esdomicilio (string), token (string), idpedido (string), descuento (string),
 * origen (string).
 * Devuelve: application/json; charset=UTF-8; claves: codigo, descuentopesos, fechacaducidad,
 * idoferta, idofertacliente, mensaje, nombreoferta, redparcial, respuesta, saldoposterior,
 * tipooferta, token.
 * Lee: log_redencion_codigo, oferta, oferta_cliente, parametros.
 * Escribe: log_redencion_codigo (INSERT), oferta_cliente (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CodigoPromocional.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CodigoPromocional")
public class CodigoPromocional extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (!CodigoPromoCtrl.autorizado(request.getHeader("X-Token-Tienda"))) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write(CodigoPromoCtrl.sinAutorizacion());
			return;
		}
		final String accion = texto(request.getParameter("accion")).toLowerCase();
		final int idTienda = entero(request.getParameter("idtienda"));
		final String origen = request.getRemoteAddr() + "|" + idTienda;

		if ("reservar".equals(accion)) {
			if (LimitadorIntentos.bloqueado(origen)) {
				out.write(CodigoPromoCtrl.bloqueado());
				return;
			}
			final String respuesta = CodigoPromoCtrl.reservar(texto(request.getParameter("codigo")), idTienda,
					texto(request.getParameter("usuario")), decimal(request.getParameter("total")),
					"S".equalsIgnoreCase(texto(request.getParameter("esdomicilio"))),
					texto(request.getParameter("token")));
			if (respuesta.contains("\"respuesta\":\"NOK\"")) {
				LimitadorIntentos.registrarFallo(origen);
			}
			out.write(respuesta);

		} else if ("confirmar".equals(accion)) {
			out.write(CodigoPromoCtrl.confirmar(texto(request.getParameter("token")),
					entero(request.getParameter("idpedido")), idTienda, texto(request.getParameter("usuario")),
					decimal(request.getParameter("descuento")), texto(request.getParameter("origen"))));

		} else if ("liberar".equals(accion)) {
			out.write(CodigoPromoCtrl.liberar(texto(request.getParameter("token"))));

		} else {
			out.write("{\"respuesta\":\"ERROR\",\"mensaje\":\"Accion no reconocida.\"}");
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
