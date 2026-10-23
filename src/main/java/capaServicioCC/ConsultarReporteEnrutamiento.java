package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.ReporteEnrutamientoCtrl;

/**
 * Servicio de consulta de Domiciliarios y enrutamiento: invoca ReporteEnrutamientoCtrl.consultar.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarReporteEnrutamiento   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idtienda (integer), fechainicial (string), fechafinal (string).
 * Devuelve: application/json; charset=UTF-8; claves: anulados, datos_hasta, desde, detalle,
 * disponible, domiciliario, en_curso, enrutados, entradas, entradas_enrutadas, error, estado, ....
 * Lee: cambios_estado_pedido, despacho_real, despacho_real_det, empleado,
 * information_schema.columns, information_schema.tables, pedido, pedido_sugerencia,
 * pedido_sugerencia_det, pedido_sugerencia_log.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarReporteEnrutamiento.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarReporteEnrutamiento")
public class ConsultarReporteEnrutamiento extends HttpServlet {
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
		final String desde = aFechaSql(request.getParameter("fechainicial"));
		final String hasta = aFechaSql(request.getParameter("fechafinal"));
		if (desde.length() == 0 || hasta.length() == 0) {
			out.write("{\"error\":\"Las fechas no tienen un formato valido\"}");
			return;
		}
		try {
			final SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			formato.setLenient(false);
			final Date d = formato.parse(desde);
			final Date h = formato.parse(hasta);
			if (d.after(h)) {
				out.write("{\"error\":\"La fecha inicial no puede ser mayor que la final\"}");
				return;
			}
			final long dias = (h.getTime() - d.getTime()) / (24L * 60L * 60L * 1000L) + 1;
			if (dias > ReporteEnrutamientoCtrl.DIAS_MAXIMOS) {
				out.write("{\"error\":\"El rango maximo es de " + ReporteEnrutamientoCtrl.DIAS_MAXIMOS
						+ " dias; el escogido tiene " + dias + "\"}");
				return;
			}
		} catch (final Exception e) {
			out.write("{\"error\":\"Las fechas no son validas\"}");
			return;
		}

		logger.info("reporte enrutamiento idtienda=" + idTienda + " desde=" + desde + " hasta=" + hasta);
		final long arranco = System.currentTimeMillis();
		out.write(ReporteEnrutamientoCtrl.consultar(idTienda, desde, hasta));
		logger.info("reporte enrutamiento tardo " + (System.currentTimeMillis() - arranco) + " ms");
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
