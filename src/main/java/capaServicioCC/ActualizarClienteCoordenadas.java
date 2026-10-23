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
 * Servicio que modifica datos de Domiciliarios y enrutamiento: invoca
 * ClienteCtrl.actualizarClienteCoordenadas.
 *
 * Ruta: /ProyectoPizzaAmericana/ActualizarClienteCoordenadas   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): latitud (number), longitud (number), idcliente (integer),
 * direccion_proveedor (string).
 * Devuelve: texto.
 * Escribe: cliente (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ActualizarClienteCoordenadas.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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