package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.TableroOfertasCtrl;
import utilidadesCC.AccesoCRM;

/**
 * Servicio de consulta de Ofertas y fidelizacion (administracion): invoca
 * AccesoCRM.usuarioEnSesion, TableroOfertasCtrl.tablero.
 *
 * Ruta: /ProyectoPizzaAmericana/TableroOfertas   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): dias (integer).
 * Devuelve: application/json; charset=UTF-8; claves: alertas, anulados, cantidad, descontado,
 * dias, emitidos, error, explicacion, habilitada, idoferta, idtienda, nivel, ....
 * Lee: log_redencion_codigo, oferta, oferta_cliente, pedido, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha TableroOfertas.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/TableroOfertas")
public class TableroOfertas extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		if (AccesoCRM.usuarioEnSesion(request) == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write("{\"error\":\"Debe iniciar sesion.\"}");
			return;
		}
		int dias = 30;
		try {
			dias = Integer.parseInt(request.getParameter("dias").trim());
		} catch (final Exception e) {
			dias = 30;
		}
		out.write(TableroOfertasCtrl.tablero(dias));
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
