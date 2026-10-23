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
 * Abre un pedido que quedo a medio tomar (estado 1, sin numero en la tienda) para seguir
 * agregandole productos, y lo reserva a nombre de quien lo abre.
 *
 * Ruta: /ProyectoPizzaAmericana/EditarPedidoEnCurso   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): accion (string), idpedido (string).
 * Devuelve: application/json;charset=UTF-8; claves: adicion, cantidad, especialidad1,
 * especialidad2, horaprogramado, idcliente, iddetallepedido, idpedido, idtienda, idtipopedido,
 * lineas, mensaje, ....
 * Lee: cliente, detalle_pedido, especialidad, pedido, producto, tienda.
 * Escribe: pedido (UPDATE).
 *
 * Trampas y riesgos conocidos:
 * - Es el servicio con mejores practicas del grupo (sesion obligatoria, codigos HTTP, mensajes
 *   claros, proteccion contra concurrencia): sirve de modelo.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha EditarPedidoEnCurso.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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

		if ("reanudar".equals(accion)) {
			out.write(ctrl.reanudar(idPedido, usuario));
		} else if ("renovar".equals(accion)) {
			if (!esPost) {
				//Cambia datos: no se hace por GET, un enlace o un rastreador lo dispararia.
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				out.write("{\"ok\":\"N\",\"mensaje\":\"Use POST para renovar.\"}");
				return;
			}
			out.write(ctrl.renovar(idPedido, usuario));
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
	public static String usuarioDeLaSesion(final HttpServletRequest request) {
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
