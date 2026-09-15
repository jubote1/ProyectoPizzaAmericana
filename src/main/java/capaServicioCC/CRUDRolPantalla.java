package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.RolCtrl;

/**
 * Servlet implementation class CRUDRolPantalla
 * Administra que pantallas ve un rol. idoperacion 1 guardar (reemplaza todo
 * el set de pantallas del rol) 4 consultar (ids de pantalla ya asignados).
 */
@WebServlet("/CRUDRolPantalla")
public class CRUDRolPantalla extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		RolCtrl rolCtrl = new RolCtrl();
		int operacion;
		String respuesta = "";
		try {
			operacion = Integer.parseInt(request.getParameter("idoperacion"));
		} catch (Exception e) {
			operacion = 0;
		}
		int idrol = parsearEntero(request.getParameter("idrol"));
		if (operacion == 1) {
			String idspantalla = request.getParameter("idspantalla");
			logger.info("guardar pantallas del rol " + idrol + ": " + idspantalla);
			respuesta = rolCtrl.guardarPantallasRol(idrol, idspantalla);
		} else if (operacion == 4) {
			respuesta = rolCtrl.listarIdsPantallaPorRol(idrol);
		}
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	private int parsearEntero(String valor) {
		try {
			return (Integer.parseInt(valor));
		} catch (Exception e) {
			return (0);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
