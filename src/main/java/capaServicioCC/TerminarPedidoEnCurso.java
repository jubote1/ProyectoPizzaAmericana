package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoEnCursoCtrl;

/**
 * Terminar un pedido que quedo a medio tomar.
 *
 * QUE PEDIDO
 *
 * idestadopedido = 1 con enviadopixel = 0: tiene productos, cliente y
 * direccion, pero no tiene forma de pago, y por eso ningun proceso de envio lo
 * ve. Medido sobre 7 dias, 29 de los 33 pedidos atascados son de estos.
 *
 * TRES ACCIONES, EN ESTE ORDEN
 *
 * retener  - antes de abrir el dialogo de forma de pago. Si no se retiene,
 *            apenas el pedido pase a finalizado la red de seguridad se lo
 *            lleva a cocina a medio terminar.
 * terminar - escoge la forma de pago, calcula el total y finaliza.
 * soltar   - si la persona se arrepiente. No es obligatorio: la retencion se
 *            vence sola, pero soltarla de una libera el pedido al instante.
 */
@WebServlet("/TerminarPedidoEnCurso")
public class TerminarPedidoEnCurso extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/** Tope para que una pantalla no pueda retener un pedido por horas. */
	private static final int MAXIMO_MINUTOS = 30;

	public TerminarPedidoEnCurso() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");

		final String accion = texto(request.getParameter("accion"));
		final int idpedido = entero(request.getParameter("idpedido"));

		//El usuario sale de la sesion, no del parametro: quien retiene un
		//pedido queda registrado, y un parametro se puede escribir a mano.
		String usuario = "";
		try {
			if (request.getSession(false) != null
					&& request.getSession(false).getAttribute("usuario") != null) {
				usuario = String.valueOf(request.getSession(false).getAttribute("usuario"));
			}
		} catch (final Exception e) {
			usuario = "";
		}

		final PedidoEnCursoCtrl ctrl = new PedidoEnCursoCtrl();
		String respuesta;

		if ("retener".equals(accion)) {
			int minutos = entero(request.getParameter("minutos"));
			if (minutos <= 0 || minutos > MAXIMO_MINUTOS) {
				minutos = 10;
			}
			respuesta = ctrl.retener(idpedido, usuario, minutos);

		} else if ("terminar".equals(accion)) {
			respuesta = ctrl.terminar(idpedido,
					entero(request.getParameter("idcliente")),
					entero(request.getParameter("idformapago")),
					decimal(request.getParameter("valorformapago")),
					usuario);

		} else if ("soltar".equals(accion)) {
			respuesta = ctrl.soltar(idpedido);

		} else {
			respuesta = "{\"ok\":\"N\",\"mensaje\":\"Accion no reconocida.\"}";
		}

		final PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	private static String texto(final String v) {
		return (v == null ? "" : v.trim());
	}

	private static int entero(final String v) {
		try {
			return (Integer.parseInt(texto(v)));
		} catch (final Exception e) {
			return (0);
		}
	}

	private static double decimal(final String v) {
		try {
			return (Double.parseDouble(texto(v)));
		} catch (final Exception e) {
			return (0);
		}
	}
}
