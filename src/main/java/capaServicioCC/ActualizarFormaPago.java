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
import capaControladorCC.PedidoCtrl;;

/**
 * Servlet implementation class GetCliente
 * Servicio que se encarga de consultar todos los registros que tiene asociado un cliente en la tabla de clientes, dando 
 * como parámetro un teléfono determinado.
 */
@WebServlet("/ActualizarFormaPago")
public class ActualizarFormaPago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarFormaPago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro del teléfono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el teléfono indicado.
	 * Lo anterior invocando el método obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			HttpSession sesion = request.getSession();
			int idFormaPagoNueva = Integer.parseInt(request.getParameter("idformapagonueva"));
			int idPedido = Integer.parseInt(request.getParameter("idpedido"));
			double valorFormaPagoNueva = Double.parseDouble(request.getParameter("valorformapagonueva"));
			double valorTotal = Double.parseDouble(request.getParameter("valortotal"));
			boolean virtualOrigen = Boolean.parseBoolean(request.getParameter("virtualorigen"));
			boolean virtualDestino = Boolean.parseBoolean(request.getParameter("virtualdestino"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.actualizarFormaPago(idPedido, idFormaPagoNueva, valorFormaPagoNueva, valorTotal, virtualOrigen, virtualDestino);
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
