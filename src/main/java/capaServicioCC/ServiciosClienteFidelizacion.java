package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import capaControladorCC.FidelizacionCtrl;

/**
 * Servlet implementation class CRUDTienda
 * Servicio que implementa los m�todos para soportar el CRUD de la entidad Tienda.
 */
@WebServlet("/ServiciosClienteFidelizacion")
public class ServiciosClienteFidelizacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServiciosClienteFidelizacion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como par�metro principal el idoperacion con base en lo siguiente: 
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la operaci�n seleccionada se 
	 * solicitar�n ciertos par�metros y posteriormente se invocar� al m�todo correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operaci�n idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String idoperacion = request.getParameter("idoperacion");
		FidelizacionCtrl fideCtrl = new FidelizacionCtrl();
		int operacion;
		String respuesta="";
		System.out.println("operacion " + idoperacion) ;
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		
		String correo = request.getParameter("correo");
		if (operacion ==1)
		{
			
			respuesta = fideCtrl.existeClienteFidelizacion(correo);
		}else if (operacion ==2)
		{
			respuesta = fideCtrl.insertarClienteFidelizacion(correo);
		}else if (operacion ==3 )
		{   respuesta = fideCtrl.activarClienteFidelizacion(correo);
			
		}else if (operacion == 4)
		{
			respuesta = fideCtrl.desactivarClienteFidelizacion(correo);
		}else if(operacion == 5)
		{
			
		}else if(operacion == 6)
		{
			
		}
		//System.out.println(respuesta);
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
