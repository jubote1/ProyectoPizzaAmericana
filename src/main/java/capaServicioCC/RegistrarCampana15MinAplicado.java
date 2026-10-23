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
 * Campana15MinCtrl.registrarAplicado.
 *
 * Ruta: /ProyectoPizzaAmericana/RegistrarCampana15MinAplicado   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer), idcampana (integer),
 * fechahorainicio (string), valorbasepizza (number), nombrecliente (string), celularcliente
 * (string).
 * Devuelve: application/json; claves: respuesta.
 * Escribe: campana_15min_aplicado (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RegistrarCampana15MinAplicado.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RegistrarCampana15MinAplicado")
public class RegistrarCampana15MinAplicado extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public RegistrarCampana15MinAplicado() {
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
			int idCampana = Integer.parseInt(request.getParameter("idcampana"));
			String fechaHoraInicio = request.getParameter("fechahorainicio");
			double valorBasePizza = Double.parseDouble(request.getParameter("valorbasepizza"));
			String nombreCliente = request.getParameter("nombrecliente");
			String celularCliente = request.getParameter("celularcliente");
			final Campana15MinCtrl ctrl = new Campana15MinCtrl();
			out.write(ctrl.registrarAplicado(idPedidoTienda, idTienda, idCampana, fechaHoraInicio, valorBasePizza,
					nombreCliente, celularCliente));
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
