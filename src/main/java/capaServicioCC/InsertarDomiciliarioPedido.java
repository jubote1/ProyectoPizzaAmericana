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
import capaModeloCC.DomiciliarioPedido;

/**
 * Servlet implementation class InsertarDespachoTienda
 */
@WebServlet("/InsertarDomiciliarioPedido")
/**
 * Servlet que tiene como objetivo la INserción del encabezado de un despacho tienda.
 * @author JuanDavid
 *
 */
public class InsertarDomiciliarioPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDomiciliarioPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession(true);
		String fecha = request.getParameter("fecha");
        int idTienda;
        try
        {
        	idTienda = Integer.parseInt(request.getParameter("idtienda"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	idTienda = 0;
        }
        int idUsuario;
        try
        {
        	idUsuario = Integer.parseInt(request.getParameter("idusuario"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	idUsuario = 0;
        }
        int cantidad;
        try
        {
        	cantidad = Integer.parseInt(request.getParameter("cantidad"));
        	
        }catch(Exception e)
        {
        	logger.error(e.toString());
        	cantidad = 0;
        }
        PedidoCtrl pedCtrl = new PedidoCtrl();
        DomiciliarioPedido domPed = new DomiciliarioPedido(idUsuario,fecha, idTienda, cantidad);
        pedCtrl.insertarDomiciliarioPedido(domPed);
        String respuesta = "OK";
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
