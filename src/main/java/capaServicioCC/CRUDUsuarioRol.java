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
 * Servicio con 2 operaciones que se escogen con el parametro operacion.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDUsuarioRol   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (integer), idusuario (string), idsrol (string).
 * Operaciones (operacion): 1, 4.
 * Devuelve: texto; claves: idsrol, resultado.
 * Lee: usuario_rol.
 * Escribe: usuario_rol (DELETE/INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDUsuarioRol.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
