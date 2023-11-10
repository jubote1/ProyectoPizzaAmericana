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
 * Servlet implementation class CRUDTienda
 * Servicio que implementa los métodos para soportar el CRUD de la entidad Tienda.
 */
@WebServlet("/CRUDTienda")
public class CRUDTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como parámetro principal el idoperacion con base en lo siguiente: 
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la operación seleccionada se 
	 * solicitarán ciertos parámetros y posteriormente se invocará al método correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String idoperacion = request.getParameter("idoperacion");
		TiendaCtrl tiendaCtrl = new TiendaCtrl();
		int operacion;
		String respuesta="";
		System.out.println("operacion " + idoperacion) ;
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			String nombre = request.getParameter("nombre");
			String dsn = request.getParameter("dsn");
			respuesta = tiendaCtrl.insertarTienda(nombre, dsn);
		}else if (operacion ==2)
		{
			int idtienda= Integer.parseInt(request.getParameter("idtienda"));
			String nombre = request.getParameter("nombre");
			String dsn = request.getParameter("dsn");
			String alertarPedidos = request.getParameter("alertarpedidos");
			respuesta = tiendaCtrl.editarTienda(idtienda, nombre, dsn, alertarPedidos);
		}else if (operacion ==3 )
		{
			int idtienda = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = tiendaCtrl.eliminarTienda(idtienda);
		}else if (operacion == 4)
		{
			int idtiecon = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = tiendaCtrl.retornarTienda(idtiecon);
		}else if(operacion == 5)
		{
			respuesta = tiendaCtrl.retornarTiendas();
		}else if(operacion == 6)
		{
			respuesta = tiendaCtrl.retornarTiendas();
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
