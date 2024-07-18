package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class GetOtrosProductos
 * M�todo que retorna todos los productos parametrizados en el sistema y con base en estos se toma el pedido, esta informaci�n
 * es retornada en formato JSON.
 */
@WebServlet("/ObtenerClienteAlerta")
public class ObtenerClienteAlerta extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerClienteAlerta() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe par�metros se encarga de retornar en formato JSON todos los productos parametrizados en el
	 * sistema invocando al m�todo obtenerTodosProductos de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			String telefono;
			try
			{
				telefono = request.getParameter("telefono");
			}catch(Exception e)
			{
				System.out.println(e.toString());
				telefono = "";
			}
			ClienteCtrl clienteCtrl = new ClienteCtrl();
			String respuesta = clienteCtrl.obtenerClienteAlerta(telefono);
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
