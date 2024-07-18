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
import capaModeloCC.PedidoPrecioEmpleado;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class InsertarEncabezadoPedido
 * Servicio que se encarga de insertar el encabezado de un pedido, se retorna en formato JSON el dato esperado que 
 * corresponde al idpedido.
 */
@WebServlet("/InsertarPedidoPrecioEmpleado")
public class InsertarPedidoPrecioEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarPedidoPrecioEmpleado() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro la información más básica de un pedido como lo son, tinda, idcliente, 
	 * fechapedido, con estos tres hace una inserción del encabezado pedido y retorna en formato JSON el idpedido que se
	 * manejará para el pedido. Esta labor se realiza invocando el método InsertarEncabezadoPedido de la capa Pedido
	 * Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		//Capturamos la información de la sesion del usuario que está ejecutando la inserción
		int idEmpleado = Integer.parseInt(request.getParameter("idempleado"));
		String fecha = request.getParameter("fecha");		
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		double valor = Double.parseDouble(request.getParameter("valor"));
		int idPedido = Integer.parseInt(request.getParameter("idpedido"));
		String autorizado = "N";
		String codigo = "";
		PedidoPrecioEmpleado pedPrecio = new PedidoPrecioEmpleado(idEmpleado, fecha, idTienda, valor, idPedido, autorizado,codigo);
        PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta = pedCtrl.insertarPedidoPrecioEmpleado(pedPrecio);
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
