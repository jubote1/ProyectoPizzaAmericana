package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Reenvia por correo el link de pago que ya existe, opcionalmente a un correo nuevo.
 *
 * Ruta: /ProyectoPizzaAmericana/ReenviarCorreoLinkPago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idpedido (integer), correo (string), actualizar (string).
 * Devuelve: application/json; charset=UTF-8; claves: actualizado, mail.smtp.host,
 * mail.smtp.ssl.protocols, mensaje, resultado.
 * Lee: cliente, crm.correo_rebotado, municipio, nomenclatura_direccion, parametros,
 * pedido_pago_virtual, tienda.
 * Escribe: cliente (UPDATE), pedido_pago_virtual (INSERT).
 * Sistemas externos: Correo SMTP, Gmail, Wompi.
 *
 * Trampas y riesgos conocidos:
 * - Con actualizar = S se puede cambiar el correo de un cliente sin sesion: quien conozca un
 *   numero de pedido podria desviar los links de pago a su propio correo.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ReenviarCorreoLinkPago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ReenviarCorreoLinkPago")
public class ReenviarCorreoLinkPago extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ReenviarCorreoLinkPago() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		HttpSession sesion = request.getSession(false);
		Usuario usuario = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");
		if (usuario == null) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"La sesion se vencio. Vuelva a entrar.\"}");
			return;
		}
		int idPedido;
		try {
			idPedido = Integer.parseInt(request.getParameter("idpedido"));
		} catch (Exception e) {
			idPedido = 0;
		}
		if (idPedido <= 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"No se indico el pedido.\"}");
			return;
		}
		String correo = request.getParameter("correo");
		String actualizar = request.getParameter("actualizar");
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.reenviarCorreoLinkPago(idPedido, correo, actualizar));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
