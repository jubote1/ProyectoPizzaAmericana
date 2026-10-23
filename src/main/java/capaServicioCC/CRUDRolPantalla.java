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
 * Ruta: /ProyectoPizzaAmericana/CRUDRolPantalla   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (integer), idrol (string), idspantalla (string).
 * Operaciones (operacion): 1, 4.
 * Devuelve: texto; claves: idspantalla, resultado.
 * Lee: rol_pantalla.
 * Escribe: rol_pantalla (DELETE/INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDRolPantalla.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
