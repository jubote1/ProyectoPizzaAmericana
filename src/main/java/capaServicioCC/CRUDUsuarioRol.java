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
 * Servlet implementation class CRUDUsuarioRol
 * Administra que rol(es) tiene un usuario. idoperacion 1 guardar (reemplaza
 * todo el set de roles del usuario) 4 consultar (ids de rol ya asignados).
 */
@WebServlet("/CRUDUsuarioRol")
public class CRUDUsuarioRol extends HttpServlet {
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
		int idusuario = parsearEntero(request.getParameter("idusuario"));
		if (operacion == 1) {
			String idsrol = request.getParameter("idsrol");
			logger.info("guardar roles del usuario " + idusuario + ": " + idsrol);
			respuesta = rolCtrl.guardarRolesUsuario(idusuario, idsrol);
		} else if (operacion == 4) {
			respuesta = rolCtrl.listarIdsRolPorUsuario(idusuario);
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
