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
 * PreguntaEmpleadoResultadoCtrl.consultarResumen.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarResumenPreguntasEmpleado   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechadesde (string), fechahasta (string).
 * Devuelve: application/json; charset=UTF-8; claves: cargo, correctas, desde, empleados, error,
 * hasta, id, nombre, nombrelargo, porcentaje, respondidas, ultima.
 * Lee: empleado, opcion_respuesta_pregunta, respuesta_empleado, tipo_empleado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarResumenPreguntasEmpleado.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarResumenPreguntasEmpleado")
public class ConsultarResumenPreguntasEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		final String desde = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechadesde"));
		final String hasta = ConsultarDetallePreguntasEmpleado.aFechaSql(request.getParameter("fechahasta"));
		final PrintWriter out = response.getWriter();
		out.write(PreguntaEmpleadoResultadoCtrl.consultarResumen(desde, hasta));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
