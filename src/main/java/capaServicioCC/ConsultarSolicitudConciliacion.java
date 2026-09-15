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
 * Las solicitudes de conciliacion de QR y datafono.
 *
 * Antes recibia solo la fecha desde y el id de tienda, y hacia
 * Integer.parseInt(idtienda) dejandolo en 0 si fallaba. La pantalla mandaba
 * idtienda=TODAS para consultar todas, asi que quedaba en 0 y la consulta
 * buscaba "idtienda = 0": escoger TODAS era garantia de pantalla vacia, sin
 * ningun mensaje que lo explicara.
 *
 * Ahora 0 o TODAS significan todas de verdad, y se aceptan fecha hasta y
 * estado, que es como se encuentran las pendientes sin buscarlas a ojo.
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
