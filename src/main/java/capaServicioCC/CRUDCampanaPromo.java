package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.CampanaPromoCtrl;

/**
 * Pantalla de campanas de promocion. Mismo molde que CRUDCampana15MinConfig:
 * una sola puerta con idoperacion.
 *
 *   1  guardar (crea si idcampana viene en cero)
 *   2  eliminar
 *   4  listar
 *   6  obtener una con sus tiendas, dias y productos
 *   7  consultar como se llama un producto o una excepcion
 *   8  listar las excepciones de precio del central
 */
@WebServlet("/CRUDCampanaPromo")
public class CRUDCampanaPromo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CRUDCampanaPromo() {
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

		// Una campana define que se le promete a un cliente y en que tiendas, asi
		// que esto no se toca sin sesion.
		final HttpSession sesion = request.getSession(false);
		if (sesion == null || sesion.getAttribute("usuario") == null) {
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Debe iniciar sesion\"}");
			return;
		}

		final int operacion = this.entero(request, "idoperacion");
		final CampanaPromoCtrl ctrl = new CampanaPromoCtrl();
		try {
			if (operacion == 1) {
				out.write(ctrl.guardar(this.entero(request, "idcampana"), this.texto(request, "nombre"),
						"S".equalsIgnoreCase(this.texto(request, "activo")), this.texto(request, "fechadesde"),
						this.texto(request, "fechahasta"), this.texto(request, "pregunta"),
						this.entero(request, "idoferta"),
						"S".equalsIgnoreCase(this.texto(request, "exigecorreo")), this.texto(request, "tiendas"),
						this.texto(request, "dias"), this.texto(request, "productos")));
			} else if (operacion == 2) {
				out.write(ctrl.eliminar(this.entero(request, "idcampana")));
			} else if (operacion == 4) {
				out.write(ctrl.listar());
			} else if (operacion == 6) {
				out.write(ctrl.obtener(this.entero(request, "idcampana")));
			} else if (operacion == 7) {
				out.write(ctrl.consultarNombreProducto(this.texto(request, "ambito"),
						this.entero(request, "idproducto"), this.entero(request, "idexcepcion"),
						this.texto(request, "tiendas")));
			} else if (operacion == 8) {
				out.write(ctrl.listarExcepciones());
			} else {
				out.write("[]");
			}
		} catch (Exception e) {
			System.out.println("CRUDCampanaPromo idoperacion " + operacion + ": " + e);
			out.write("{\"respuesta\":\"NOK\",\"detalle\":\"Error tecnico\"}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
