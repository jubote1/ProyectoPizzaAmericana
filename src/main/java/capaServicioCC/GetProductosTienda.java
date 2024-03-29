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
 * Servlet implementation class GetOtrosProductos
 * Método que retorna todos los productos parametrizados en el sistema y con base en estos se toma el pedido, esta información
 * es retornada en formato JSON.
 */
@WebServlet("/GetProductosTienda")
public class GetProductosTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetProductosTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros se encarga de retornar en formato JSON todos los productos parametrizados en el
	 * sistema invocando al método obtenerTodosProductos de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				System.out.println(e.toString());
				idtienda = 0;
			}
			String plataforma = request.getParameter("plataforma");
			if(plataforma == null )
			{
				plataforma = "N";
			}
			if(plataforma.equals(new String("")))
			{
				plataforma = "N";
			}
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.GetProductosTiendaPlat(idtienda, plataforma);
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
