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
 * Le dice al POS si hay una campana de promocion viva para su tienda, y con que
 * productos se dispara.
 *
 * El POS no puede preguntar "aplica a este pedido?" porque el detalle del pedido
 * vive en la base de la tienda y el central no la tiene. Asi que se le manda la
 * REGLA y el POS la evalua contra su propio detalle. Mismo reparto que la
 * campana de 15 minutos.
 *
 * Sin sesion a proposito, igual que ObtenerCampana15MinActiva: quien llama es el
 * POS de una tienda, no un navegador con usuario. No expone nada sensible: el
 * nombre de una promocion, la pregunta que la caja va a leer en voz alta, y unos
 * ids de producto.
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
