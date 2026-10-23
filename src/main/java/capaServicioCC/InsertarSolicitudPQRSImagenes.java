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
import capaModeloCC.SolicitudPQRSImagenes;;

/**
 * Servicio que modifica datos de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.insertarSolicitudPQRSImagen.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarSolicitudPQRSImagenes   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): rutaimagen (string), idsolicitudpqrs (integer).
 * Devuelve: texto; claves: idimagen.
 * Escribe: solicitudpqrs_imagenes (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarSolicitudPQRSImagenes.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarSolicitudPQRSImagenes")
public class InsertarSolicitudPQRSImagenes extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarSolicitudPQRSImagenes() {
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
				int idSolicitudPQRS=0;
				String rutaImagen = "";
				rutaImagen = request.getParameter("rutaimagen");
		        try
		        {
		        	idSolicitudPQRS = Integer.parseInt(request.getParameter("idsolicitudpqrs"));
		        }catch(Exception e)
		        {
		        	idSolicitudPQRS = 0;
		        }
		        SolicitudPQRSImagenes imagen = new SolicitudPQRSImagenes(0,idSolicitudPQRS, rutaImagen);
		        SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
		        String respuesta = solicitudCtrl.insertarSolicitudPQRSImagen(imagen);
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
