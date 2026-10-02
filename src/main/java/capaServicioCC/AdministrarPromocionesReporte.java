package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PromocionReporteCtrl;
import capaModeloCC.Usuario;

/**
 * Administracion del catalogo de promociones del reporte diario.
 *
 * GET  que=listar     las promociones con sus productos
 *      que=buscar     productos de la tienda que coinciden con un texto
 *
 * POST que=crear      promocion nueva
 *      que=actualizar nombre, tipo, activo y orden
 *      que=agregar    un producto a una promocion
 *      que=quitar     un producto de una promocion
 *
 * Lo que cambia el catalogo va por POST: un GET se dispara desde un enlace o
 * desde el historial, y aqui se decide que mide el reporte de toda la red.
 *
 * Exige sesion. No exige administrador: cambiar que promociones se miden no
 * expone informacion sensible ni mueve plata, y quien administra promociones no
 * es necesariamente administrador del sistema. Queda registrado en el log quien
 * toco que.
 */
@WebServlet("/AdministrarPromocionesReporte")
public class AdministrarPromocionesReporte extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public AdministrarPromocionesReporte() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (sinSesion(request, out)) {
			return;
		}
		try {
			final PromocionReporteCtrl ctrl = new PromocionReporteCtrl();
			if ("buscar".equals(texto(request, "que"))) {
				out.write(ctrl.buscarProductos(texto(request, "texto")));
				return;
			}
			out.write(ctrl.listar());
		} catch (final Exception e) {
			System.out.println("AdministrarPromocionesReporte GET: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error consultando el catalogo\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		if (sinSesion(request, out)) {
			return;
		}
		try {
			final PromocionReporteCtrl ctrl = new PromocionReporteCtrl();
			final String que = texto(request, "que");
			final String usuario = usuarioDe(request);

			if ("crear".equals(que)) {
				System.out.println("Promociones: " + usuario + " crea '" + texto(request, "nombre") + "'");
				out.write(ctrl.crear(texto(request, "nombre"),
						"S".equalsIgnoreCase(texto(request, "plataforma")),
						entero(request, "orden", 0)));
				return;
			}
			if ("actualizar".equals(que)) {
				System.out.println("Promociones: " + usuario + " actualiza la "
						+ entero(request, "idpromo", 0));
				out.write(ctrl.actualizar(entero(request, "idpromo", 0), texto(request, "nombre"),
						"S".equalsIgnoreCase(texto(request, "plataforma")),
						"S".equalsIgnoreCase(texto(request, "activo")),
						entero(request, "orden", 0)));
				return;
			}
			if ("agregar".equals(que)) {
				System.out.println("Promociones: " + usuario + " agrega el producto "
						+ entero(request, "idproducto", 0) + " a la " + entero(request, "idpromo", 0));
				out.write(ctrl.agregarProducto(entero(request, "idpromo", 0),
						entero(request, "idproducto", 0)));
				return;
			}
			if ("quitar".equals(que)) {
				System.out.println("Promociones: " + usuario + " quita el producto "
						+ entero(request, "idproducto", 0) + " de la " + entero(request, "idpromo", 0));
				out.write(ctrl.quitarProducto(entero(request, "idpromo", 0),
						entero(request, "idproducto", 0)));
				return;
			}
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Accion desconocida\"}");
		} catch (final Exception e) {
			System.out.println("AdministrarPromocionesReporte POST: " + e.toString());
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error guardando el catalogo\"}");
		}
	}

	private boolean sinSesion(final HttpServletRequest request, final PrintWriter out) {
		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOSESION\",\"detalle\":\"Debe iniciar sesion\"}");
			return (true);
		}
		return (false);
	}

	private String usuarioDe(final HttpServletRequest request) {
		final HttpSession sesion = request.getSession(false);
		final Object guardado = (sesion == null) ? null : sesion.getAttribute("usuario");
		return ((guardado instanceof Usuario) ? ((Usuario) guardado).getNombreUsuario() : "");
	}

	private String texto(final HttpServletRequest request, final String nombre) {
		final String crudo = request.getParameter(nombre);
		return (crudo == null ? "" : crudo.trim());
	}

	private int entero(final HttpServletRequest request, final String nombre, final int siNo) {
		try {
			return (Integer.parseInt(request.getParameter(nombre).trim()));
		} catch (final Exception e) {
			return (siNo);
		}
	}
}
