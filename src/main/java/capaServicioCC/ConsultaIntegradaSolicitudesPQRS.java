package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.SolicitudPQRSCtrl;
/**
 * Servicio de consulta de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.ConsultaIntegradaSolicitudesPQRS.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultaIntegradaSolicitudesPQRS   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): fechainicial (string), fechafinal (string), tienda (string), tiposolicitud
 * (string), descuentoredimido (string).
 * Devuelve: application/json; charset=UTF-8; claves: apellidos, arearesponsable, ccVinculado,
 * cliente, correo, descuentoRedimido, direccion, envio_encuesta, fecha_hora_cierre,
 * fecha_hora_registro, fechasolicitud, idconsultaPQRS, ....
 * Lee: estado_pqrs, foco_pqrs, municipio, origen_pqrs, pqrs_comentario, solicitudpqrs,
 * solicitudpqrs_imagenes, tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultaIntegradaSolicitudesPQRS.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
	 * Este servicio se encarga de recibir como par�metros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos par�metros, se invoca en la capa controlador al m�todo ConsultaIntegradaPedidos.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		String fechainicial = request.getParameter("fechainicial");
        String fechafinal = request.getParameter("fechafinal");
        String tienda = request.getParameter("tienda");
        String tipoSolicitud = request.getParameter("tiposolicitud");
        boolean descuentoRedimido = "true".equalsIgnoreCase(request.getParameter("descuentoredimido"));
        logger.info("Llamado a servicio con par�metros fechainicial " + fechainicial + " fechafinal " + fechafinal + " tienda "+ tienda);
        SolicitudPQRSCtrl consultasolicitudes = new SolicitudPQRSCtrl();
        String respuestaConsulta = consultasolicitudes.ConsultaIntegradaSolicitudesPQRS(fechainicial, fechafinal, tienda, tipoSolicitud,descuentoRedimido);
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
