package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EmpleadoCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class ObtenerTotalPedido
 * Servicio que permite dado un id de pedido, retornar el total de un pedido, sumando los detalles de pedido asociados
 * al pedido.
 */
@WebServlet("/ObtenerResultadoEncuesta")
public class ObtenerResultadoEncuesta extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerResultadoEncuesta() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el idpedido correspondiente y se encarga de retornar el total del pedido,
	 * sumando los detalles de pedidos asociados al pedido, esto invocando al método obtenerTotalPedido de la capa
	 * Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		EmpleadoCtrl empCtrl = new EmpleadoCtrl();
        String respuesta = empCtrl.obtenerResultadoEncuesta(idTienda,4);
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
