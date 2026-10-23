package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.DesempenoDomiciliarioCtrl;

/**
 * Servicio de consulta de Domiciliarios y enrutamiento: invoca
 * DesempenoDomiciliarioCtrl.consultarDesempeno.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDesempenoDomiciliario   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idempleado (integer), fechadesde (string), fechahasta (string),
 * tiendasdatamart (string).
 * Devuelve: application/json; charset=UTF-8; claves: a_tiempo, aceptadas, alerta_minutos, bueno,
 * calle, datamart_hasta, datos_hasta, demorados, desde, distribucion_promesa,
 * distribucion_regreso, domiciliario, ....
 * Lee: despacho_real, despacho_real_det, empleado, pedido, pedido_sugerencia,
 * pedido_sugerencia_det, pedido_sugerencia_log, tienda, tipo_empleado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDesempenoDomiciliario.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDesempenoDomiciliario")
public class ConsultarDesempenoDomiciliario extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		int idEmpleado = 0;
		try {
			idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
		} catch (Exception e) {
			idEmpleado = 0;
		}
		String desde = aFechaSql(request.getParameter("fechadesde"));
		String hasta = aFechaSql(request.getParameter("fechahasta"));
		logger.info("desempeno domiciliario idempleado=" + idEmpleado + " desde=" + desde + " hasta=" + hasta);
		//Tiendas que no contestaron y que la persona pidio ver desde el datamart (ids separados por coma).
		java.util.Set<Integer> idsDatamart = new java.util.HashSet<Integer>();
		String csv = request.getParameter("tiendasdatamart");
		if (csv != null) {
			for (String parte : csv.split(",")) {
				try {
					idsDatamart.add(Integer.valueOf(Integer.parseInt(parte.trim())));
				} catch (Exception e) {
					//Un id mal escrito se ignora.
				}
			}
		}
		String respuesta = DesempenoDomiciliarioCtrl.consultarDesempeno(idEmpleado, desde, hasta, idsDatamart);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * De dd/MM/yyyy a yyyy-MM-dd. Mismo helper que ConsultarPagosTiendaConciliacion:
	 * la pantalla manda lo que muestra el datepicker y la consulta necesita el
	 * formato de MySQL.
	 */
	private String aFechaSql(final String fecha) {
		if (fecha == null || fecha.trim().length() == 0) {
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
