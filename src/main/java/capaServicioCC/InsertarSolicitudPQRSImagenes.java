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
import capaModeloCC.SolicitudPQRSImagenes;;

/**
 * Servlet implementation class InseratarSolicitudPQRS
 * Este servicio se encarga de insertar en el sistema la información de una solicitud PQRS, este servlet hace las veces de front
 * y se encarga de la invocación a la clase en la capa Controladora.
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
				HttpSession sesion = request.getSession();
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
