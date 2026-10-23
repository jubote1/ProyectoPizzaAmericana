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
 * Crea de nuevo el link de pago de un pedido (por ejemplo cuando vencio) y lo vuelve a enviar.
 *
 * Ruta: /ProyectoPizzaAmericana/RecrearLinkPagoWompi   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Lee la sesion pero no la exige.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: application/json; charset=UTF-8; claves: amount_in_cents, collect_shipping, currency,
 * description, expires_at, idlink, mensaje, name, redirect_url, respuesta, resultado, single_use,
 * ....
 * Lee: cliente, forma_pago, municipio, nomenclatura_direccion, parametros, parametros_correo,
 * tienda.
 * Escribe: cliente (UPDATE), pedido (UPDATE), pedido_idlink (INSERT), pedido_pago_virtual
 * (INSERT).
 * Sistemas externos: Brevo, Kommo (CRM), WhatsApp, Wompi.
 *
 * Trampas y riesgos conocidos:
 * - Sin sesion: cualquiera puede generar links y mandar correos a clientes de cualquier pedido.
 * - Cada llamada puede generar un correo al cliente (el tope existe, pero hay que tenerlo
 *   presente).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha RecrearLinkPagoWompi.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/RecrearLinkPagoWompi")
public class RecrearLinkPagoWompi extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public RecrearLinkPagoWompi() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
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
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.recrearLinkPagoWompi(idPedido));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
