package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;

/**
 * Servlet implementation class DuplicarDetallePedido
 */
@WebServlet("/DuplicarDetallePedido")
public class DuplicarDetallePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DuplicarDetallePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession();
		int iddetallepedido;
        try
        {
        	iddetallepedido = Integer.parseInt(request.getParameter("iddetallepedido"));
        }catch(Exception e)
        {
        	iddetallepedido= 0;
        }
        logger.info("Llamado a servicio con parámetros iddetallepedido " + iddetallepedido);
        PedidoCtrl pedido = new PedidoCtrl();
        String respuestaDuplicar = pedido.DuplicarDetallePedido(iddetallepedido);
        logger.debug("Respuesta duplicar de detalle pedido " + iddetallepedido + " ");
        PrintWriter out = response.getWriter();
		out.write(respuestaDuplicar);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
