package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Domiciliarios y enrutamiento: invoca
 * PedidoCtrl.consultarConteoPedidosDomiciliario.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarConteoPedidosDomiciliario   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer), claveusuario (string).
 * Devuelve: application/json;charset=UTF-8; claves: resultado, tipo_error.
 * Lee: tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarConteoPedidosDomiciliario.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarConteoPedidosDomiciliario")
public class ConsultarConteoPedidosDomiciliario extends HttpServlet {
	private static final long serialVersionUID = 1L;

    public ConsultarConteoPedidosDomiciliario() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		int idTienda = 0;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}
		String claveUsuario = request.getParameter("claveusuario");
		if (claveUsuario == null) {
			claveUsuario = "";
		}
		PedidoCtrl pedidoCtrl = new PedidoCtrl();
        String respuesta = pedidoCtrl.consultarConteoPedidosDomiciliario(idTienda, claveUsuario);
        PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
