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
 * PedidoCtrl.consultarDireccionesDomicilio.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarDireccionesDomicilio   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), idtienda (integer).
 * Devuelve: application/json;charset=UTF-8; claves: apellido, direccion, fechapedido, idpedido,
 * latitud, longitud, municipio, nombre, valor.
 * Lee: cliente, municipio, pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarDireccionesDomicilio.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarDireccionesDomicilio")
public class ConsultarDireccionesDomicilio extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
		int idTienda;
		try {
			idTienda = Integer.parseInt(request.getParameter("idtienda"));
		} catch (Exception e) {
			idTienda = 0;
		}
		PrintWriter out = response.getWriter();
		if (idTienda <= 0 || fechaInicial == null || fechaFinal == null) {
			out.write("[]");
			return;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.consultarDireccionesDomicilio(fechaInicial, fechaFinal, idTienda));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
