package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.SolicitudPQRSCtrl;


/**
 * Servicio de consulta de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.ObtenerMotivosPrioridadPqrs.
 *
 * Ruta: /ProyectoPizzaAmericana/MotivoPrioridadPqrs   Metodos: GET.
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: error, motivo_pqrs, prioridad_pqrs.
 * Lee: motivo_pqrs, prioridad_pqrs.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha MotivoPrioridadPqrs.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/MotivoPrioridadPqrs")
public class MotivoPrioridadPqrs extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			
			SolicitudPQRSCtrl soliCtrl = new SolicitudPQRSCtrl();
			String respuesta = soliCtrl.ObtenerMotivosPrioridadPqrs();
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			out.flush(); 
			out.close(); 

		} catch (Exception e) {
			System.out.println("Error al obtener los estados de Pqrs: " + e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("{\"error\":\"Error en el servidor\"}");
		}
	}
}
