package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaDAOCC.CampanaPromoDAO;

/**
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca CampanaPromoDAO.marcar.
 *
 * Ruta: /ProyectoPizzaAmericana/RegistrarCampanaPromoTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: detalle, respuesta.
 * Escribe: pedido_campana_promo (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RegistrarCampanaPromoTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RegistrarCampanaPromoTienda")
public class RegistrarCampanaPromoTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public RegistrarCampanaPromoTienda() {
		super();
	}

	private int entero(final HttpServletRequest request, final String nombre) {
		try {
			return (Integer.parseInt(request.getParameter(nombre)));
		} catch (Exception e) {
			return (0);
		}
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String valor = request.getParameter(nombre);
		return (valor == null ? "" : valor);
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		final int idCampana = this.entero(request, "idcampana");
		final int idTienda = this.entero(request, "idtienda");
		final int idPedidoTienda = this.entero(request, "idpedidotienda");
		if (idCampana <= 0 || idTienda <= 0 || idPedidoTienda <= 0) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Faltan campana, tienda o pedido\"}");
			return;
		}
		final boolean solicito = "S".equalsIgnoreCase(this.texto(request, "solicito"));
		final boolean guardo = CampanaPromoDAO.marcar(idCampana, idTienda, 0, idPedidoTienda,
				this.entero(request, "idcliente"), this.texto(request, "correo"), solicito,
				this.texto(request, "usuario"), this.texto(request, "observacion"));
		out.write(guardo ? "{\"respuesta\":\"OK\"}"
				: "{\"respuesta\":\"NOK\",\"detalle\":\"No se pudo registrar\"}");
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
