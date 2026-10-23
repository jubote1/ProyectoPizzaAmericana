package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.SolicitudPQRSImagenes;;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * PedidoCtrl.consultarSolicitudCumpleImagenes.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarSolicitudCumpleImagenes   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idsolicitudcumple (integer).
 * Devuelve: texto; claves: rutaimagen.
 * Lee: solicitud_cumple_imagenes.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarSolicitudCumpleImagenes.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarSolicitudCumpleImagenes")
public class ConsultarSolicitudCumpleImagenes extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarSolicitudCumpleImagenes() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como par�metro el iddetallepedido al cual se le realiz� la adicion, adicionalmente se 
	 * env�a el iddetallepedido relacionado a la adicion, la adici�n relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al m�todo InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				int idSolicitud=0;
		        try
		        {
		        	idSolicitud = Integer.parseInt(request.getParameter("idsolicitudcumple"));
		        }catch(Exception e)
		        {
		        	idSolicitud = 0;
		        }
		        PedidoCtrl pedCtrl = new PedidoCtrl();
		        String respuesta = pedCtrl.consultarSolicitudCumpleImagenes(idSolicitud);
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
