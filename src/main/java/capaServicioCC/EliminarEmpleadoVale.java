package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import capaControladorCC.EmpleadoCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EmpleadoVale;;

/**
 * Servlet implementation class InseratarSolicitudPQRS
 * Este servicio se encarga de insertar en el sistema la informaci�n de una solicitud PQRS, este servlet hace las veces de front
 * y se encarga de la invocaci�n a la clase en la capa Controladora.
 */
@WebServlet("/EliminarEmpleadoVale")
public class EliminarEmpleadoVale extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EliminarEmpleadoVale() {
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
				String fecha = "";
				int idempleado = 0;
				int idEgreso = 0;
				
				fecha = request.getParameter("fecha");
			    try
		        {
		        	idempleado = Integer.parseInt(request.getParameter("idempleado"));
		        }catch(Exception e)
		        {
		        	idempleado = 0;
		        }
		        try
		        {
		        	idEgreso = Integer.parseInt(request.getParameter("idegreso"));
		        }catch(Exception e)
		        {
		        	idEgreso = 0;
		        }
		        EmpleadoCtrl empCtrl = new EmpleadoCtrl();
		        String respuesta = empCtrl.eliminarEmpleadoVale(idempleado, fecha, idEgreso);
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
