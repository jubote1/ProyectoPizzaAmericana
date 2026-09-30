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
 * CRUD de la configuracion de la campana 15 minutos: idoperacion 1 guardar
 * (inserta o actualiza segun traiga idcampana), 4 listar. Exige sesion, es
 * pantalla de administracion.
 */
@WebServlet("/CRUDCampana15MinConfig")
public class CRUDCampana15MinConfig extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public CRUDCampana15MinConfig() {
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
				int idCampana = 0;
				try {
					idCampana = Integer.parseInt(request.getParameter("idcampana"));
				} catch (Exception e) {
					idCampana = 0;
				}
				String nombre = request.getParameter("nombre");
				boolean activo = "S".equalsIgnoreCase(request.getParameter("activo"));
				String mensajeOperario = request.getParameter("mensajeoperario");
				String mensajeFactura = request.getParameter("mensajefactura");
				String fechaDesde = request.getParameter("fechadesde");
				String fechaHasta = request.getParameter("fechahasta");
				int minutosPromesa = 15;
				try {
					minutosPromesa = Integer.parseInt(request.getParameter("minutospromesa"));
				} catch (Exception e) {
					minutosPromesa = 15;
				}
				double porcentajeRetencion = 5;
				try {
					porcentajeRetencion = Double.parseDouble(request.getParameter("porcentajeretencionmediovirtual"));
				} catch (Exception e) {
					porcentajeRetencion = 5;
				}
				out.write(ctrl.guardarConfiguracion(idCampana, nombre, activo, mensajeOperario, mensajeFactura,
						fechaDesde, fechaHasta, minutosPromesa, porcentajeRetencion));
			} else if (operacion == 4) {
				out.write(ctrl.obtenerConfiguraciones());
			} else if (operacion == 5) {
				out.write(ctrl.obtenerTiendasDesactivadasHoy());
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
