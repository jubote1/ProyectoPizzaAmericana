package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.OperacionDomiciliariosCtrl;

/**
 * Servicio de consulta del reporte de operacion de domiciliarios diaria: que
 * domiciliarios trabajaron en una tienda un dia, cuantos pedidos llevaron y sus
 * tiempos. Invoca OperacionDomiciliariosCtrl.consultar.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarOperacionDomiciliarios   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idtienda (integer), fecha (string, dd/MM/yyyy o yyyy-MM-dd).
 * Devuelve: application/json; charset=UTF-8; claves: fecha, idtienda, tienda, fuente, datos_hasta, resumen,
 * domiciliarios, por_hora, salidas, alerta_regreso, tope_regreso, error.
 * Lee: despacho_real, despacho_real_det, pedido (en la tienda o en el datamart), empleado,
 * empleado_evento, tipo_empleado (en general).
 */
@WebServlet("/ConsultarOperacionDomiciliarios")
public class ConsultarOperacionDomiciliarios extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		final Logger logger = Logger.getLogger("log_file");

		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"error\":\"Debe iniciar sesion\"}");
			return;
		}

		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (final Exception e) {
			idTienda = 0;
		}
		//Un administrador de punto de venta solo puede ver su propia tienda, pida lo que pida.
		final int idTiendaForzada = capaControladorCC.RestriccionTiendaCtrl.tiendaForzada(request, idTienda);
		if (idTiendaForzada < 0) {
			out.write(capaControladorCC.RestriccionTiendaCtrl.mensajeSinTienda());
			return;
		}
		idTienda = idTiendaForzada;
		final String fecha = aFechaSql(request.getParameter("fecha"));
		if (fecha.length() == 0) {
			out.write("{\"error\":\"La fecha no tiene un formato valido\"}");
			return;
		}
		try {
			final SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			formato.setLenient(false);
			formato.parse(fecha);
		} catch (final Exception e) {
			out.write("{\"error\":\"La fecha no es valida\"}");
			return;
		}

		logger.info("operacion domiciliarios idtienda=" + idTienda + " fecha=" + fecha);
		final long arranco = System.currentTimeMillis();
		out.write(OperacionDomiciliariosCtrl.consultar(idTienda, fecha));
		logger.info("operacion domiciliarios tardo " + (System.currentTimeMillis() - arranco) + " ms");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/** De dd/MM/yyyy (o yyyy-MM-dd) a yyyy-MM-dd; vacio si no es ninguna de las dos. */
	private static String aFechaSql(final String fecha) {
		if (fecha == null) {
			return ("");
		}
		final String limpia = fecha.trim();
		if (limpia.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
			return (limpia);
		}
		if (limpia.matches("^[0-9]{2}/[0-9]{2}/[0-9]{4}$")) {
			return (limpia.substring(6) + "-" + limpia.substring(3, 5) + "-" + limpia.substring(0, 2));
		}
		return ("");
	}
}
