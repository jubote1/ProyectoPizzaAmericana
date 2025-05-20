package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import capaControladorCC.SolicitudPQRSCtrl;

@WebServlet("/ActualizarSolicitudPQRS")
public class ActualizarSolicitudPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ActualizarSolicitudPQRS() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");

		// Parámetros tipo String
		String fechasolicitud = request.getParameter("fechasolicitud");
		String tiposolicitud = request.getParameter("tiposolicitud");
		String nombres = request.getParameter("nombres");
		String apellidos = request.getParameter("apellidos");
		String telefono = request.getParameter("telefono");
		String direccion = request.getParameter("direccion");
		String zona = request.getParameter("zona");
		String comentario = request.getParameter("comentario");
		String tipo = request.getParameter("tipo");
		String areaResponsable = request.getParameter("arearesponsable");

		// Parámetros numéricos con parseo seguro
		int idSolicitudPQRS = parseIntSafe(request, "idsolicitudpqrs");
		int idcliente = parseIntSafe(request, "idcliente");
		int idtienda = parseIntSafe(request, "idtienda");
		int idmunicipio = parseIntSafe(request, "idmunicipio");
		int idOrigen = parseIntSafe(request, "idorigen");
		int idFoco = parseIntSafe(request, "idfoco");
		int idpedidotienda = parseIntSafe(request, "idpedidotienda");
		double valorPedido = parseDoubleSafe(request, "valorPedido");
		double valorDescuento = parseDoubleSafe(request, "valorDescuento");
		int porcentajeDescuento= parseIntSafe(request,"porcentajeDescuento");
		boolean descuentoRedimido = "true".equalsIgnoreCase(request.getParameter("descuentoRedimido"));
		int idpedidoredencion = parseIntSafe(request ,"idpedidoredencion");
		// Lógica principal
		SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
		String respuesta = solicitudCtrl.actualizarSolicitudPQRS(idSolicitudPQRS, fechasolicitud, tiposolicitud,
				idcliente, idtienda, nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idOrigen,
				idFoco, tipo, areaResponsable, idpedidotienda, valorPedido, valorDescuento,porcentajeDescuento,descuentoRedimido,idpedidoredencion);

		// Salida
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	// Métodos auxiliares de parseo seguro
	private int parseIntSafe(HttpServletRequest request, String paramName) {
		try {
			return Integer.parseInt(request.getParameter(paramName));
		} catch (Exception e) {
			return 0;
		}
	}

	private double parseDoubleSafe(HttpServletRequest request, String paramName) {
		try {
			return Double.parseDouble(request.getParameter(paramName));
		} catch (Exception e) {
			return 0.0;
		}
	}
}
