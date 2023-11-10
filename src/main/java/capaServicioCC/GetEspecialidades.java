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
 * Servlet implementation class GetEspecialidades
 * Servicio que retorna todas la especialidades creadas en el sistemas, no se tiene filtro y por lo tanto no toma ningún
 * parámetro o filtro, el retorno de la información es formato JSON.
 */
@WebServlet("/GetEspecialidades")
public class GetEspecialidades extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetEspecialidades() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio no toma ningún parámetro y retorna todas las especialidades en formato JSON, invocando al método
	 * obtenerEspecialidades() de la capa Controlador Pedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			int idExcepcion = 0;
			try {
				idExcepcion = Integer.parseInt(request.getParameter("idexcepcion"));
			} catch (Exception e) {
				idExcepcion = 0;
			}
			int idProducto = 0;
			try {
				idProducto = Integer.parseInt(request.getParameter("idproducto"));
			} catch (Exception e) {
				idProducto = 0;
			}
			String respuesta = PedidoCtrl.obtenerEspecialidades(idExcepcion, idProducto);
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
