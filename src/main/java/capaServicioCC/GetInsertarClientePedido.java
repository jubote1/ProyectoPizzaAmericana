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

/**
 * Servlet implementation class GetInsertarClientePedido
 * Servicio que se encarga de recibir los parámetros de cliente con el fin de tomar alguna acción, que podrá ser actualización
 * o inserción. Se reciben todo lso parámetos del cliente.
 */
@WebServlet("/GetInsertarClientePedido")
public class GetInsertarClientePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetInsertarClientePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los parámetros de cliente y realiza la utilización de método en capa Cliente Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		String telefono = request.getParameter("telefono");
        String nombres = request.getParameter("nombres");
        String direccion = request.getParameter("direccion");
        String zona = request.getParameter("zona");
        String observacion = request.getParameter("observacion");
        String tienda = request.getParameter("tienda");
        ClienteCtrl ClienCtrl = new ClienteCtrl();
        String respuesta = ClienCtrl.InsertarClientePedido(telefono, nombres, direccion, zona, observacion, tienda);
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
