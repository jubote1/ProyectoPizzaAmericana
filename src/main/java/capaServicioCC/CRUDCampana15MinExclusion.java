package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.Campana15MinCtrl;

/**
 * Servicio con 3 operaciones que se escogen con el parametro operacion.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDCampana15MinExclusion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idoperacion (integer), idcampana (integer), nombreproducto (string),
 * idexclusion (integer).
 * Operaciones (operacion): 1, 3, 4.
 * Devuelve: application/json; claves: detalle, idcampana, idexclusion, nombreproducto, respuesta.
 * Lee: campana_15min_exclusion.
 * Escribe: campana_15min_exclusion (DELETE/INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDCampana15MinExclusion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDCampana15MinExclusion")
public class CRUDCampana15MinExclusion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDCampana15MinExclusion() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}

		int operacion;
		try {
			operacion = Integer.parseInt(request.getParameter("idoperacion"));
		} catch (Exception e) {
			operacion = 0;
		}
		final Campana15MinCtrl ctrl = new Campana15MinCtrl();
		try {
			if (operacion == 1) {
				int idCampana = Integer.parseInt(request.getParameter("idcampana"));
				String nombreProducto = request.getParameter("nombreproducto");
				out.write(ctrl.agregarExclusion(idCampana, nombreProducto));
			} else if (operacion == 3) {
				int idExclusion = Integer.parseInt(request.getParameter("idexclusion"));
				out.write(ctrl.eliminarExclusion(idExclusion));
			} else if (operacion == 4) {
				int idCampana = Integer.parseInt(request.getParameter("idcampana"));
				out.write(ctrl.obtenerExclusiones(idCampana));
			} else {
				out.write("[]");
			}
		} catch (Exception e) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
