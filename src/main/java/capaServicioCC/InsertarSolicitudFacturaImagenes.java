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
import capaModeloCC.SolicitudFacturaImagenes;
import capaModeloCC.SolicitudPQRSImagenes;;

/**
 * Servicio que modifica datos de Facturacion electronica: invoca
 * PedidoCtrl.insertarSolicitudFacturaImagen.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarSolicitudFacturaImagenes   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): rutaimagen (string), idsolicitud (integer).
 * Devuelve: texto; claves: idimagen.
 * Escribe: solicitud_factura_imagenes (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarSolicitudFacturaImagenes.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarSolicitudFacturaImagenes")
public class InsertarSolicitudFacturaImagenes extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarSolicitudFacturaImagenes() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el iddetallepedido al cual se le realizó la adicion, adicionalmente se 
	 * envía el iddetallepedido relacionado a la adicion, la adición relacionada a la especialidad 1 o la especialidad 2 y 
	 * las cantidades. Lo anterior invocando al método InsertarDetalleAdicion de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				int idSolicitud=0;
				String rutaImagen = "";
				rutaImagen = request.getParameter("rutaimagen");
		        try
		        {
		        	idSolicitud = Integer.parseInt(request.getParameter("idsolicitud"));
		        }catch(Exception e)
		        {
		        	idSolicitud = 0;
		        }
		        SolicitudFacturaImagenes imagen = new SolicitudFacturaImagenes(0,idSolicitud, rutaImagen);
		        PedidoCtrl pedCtrl = new PedidoCtrl();
		        String respuesta = pedCtrl.insertarSolicitudFacturaImagen(imagen);
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
