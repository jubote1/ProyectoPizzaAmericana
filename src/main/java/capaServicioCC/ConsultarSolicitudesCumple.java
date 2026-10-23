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
import capaModeloCC.SolicitudFactura;
import capaModeloCC.Usuario;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * PedidoCtrl.consultarSolicitudesCumple.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarSolicitudesCumple   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string).
 * Devuelve: texto; claves: fecha, idpedido, idsolicitudcumple, tienda.
 * Lee: solicitud_cumple, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarSolicitudesCumple.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarSolicitudesCumple")
public class ConsultarSolicitudesCumple extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarSolicitudesCumple() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		String fechaInicial = request.getParameter("fechainicial");
		String fechaFinal = request.getParameter("fechafinal");
        PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta  = pedCtrl.consultarSolicitudesCumple(fechaInicial, fechaFinal);
        PrintWriter out = response.getWriter();
        out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
