package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.CampanaPromoDAO;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * CampanaPromoDAO.obtenerActivaParaTienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerCampanaPromoActiva   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer).
 * Devuelve: application/json; claves: exigecorreo, hayCampana, idcampana, nombre, pregunta,
 * productos.
 * Lee: campana_promo, campana_promo_dia, campana_promo_producto, campana_promo_tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerCampanaPromoActiva.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerCampanaPromoActiva")
public class ObtenerCampanaPromoActiva extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ObtenerCampanaPromoActiva() {
		super();
	}

	@SuppressWarnings("unchecked")
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}

		final JSONObject j = new JSONObject();
		if (idTienda <= 0) {
			j.put("hayCampana", Boolean.FALSE);
			out.write(j.toJSONString());
			return;
		}

		final CampanaPromoDAO.Campana c = CampanaPromoDAO.obtenerActivaParaTienda(idTienda);
		if (c == null) {
			j.put("hayCampana", Boolean.FALSE);
			out.write(j.toJSONString());
			return;
		}
		j.put("hayCampana", Boolean.TRUE);
		j.put("idcampana", c.idCampana);
		j.put("nombre", c.nombre);
		j.put("pregunta", c.pregunta);
		j.put("exigecorreo", c.exigeCorreo ? "S" : "N");
		final JSONArray productos = new JSONArray();
		for (CampanaPromoDAO.Producto p : c.productos) {
			productos.add(Integer.valueOf(p.idProducto));
		}
		j.put("productos", productos);
		out.write(j.toJSONString());
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
