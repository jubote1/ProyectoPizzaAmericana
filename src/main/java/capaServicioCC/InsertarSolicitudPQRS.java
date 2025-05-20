package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

import capaControladorCC.SolicitudPQRSCtrl;;

/**
 * Servlet implementation class InseratarSolicitudPQRS Este servicio se encarga
 * de insertar en el sistema la informaci�n de una solicitud PQRS, este servlet
 * hace las veces de front y se encarga de la invocaci�n a la clase en la capa
 * Controladora.
 */
@WebServlet("/InsertarSolicitudPQRS")
public class InsertarSolicitudPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public InsertarSolicitudPQRS() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession(); // no se usa, puedes eliminar si no se necesita
		Logger logger = Logger.getLogger("log_file");

		// Strings simples
		String fechasolicitud = request.getParameter("fechasolicitud");
		String tiposolicitud = request.getParameter("tiposolicitud");
		String nombres = request.getParameter("nombre");
		String apellidos = request.getParameter("apellido");
		String telefono = request.getParameter("telefono");
		String direccion = request.getParameter("direccion");
		String zona = request.getParameter("zona");
		String comentario = request.getParameter("comentario");
		String tipo = request.getParameter("tipo");
		String areaResponsable = request.getParameter("arearesponsable");

		// Enteros con método auxiliar
		int idcliente = parseIntSafe(request.getParameter("idcliente"));
		int idtienda = parseIntSafe(request.getParameter("idtienda"));
		int idmunicipio = parseIntSafe(request.getParameter("idmunicipio"));
		int idOrigen = parseIntSafe(request.getParameter("idorigen"));
		int idFoco = parseIntSafe(request.getParameter("idfoco"));
		int idpedidotienda = parseIntSafe(request.getParameter("idpedidotienda"));
		double valorPedido = parseDoubleSafe(request.getParameter("valorPedido"));
		double valorDescuento = parseDoubleSafe(request.getParameter("valorDescuento"));
		int porcentajeDescuento= parseIntSafe(request.getParameter("porcentajeDescuento"));
		boolean descuentoRedimido = "true".equalsIgnoreCase(request.getParameter("descuentoRedimido"));
		int idpedidoredencion = parseIntSafe(request.getParameter("idpedidoredencion"));


		// Llamar a controlador
		SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
		String respuesta = solicitudCtrl.insertarSolicitudPQRS(fechasolicitud, tiposolicitud, idcliente, idtienda,
				nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idOrigen, idFoco, tipo,
				areaResponsable, idpedidotienda,valorPedido,valorDescuento,porcentajeDescuento,descuentoRedimido,idpedidoredencion);

		System.out.println(respuesta);
		response.getWriter().write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	// Métodos auxiliares para parseo seguro
	private int parseIntSafe(String param) {
		try {
			return Integer.parseInt(param);
		} catch (Exception e) {
			return 0;
		}
	}

	private double parseDoubleSafe(String param) {
		try {
			return Double.parseDouble(param);
		} catch (Exception e) {
			return 0.0;
		}
	}
}
