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
 * Servlet implementation class GetAdicionProducto
 * Servicio que se encarga de retornar en formato JSON todas los productos con la marca de tipo adicion, cone el fin
 * de que la capa de presentación se encargue de desplegarlos según sea la necesidad.
 */ 
@WebServlet("/GetAdicionProducto")
public class GetAdicionProducto extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetAdicionProducto() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El método GET no recibe parámetros, pues retorna todos los productos tipo adición, invocando a la capa Pedido
	 * controlador al método obtenerAdicionProductos();
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.obtenerAdicionProductos();
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
