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
 * Servlet implementation class GetSaboresLiquidoProducto
 * Servicio que recibe un idproducto y con base en este retorna los liquidos asociados al producto en formato JSON.
 */
@WebServlet("/GetSaboresLiquidoProducto")
public class GetSaboresLiquidoProducto extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetSaboresLiquidoProducto() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El Servicio recibe como parámetro el idproducto y con base en este invocando al método ObtenerSaboresLiquidoProducto 
	 * de la capa Pedido controlador, retorna los sabores tipo liquido en formato JSON, con el objetivo de que la capa
	 * de presentación se encargue de pintarlos, según la elección del pedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			String idProducto = request.getParameter("idProducto");
			int idProdu = Integer.parseInt(idProducto);
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				idtienda = 0;
			}
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.ObtenerSaboresLiquidoProducto(idProdu, idtienda);
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
