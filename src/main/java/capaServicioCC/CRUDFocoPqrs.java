package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.SolicitudPQRSCtrl;

/**
 * Servlet implementation class CRUDFormaPago
 * Servicio que implementa el CRUD de la entidad Forma Pago.
 */
@WebServlet("/CRUDFocoPqrs")
public class CRUDFocoPqrs extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDFocoPqrs() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibirá como parámetro principal el idoperacion con base en los siguientes valores 
	 *  idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la id operacion se pedirán los otros parámetros y se invocará el método en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");		
		HttpSession sesion = request.getSession();
				response.addHeader("Access-Control-Allow-Origin", "*");
				String idoperacion = request.getParameter("idoperacion");
				SolicitudPQRSCtrl PqrsCtrl = new SolicitudPQRSCtrl();
				int operacion;
				String respuesta="";
				try
				{
					operacion = Integer.parseInt(idoperacion);
				}catch(Exception e){
					operacion = 0;
				}
				if (operacion ==1)
				{
				
					//respuesta = ParametrosCtrl.insertarEstadoPedido(descripcion);
				}else if (operacion ==2)
				{

					//respuesta = ParametrosCtrl.editarEstadoPedido(idestedit, descedit);
				}else if (operacion ==3 )
				{
					
					//respuesta = ParametrosCtrl.eliminarEstadoPedido(idesteli);
				}else if (operacion == 4)
				{
					//respuesta = ParametrosCtrl.retornarEstadoPedido(idestcon);
				}else if(operacion == 5)
				{
					respuesta = PqrsCtrl.obtenerFocosPqrs();
				}
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
