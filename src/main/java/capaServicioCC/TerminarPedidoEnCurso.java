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
 * Termina un pedido que quedo a medio tomar: le pone forma de pago y lo finaliza.
 *
 * Ruta: /ProyectoPizzaAmericana/TerminarPedidoEnCurso   Metodos: GET.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): accion (string), idpedido (string), minutos (string), idcliente (string),
 * idformapago (string), valorformapago (string).
 * Devuelve: application/json;charset=UTF-8; claves: apellidos, apiKey, bloqueado, cantidad,
 * cantidaditempedido, clientId, cliente, clientesiniden, contactEvent, contactExtEventType, date,
 * descuento, ....
 * Lee: cliente, detalle_pedido, forma_pago, homologacion_forma_pago, integracion_crm, marcacion,
 * marcacion_pedido, municipio, nomenclatura_direccion, pedido.
 * Escribe: pedido (UPDATE).
 * Sistemas externos: Rappi, SALESmanago.
 *
 * Trampas y riesgos conocidos:
 * - Responde ok: S aunque el envio a la tienda no haya ocurrido: el exito es solo "finalizado en
 *   el central".
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha TerminarPedidoEnCurso.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
		//OJO: el atributo de la sesion es un objeto Usuario, no un texto. Se lee igual que en
		//EditarPedidoEnCurso: con String.valueOf quedaba "Usuario@1a2b3c" en retenido_por y en
		//usuarioreenvio, que no le dice a nadie quien fue.
		final String usuario = EditarPedidoEnCurso.usuarioDeLaSesion(request);

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
