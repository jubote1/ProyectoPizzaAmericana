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
 * Ruta: /ProyectoPizzaAmericana/CRUDCampana15MinConfig   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idoperacion (integer), idcampana (integer), nombre (string), activo
 * (string), mensajeoperario (string), mensajefactura (string), fechadesde (string), fechahasta
 * (string), minutospromesa (integer), porcentajeretencionmediovirtual (number).
 * Operaciones (operacion): 1, 4, 5.
 * Devuelve: application/json; claves: activo, detalle, fechadesde, fechahasta, fechahora,
 * idcampana, idtienda, mensajefactura, mensajeoperario, minutospromesa, motivo, nombre, ....
 * Lee: campana_15min_config, campana_15min_tienda_desactivada_dia, tienda.
 * Escribe: campana_15min_config (INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDCampana15MinConfig.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
