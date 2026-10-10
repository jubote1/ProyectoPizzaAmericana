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
 * Editar el contenido de un pedido que quedo a medio tomar: ver lo que tiene, quitarle un producto, agregarle
 * otro. Es la parte de "editar" de la pantalla ConsultaPedidosEnCurso.html; terminarlo o descartarlo lo hace
 * TerminarPedidoEnCurso.
 *
 * ACCIONES
 *
 * Lectura (GET):
 *   lineas   idpedido                       El detalle del pedido y su total.
 *   catalogo idpedido                       Los productos disponibles en la tienda del pedido.
 *   opciones idpedido, idproducto           Especialidades y sabores de bebida de ese producto.
 *
 * Cambian datos (solo POST):
 *   agregar  idpedido, idproducto, cantidad, idespecialidad1, idespecialidad2, idsabor, observacion
 *   quitar   idpedido, iddetalle
 *   renovar  idpedido                       Alarga la edicion mientras la persona trabaja.
 *
 * PARA EDITAR HAY QUE TENER EL PEDIDO RETENIDO (TerminarPedidoEnCurso, accion=retener) y que sea de quien
 * llama. El usuario sale de la sesion, no de un parametro. El precio no se recibe: lo calcula el servidor.
 *
 * Acceso: P2 (usuario interno con sesion). A diferencia de la mayoria de los servicios antiguos, este si
 * verifica la sesion: modifica el contenido de un pedido, y eso se cobra.
 */
@WebServlet("/EditarPedidoEnCurso")
public class EditarPedidoEnCurso extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, false);
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		atender(request, response, true);
	}

	private void atender(final HttpServletRequest request, final HttpServletResponse response,
			final boolean esPost) throws IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");
		final PrintWriter out = response.getWriter();

		final String usuario = usuarioDeLaSesion(request);
		if (usuario.length() == 0) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			out.write("{\"ok\":\"N\",\"mensaje\":\"Inicie sesion para editar pedidos.\"}");
			return;
		}

		final String accion = texto(request.getParameter("accion"));
		final int idPedido = entero(request.getParameter("idpedido"));
		final PedidoEnCursoCtrl ctrl = new PedidoEnCursoCtrl();

		if ("lineas".equals(accion)) {
			out.write(ctrl.lineas(idPedido));
		} else if ("catalogo".equals(accion)) {
			out.write(ctrl.catalogo(idPedido));
		} else if ("opciones".equals(accion)) {
			out.write(ctrl.opciones(idPedido, entero(request.getParameter("idproducto"))));
		} else if ("agregar".equals(accion) || "quitar".equals(accion) || "renovar".equals(accion)) {
			if (!esPost) {
				//Un cambio de datos no se hace por GET: un enlace o un rastreador lo dispararia.
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"ok\":\"N\",\"mensaje\":\"Use POST para modificar el pedido.\"}");
				return;
			}
			if ("agregar".equals(accion)) {
				out.write(ctrl.agregar(idPedido, entero(request.getParameter("idproducto")),
						entero(request.getParameter("cantidad")), entero(request.getParameter("idespecialidad1")),
						entero(request.getParameter("idespecialidad2")), entero(request.getParameter("idsabor")),
						texto(request.getParameter("observacion")), usuario));
			} else if ("quitar".equals(accion)) {
				out.write(ctrl.quitar(idPedido, entero(request.getParameter("iddetalle")), usuario));
			} else {
				out.write(ctrl.renovar(idPedido, usuario));
			}
		} else {
			out.write("{\"ok\":\"N\",\"mensaje\":\"Accion no reconocida.\"}");
		}
	}

	/**
	 * El nombre de usuario de quien tiene la sesion abierta. El atributo "usuario" de la sesion es un objeto
	 * Usuario (lo pone GetIngresarAplicacion), no un texto: convertirlo con String.valueOf deja algo como
	 * "capaModeloCC.Usuario@1a2b3c", distinto en cada sesion, que es justo lo que NO sirve para saber quien
	 * tiene retenido un pedido.
	 */
	static String usuarioDeLaSesion(final HttpServletRequest request) {
		try {
			if (request.getSession(false) != null) {
				final Object u = request.getSession(false).getAttribute("usuario");
				if (u instanceof capaModeloCC.Usuario) {
					final String nombre = ((capaModeloCC.Usuario) u).getNombreUsuario();
					return (nombre == null ? "" : nombre.trim());
				}
				if (u != null) {
					return (String.valueOf(u).trim());
				}
			}
		} catch (final Exception e) {
			// Sin sesion legible: se trata como sin sesion.
		}
		return ("");
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
}
