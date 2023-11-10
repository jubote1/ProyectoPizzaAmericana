package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.GeolocalizacionCtrl;
import capaControladorCC.TiendaCtrl;
/**
 * Servlet implementation class GetTiendas
 * Servicio que se encarga de retornar las tiendas o puntos de venta para los cuales se podrá tomar un pedido, 
 * la información será retornada en formato JSON.
 */
@WebServlet("/GetCoordenadasPoligono")
public class GetCoordenadasPoligono extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetCoordenadasPoligono() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros dado que no filtra la información, simplemente retorna en formato JSON 
	 * las tiendas o puntos de venta parametrizados en el sistema, invocando el método obtenerTiendas de la capa Tienda Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			int idPoligono = 0;
			try
			{
				idPoligono = Integer.parseInt(request.getParameter("idpoligono"));
			}
			catch(Exception e)
			{
				idPoligono = 0;
			}
			GeolocalizacionCtrl geoCtrl = new GeolocalizacionCtrl();
			String respuesta = geoCtrl.obtenerCoordenadasPoligono(idPoligono);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
