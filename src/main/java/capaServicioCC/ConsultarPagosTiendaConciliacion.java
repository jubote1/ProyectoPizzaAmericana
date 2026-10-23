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

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Pagos virtuales y conciliacion: invoca
 * PedidoCtrl.consultarPagosTiendaConciliacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarPagosTiendaConciliacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer), origen (string), fechadesde (string), fechahasta
 * (string).
 * Devuelve: texto; claves: anulado, error, estacion, fecha, idpedidotienda, nombrecliente, pagos,
 * referenciadatafono, telefono, totalneto, valorformapago.
 * Lee: cliente, pedido, pedido_forma_pago, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarPagosTiendaConciliacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarPagosTiendaConciliacion")
public class ConsultarPagosTiendaConciliacion extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}
		String origen = request.getParameter("origen");
		String fechaDesde = aFechaSql(request.getParameter("fechadesde"));
		String fechaHasta = aFechaSql(request.getParameter("fechahasta"));
		logger.info("consultar pagos tienda conciliacion idtienda=" + idTienda + " origen=" + origen + " desde="
				+ fechaDesde + " hasta=" + fechaHasta);
		String respuesta = PedidoCtrl.consultarPagosTiendaConciliacion(idTienda, origen, fechaDesde, fechaHasta);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/** De dd/MM/yyyy a yyyy-MM-dd. Mismo helper que ConsultarSolicitudConciliacion. */
	private String aFechaSql(final String fecha) {
		if (fecha == null || fecha.trim().length() == 0) {
			return ("");
		}
		final String limpia = fecha.trim();
		if (limpia.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
			return (limpia);
		}
		try {
			final SimpleDateFormat origen = new SimpleDateFormat("dd/MM/yyyy");
			origen.setLenient(false);
			final Date convertida = origen.parse(limpia);
			return (new SimpleDateFormat("yyyy-MM-dd").format(convertida));
		} catch (final ParseException e) {
			System.out.println("ConsultarPagosTiendaConciliacion: fecha ilegible '" + limpia + "'");
			return ("");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
