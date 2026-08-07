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
import capaControladorCC.TiendaCtrl;

/**
 * Servlet implementation class AprobarBloqueoTienda
 * Servicio que implementa los m�todos para soportar el CRUD de la entidad Tienda.
 */
@WebServlet("/AprobarBloqueoTienda")
public class AprobarBloqueoTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AprobarBloqueoTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como par�metro principal el idoperacion con base en lo siguiente: 
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la operaci�n seleccionada se 
	 * solicitar�n ciertos par�metros y posteriormente se invocar� al m�todo correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		//Operaci�n idoperacion 1 insertar 3 Eliminar  4 Consultar
		HttpSession sesion = request.getSession();
		request.setCharacterEncoding("UTF-8");
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		String stridLogBloqueo = request.getParameter("idlogbloqueo");
		int idLogBloqueo;
		String respuesta="";
		TiendaCtrl tiendaCtrl = new TiendaCtrl();
		try
		{
			idLogBloqueo = Integer.parseInt(stridLogBloqueo);
		}catch(Exception e){
			idLogBloqueo = 0;
		}
		respuesta = tiendaCtrl.aprobarBloqueoTienda(idLogBloqueo);
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
