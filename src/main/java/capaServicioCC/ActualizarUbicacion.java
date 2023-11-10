package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.GeolocalizacionCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class FinalizarPedido
 * Servicio que se encarga de cerrar un pedido, totalizar el valor del pedido,y cambiar el estado del pedido a finalizado.
 * 
 */
@WebServlet("/ActualizarUbicacion")
public class ActualizarUbicacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarUbicacion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocará el método de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		String claveDomiciliario = request.getParameter("clave_dom");
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		float latitud, longitud;
		try
		{
			latitud = Float.parseFloat(request.getParameter("latitud"));
		}catch(Exception e)
		{
			latitud = 0;
		}
		try
		{
			longitud = Float.parseFloat(request.getParameter("longitud"));
		}catch(Exception e)
		{
			longitud = 0;
		}
		GeolocalizacionCtrl geoCtrl = new GeolocalizacionCtrl();
        String respuesta = geoCtrl.insertarUbicacionDomiciliario(claveDomiciliario, idTienda, latitud, longitud);
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
