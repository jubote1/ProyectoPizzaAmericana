package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class ActualizarNumeroPedidoPixel Servicio que se encarga de actualizar el n�mero de pedido que retorna la tienda PIXEL, luego de insertado un pedido.
 */
@WebServlet("/ObtenerEstadoEnviadoPixel")
public class ObtenerEstadoEnviadoPixel extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerEstadoEnviadoPixel() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * M�todo GET del Servicio que recibe como par�metroel idpedido del sistema Contact Center y el numPedidoPixel el cual es el 
	 * n�mero de pedido Pixel que retorno la inserci�n, se invoca en al capa controlador el m�todo actualizarEstadoNumeroPedidoPixel.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				HttpSession sesion = request.getSession();
				int idpedido = Integer.parseInt(request.getParameter("idpedido"));
				PedidoCtrl PedidoCtrl = new PedidoCtrl();
		        String respuesta = PedidoCtrl.obtenerEstadoEnviadoPixel(idpedido);
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
