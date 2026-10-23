package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;;

/**
 * Servlet implementation class GetCliente
 * Servicio que se encarga de consultar todos los registros que tiene asociado un cliente en la tabla de clientes, dando 
 * como parámetro un teléfono determinado.
 */
@WebServlet("/RealizarCambiosPedido")
public class RealizarCambiosPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RealizarCambiosPedido() {
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
		response.setContentType("application/json");
		try{
			int idTiendaNueva = 0;
			try {
				if (request.getParameter("idtienda") != null && !request.getParameter("idtienda").trim().isEmpty()) {
					idTiendaNueva = Integer.parseInt(request.getParameter("idtienda").trim());
				}
			} catch (Exception ex) {}

			int idPedido = 0;
			try {
				if (request.getParameter("idpedido") != null && !request.getParameter("idpedido").trim().isEmpty()) {
					idPedido = Integer.parseInt(request.getParameter("idpedido").trim());
				}
			} catch (Exception ex) {}

			int idCliente = 0;
			try {
				if (request.getParameter("idcliente") != null && !request.getParameter("idcliente").trim().isEmpty()) {
					idCliente = Integer.parseInt(request.getParameter("idcliente").trim());
				}
			} catch (Exception ex) {}

			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.realizarCambioPedido(idTiendaNueva, idPedido, idCliente);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			out.flush();
		}catch(Exception e){
			System.out.println("Error en RealizarCambiosPedido: " + e.getMessage());
			PrintWriter out = response.getWriter();
			out.write("{\"resultado\":\"ERROR\"}");
			out.flush();
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
