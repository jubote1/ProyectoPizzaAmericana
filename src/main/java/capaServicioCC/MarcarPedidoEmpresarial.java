package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;

/**
 * Servlet implementation class FinalizarPedido
 * Servicio que se encarga de cerrar un pedido, totalizar el valor del pedido,y cambiar el estado del pedido a finalizado.
 * 
 */
@WebServlet("/MarcarPedidoEmpresarial")
public class MarcarPedidoEmpresarial extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MarcarPedidoEmpresarial() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		int idPedido;
		try
		{
			idPedido = Integer.parseInt(request.getParameter("idpedido"));
		}catch(Exception e)
		{
			idPedido = 0;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.marcarPedidoEmpresarial(idPedido);
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