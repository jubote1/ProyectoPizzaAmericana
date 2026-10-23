package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PreguntaEmpleadoResultadoCtrl;

/**
 * Servicio de consulta de Tiendas y operacion: invoca
 * PreguntaEmpleadoResultadoCtrl.consultarDetalle.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDetallePreguntasEmpleado   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idempleado (integer), fechadesde (string), fechahasta (string).
 * Devuelve: application/json; charset=UTF-8; claves: correcta, correctas, desde, error, fecha,
 * hasta, idempleado, idpregunta, idtienda, porcentaje, porpregunta, pregunta, ....
 * Lee: opcion_respuesta, opcion_respuesta_pregunta, pregunta_empleado, respuesta_empleado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDetallePreguntasEmpleado.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDetallePreguntasEmpleado")
public class ConsultarDetallePreguntasEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		int idEmpleado = 0;
		try {
			idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
		} catch (Exception e) {
			idEmpleado = 0;
		}
		final String desde = aFechaSql(request.getParameter("fechadesde"));
		final String hasta = aFechaSql(request.getParameter("fechahasta"));
		final PrintWriter out = response.getWriter();
		out.write(PreguntaEmpleadoResultadoCtrl.consultarDetalle(idEmpleado, desde, hasta));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * De dd/MM/yyyy a yyyy-MM-dd. Lo que no tenga uno de los dos formatos se
	 * devuelve vacio, asi nada raro llega a la consulta.
	 */
	static String aFechaSql(final String fecha) {
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
