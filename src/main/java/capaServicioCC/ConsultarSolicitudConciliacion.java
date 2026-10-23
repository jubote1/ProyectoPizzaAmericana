package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Pagos virtuales y conciliacion: invoca
 * PedidoCtrl.consultarSolicitudConciliacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarSolicitudConciliacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fecha (string), fechahasta (string), estado (string), idtienda (integer).
 * Devuelve: application/json; charset=UTF-8; claves: categoria, descripcion, dias, dias_mas_vieja,
 * estado, fecha, fecha_procesa, idpedidotienda, idsolicitud, idtienda, nombretienda,
 * observacion_cierre, ....
 * Lee: solicitud_conciliacion, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarSolicitudConciliacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarSolicitudConciliacion")
public class ConsultarSolicitudConciliacion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ConsultarSolicitudConciliacion() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		final String fechaDesde = aFechaSql(request.getParameter("fecha"));
		final String fechaHasta = aFechaSql(request.getParameter("fechahasta"));
		final String estado = request.getParameter("estado") == null ? ""
				: request.getParameter("estado").trim();

		//TODAS, vacio o algo que no sea un numero significan todas las tiendas.
		//Antes cualquiera de esos casos terminaba en "idtienda = 0", que no es
		//ninguna tienda y devolvia cero filas.
		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (final Exception e) {
			idTienda = 0;
		}

		final String respuesta = PedidoCtrl.consultarSolicitudConciliacion(idTienda, fechaDesde,
				fechaHasta, estado);
		final PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/**
	 * De dd/MM/yyyy a yyyy-MM-dd, que es lo que entiende MySQL.
	 *
	 * Si viene vacia o no se entiende devuelve vacio en vez de inventarse la
	 * fecha de hoy: antes, ante una fecha ilegible, se usaba new Date() y la
	 * pantalla mostraba el resultado de una consulta que nadie pidio.
	 */
	private String aFechaSql(final String fecha) {
		if (fecha == null || fecha.trim().length() == 0) {
			return ("");
		}
		final String limpia = fecha.trim();
		//Si ya viene en formato SQL se deja como esta.
		if (limpia.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			return (limpia);
		}
		try {
			final SimpleDateFormat origen = new SimpleDateFormat("dd/MM/yyyy");
			origen.setLenient(false);
			final Date convertida = origen.parse(limpia);
			return (new SimpleDateFormat("yyyy-MM-dd").format(convertida));
		} catch (final ParseException e) {
			System.out.println("ConsultarSolicitudConciliacion: fecha ilegible '" + limpia + "'");
			return ("");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
