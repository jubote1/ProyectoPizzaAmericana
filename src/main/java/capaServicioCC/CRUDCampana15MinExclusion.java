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
 * CRUD de exclusiones por nombre (The Works, Con Todo, etc.) de la campana 15
 * minutos: idoperacion 1 insertar, 3 eliminar, 4 listar por idcampana. Exige
 * sesion.
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
