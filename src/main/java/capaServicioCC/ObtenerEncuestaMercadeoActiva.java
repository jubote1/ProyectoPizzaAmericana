package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EncuestaMercadeoCtrl;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * EncuestaMercadeoCtrl.obtenerPreguntasVigentes.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerEncuestaMercadeoActiva   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): espuntoventa (string), idtienda (integer), idpedidotienda (integer).
 * Devuelve: application/json; claves: preguntas.
 * Lee: encuesta_mercadeo.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerEncuestaMercadeoActiva.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerEncuestaMercadeoActiva")
public class ObtenerEncuestaMercadeoActiva extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ObtenerEncuestaMercadeoActiva() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final String parametroPV = request.getParameter("espuntoventa");
		// Por defecto se asume punto de venta, que es el unico canal habilitado hoy
		final boolean esPuntoVenta = parametroPV == null || !parametroPV.equalsIgnoreCase("N");
		//El pedido llega para no volver a encuestar uno que ya se encuesto:
		//Finalizar se puede oprimir varias veces sobre el mismo pedido. Si no
		//vienen -la pantalla de configuracion no tiene pedido- se comporta como
		//siempre.
		int idTienda = 0;
		int idPedidoTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (final Exception e) {
			idTienda = 0;
		}
		try {
			idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
		} catch (final Exception e) {
			idPedidoTienda = 0;
		}
		final EncuestaMercadeoCtrl encCtrl = new EncuestaMercadeoCtrl();
		final String respuesta = encCtrl.obtenerPreguntasVigentes(esPuntoVenta, idTienda, idPedidoTienda);
		final PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
