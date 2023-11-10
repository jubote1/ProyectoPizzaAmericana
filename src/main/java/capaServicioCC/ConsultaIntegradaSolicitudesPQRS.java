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

import capaControladorCC.SolicitudPQRSCtrl;
/**
 * Servlet implementation class ConsultaIntegradaPedidos
 */
@WebServlet("/ConsultaIntegradaSolicitudesPQRS")
public class ConsultaIntegradaSolicitudesPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultaIntegradaSolicitudesPQRS() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como parámetros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos parámetros, se invoca en la capa controlador al método ConsultaIntegradaPedidos.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession();
		String fechainicial = request.getParameter("fechainicial");
        String fechafinal = request.getParameter("fechafinal");
        String tienda = request.getParameter("tienda");
        String tipoSolicitud = request.getParameter("tiposolicitud");
        logger.info("Llamado a servicio con parámetros fechainicial " + fechainicial + " fechafinal " + fechafinal + " tienda "+ tienda);
        SolicitudPQRSCtrl consultasolicitudes = new SolicitudPQRSCtrl();
        String respuestaConsulta = consultasolicitudes.ConsultaIntegradaSolicitudesPQRS(fechainicial, fechafinal, tienda, tipoSolicitud);
        PrintWriter out = response.getWriter();
        logger.debug(respuestaConsulta);
		out.write(respuestaConsulta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
