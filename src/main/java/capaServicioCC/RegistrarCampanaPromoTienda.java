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
 * El POS reporta lo que contesto el cliente en el mostrador.
 *
 * Se guarda en el central y no solo en la tienda porque la lista de "a quien
 * hay que enviarle la boleta" se arma alla: una marca que se quede en la tienda
 * es un cliente al que nadie le va a mandar su premio.
 *
 * Sin sesion, igual que RegistrarCampana15MinAplicado: quien llama es el POS.
 *
 * El usuario que viaja es el de la caja, informativo. No se usa para decidir
 * nada, pero es lo que permite despues mirar si a alguien se le disparo la tasa
 * de "si" por encima del resto.
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
