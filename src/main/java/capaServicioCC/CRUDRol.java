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
 * Servlet implementation class CRUDRol
 * CRUD del catalogo de roles: idoperacion 1 insertar 2 editar 3 eliminar 4 consultar.
 * Mismo patron que CRUDEspecialidad.
 */
@WebServlet("/CRUDRol")
public class CRUDRol extends HttpServlet {
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
		if (operacion == 1) {
			String nombre = request.getParameter("nombre");
			String descripcion = request.getParameter("descripcion");
			logger.info("insertar rol " + nombre);
			respuesta = rolCtrl.insertarRol(nombre, descripcion);
		} else if (operacion == 2) {
			int idrol = parsearEntero(request.getParameter("idrol"));
			String nombre = request.getParameter("nombre");
			String descripcion = request.getParameter("descripcion");
			logger.info("editar rol " + idrol);
			respuesta = rolCtrl.editarRol(idrol, nombre, descripcion);
		} else if (operacion == 3) {
			int idrol = parsearEntero(request.getParameter("idrol"));
			respuesta = rolCtrl.eliminarRol(idrol);
		} else if (operacion == 4) {
			int idrol = parsearEntero(request.getParameter("idrol"));
			respuesta = rolCtrl.retornarRol(idrol);
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
