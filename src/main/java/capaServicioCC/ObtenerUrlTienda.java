package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.TiendaCtrl;

/**
 * Servlet implementation class ObtenerUrlTienda
 * Este servicio nos permite dado un idtienda, retornar el URL Tienda, el cual es la base para invocar el servicio que
 * se encarga de insertar el pedido de la tienda.
 */
@WebServlet("/ObtenerUrlTienda")
public class ObtenerUrlTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerUrlTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el idtienda y con base en este se retorna la URL tienda que será la base, para 
	 * invocar los servicios para la inserción del pedido en la tienda.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				int idtienda = Integer.parseInt(request.getParameter("idtienda"));
				TiendaCtrl TiendaCtrl = new TiendaCtrl();
		        String respuesta = TiendaCtrl.obtenerUrlTienda(idtienda);
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
