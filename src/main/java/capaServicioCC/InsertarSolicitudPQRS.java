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

import capaControladorCC.SolicitudPQRSCtrl;;

/**
 * Servlet implementation class InseratarSolicitudPQRS
 * Este servicio se encarga de insertar en el sistema la información de una solicitud PQRS, este servlet hace las veces de front
 * y se encarga de la invocación a la clase en la capa Controladora.
 */
@WebServlet("/InsertarSolicitudPQRS")
public class InsertarSolicitudPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarSolicitudPQRS() {
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
				String fechasolicitud = "";
				String tiposolicitud = ""; 
				int idcliente = 0;
				int idtienda = 0;
				String nombres = "";
				String apellidos = "";
				String telefono = ""; 
				String direccion = "";
				String zona = "";
				int idmunicipio = 0;
				String comentario = "";
				int idOrigen = 0;
				int idFoco = 0;
				String tipo = "";
				String areaResponsable = "";
				fechasolicitud = request.getParameter("fechasolicitud");
				tiposolicitud = request.getParameter("tiposolicitud");
				nombres = request.getParameter("nombres");
				apellidos = request.getParameter("apellidos");
				telefono = request.getParameter("telefono");
				direccion = request.getParameter("direccion");
				zona = request.getParameter("zona");
				comentario = request.getParameter("comentario");
				tipo = request.getParameter("tipo");
				areaResponsable = request.getParameter("arearesponsable");
		        try
		        {
		        	idcliente = Integer.parseInt(request.getParameter("idcliente"));
		        }catch(Exception e)
		        {
		        	idcliente = 0;
		        }
		        try
		        {
		        	idtienda = Integer.parseInt(request.getParameter("idtienda"));
		        }catch(Exception e)
		        {
		        	idtienda = 0;
		        }
		        
		        try
		        {
		        	idmunicipio =  Integer.parseInt(request.getParameter("idmunicipio"));
		        }catch(Exception e)
		        {
		        	idmunicipio = 0;
		        }
		        //Vamos a capturar el origen de la PQRS
		        try
		        {
		        	idOrigen=  Integer.parseInt(request.getParameter("idorigen"));
		        }catch(Exception e)
		        {
		        	idOrigen = 0;
		        }
		        //Vamos a capturar foco de la PQRS
		        try
		        {
		        	idFoco=  Integer.parseInt(request.getParameter("idfoco"));
		        }catch(Exception e)
		        {
		        	idFoco = 0;
		        }
		        //logger.info("Llamado a servicio InsertarSolicitudPQRS con parámetros iddetallepedidopadre: "
		        //		+ iddetallepedidopadre + " iddetallepedidoadicion:  " + iddetallepedidoadicion + " idespecialidad1: " + idespecialidad1
		        //		+ " idespecialidad2: " + idespecialidad2 + " cantidad1: " + cantidad1 + " cantidad2: " + cantidad2 );
		        SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
		        String respuesta = solicitudCtrl.insertarSolicitudPQRS(fechasolicitud, tiposolicitud, idcliente, idtienda, nombres, apellidos, telefono, direccion, zona, idmunicipio, comentario, idOrigen, idFoco, tipo, areaResponsable);
		        System.out.println(respuesta);
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
