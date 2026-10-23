package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.Campana15MinCtrl;

/**
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca
 * Campana15MinCtrl.registrarFormaPago.
 *
 * Ruta: /ProyectoPizzaAmericana/RegistrarCampana15MinFormaPago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer), esmediovirtual (string).
 * Devuelve: application/json; claves: respuesta.
 * Escribe: campana_15min_aplicado (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RegistrarCampana15MinFormaPago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RegistrarCampana15MinFormaPago")
public class RegistrarCampana15MinFormaPago extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinFormaPago() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			int idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
			boolean esMedioVirtual = "S".equalsIgnoreCase(request.getParameter("esmediovirtual"));
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarFormaPago(idPedidoTienda, idTienda, esMedioVirtual));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
