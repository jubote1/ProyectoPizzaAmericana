package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class ActualizarCliente
 */
@WebServlet("/ActualizarClienteCoordenadas")
public class ActualizarClienteCoordenadas extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public ActualizarClienteCoordenadas() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.addHeader("Access-Control-Allow-Origin", "*");

		Logger logger = Logger.getLogger("log_file");
		request.setCharacterEncoding("UTF-8");
		float latitud;
		float longitud;
		int idCliente;

		try {
			latitud = Float.parseFloat(request.getParameter("latitud"));
		} catch (Exception e) {
			latitud = 0;
		}

		try {
			longitud = Float.parseFloat(request.getParameter("longitud"));
		} catch (Exception e) {
			longitud = 0;
		}

		try {
			idCliente = Integer.parseInt(request.getParameter("idcliente"));
		} catch (Exception e) {
			idCliente = 0;
		}

		String direccionProveedor = request.getParameter("direccion_proveedor");

		if (direccionProveedor != null) {
			direccionProveedor = direccionProveedor.trim();

			if (direccionProveedor.isEmpty()) {
				direccionProveedor = null;
			}
		}

		ClienteCtrl ClienCtrl = new ClienteCtrl();
		ClienCtrl.actualizarClienteCoordenadas(
			idCliente,
			latitud,
			longitud,
			direccionProveedor
		);

		PrintWriter out = response.getWriter();
		out.write("");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}