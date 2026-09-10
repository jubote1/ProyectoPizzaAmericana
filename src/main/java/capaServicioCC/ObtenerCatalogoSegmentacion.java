package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.SegmentacionClienteCtrl;

/**
 * Catalogo para los filtros de la pantalla de segmentacion de clientes:
 * productos y especialidades, con su id y su nombre.
 *
 * Los dos van en la misma respuesta porque la pantalla los necesita al mismo
 * tiempo, al cargar.
 *
 * Servlet implementation class ObtenerCatalogoSegmentacion
 */
@WebServlet("/ObtenerCatalogoSegmentacion")
public class ObtenerCatalogoSegmentacion extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public ObtenerCatalogoSegmentacion() {
		super();
	}

	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		try {
			out.write(SegmentacionClienteCtrl.obtenerCatalogoSegmentacion());
		} catch (final Exception e) {
			System.out.println("ObtenerCatalogoSegmentacion: " + e);
			out.write("{\"productos\":[],\"especialidades\":[]}");
		}
	}

	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
