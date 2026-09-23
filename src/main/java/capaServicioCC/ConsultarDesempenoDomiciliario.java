package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.DesempenoDomiciliarioCtrl;

/**
 * Servlet implementation class ConsultarDesempenoDomiciliario
 * El tablero de desempeno de un domiciliario: cumplimiento del tiempo de
 * entrega, tiempo de regreso a la tienda y comportamiento frente al
 * enrutamiento, barriendo las tiendas.
 *
 * Tarda: pregunta a las doce tiendas en vivo. La pantalla tiene que avisar que
 * esta consultando.
 */
@WebServlet("/ConsultarDesempenoDomiciliario")
public class ConsultarDesempenoDomiciliario extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		int idEmpleado = 0;
		try {
			idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
		} catch (Exception e) {
			idEmpleado = 0;
		}
		String desde = aFechaSql(request.getParameter("fechadesde"));
		String hasta = aFechaSql(request.getParameter("fechahasta"));
		logger.info("desempeno domiciliario idempleado=" + idEmpleado + " desde=" + desde + " hasta=" + hasta);
		String respuesta = DesempenoDomiciliarioCtrl.consultarDesempeno(idEmpleado, desde, hasta);
		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * De dd/MM/yyyy a yyyy-MM-dd. Mismo helper que ConsultarPagosTiendaConciliacion:
	 * la pantalla manda lo que muestra el datepicker y la consulta necesita el
	 * formato de MySQL.
	 */
	private String aFechaSql(final String fecha) {
		if (fecha == null || fecha.trim().length() == 0) {
			return ("");
		}
		final String limpia = fecha.trim();
		if (limpia.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
			return (limpia);
		}
		if (limpia.matches("^[0-9]{2}/[0-9]{2}/[0-9]{4}$")) {
			return (limpia.substring(6) + "-" + limpia.substring(3, 5) + "-" + limpia.substring(0, 2));
		}
		return ("");
	}
}
