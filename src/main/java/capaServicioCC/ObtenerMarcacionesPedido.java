package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.TiendaCtrl;

/**
 * Servlet implementation class ObtenerFormaPagoPedido
 * Servicio que se encarga de retornar las formas de pago parametrizadas en el sistema, en formato JSON.
 */
@WebServlet("/ObtenerMarcacionesPedido")
public class ObtenerMarcacionesPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerMarcacionesPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros y se encarga de retornar en formato JSON todas las formas de pago
	 * parametrizadas en el sistema, invocando al método obtenerFormaPagoPedido de la capa Parametros controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.obtenerMarcacionesPedido(idpedido);
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
