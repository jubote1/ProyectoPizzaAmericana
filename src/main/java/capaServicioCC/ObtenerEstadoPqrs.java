package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.AutenticacionCtrl;
import capaControladorCC.SolicitudPQRSCtrl;



@WebServlet("/ObtenerEstadoPqrs")
public class ObtenerEstadoPqrs extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			
			SolicitudPQRSCtrl soliCtrl = new SolicitudPQRSCtrl();
			String respuesta = soliCtrl.obtenerEstadoPqrs();
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
