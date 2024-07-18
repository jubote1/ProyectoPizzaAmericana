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
 * Servlet implementation class InsertarModificadorDetallePedido
 * Este servicio se encarga de la inserción de lo llamado como modificadores de pedido, los cuales se refieren aquellos, 
 * productos con y sin que se le desean agregar a un producto.
 */
@WebServlet("/InsertarModificadorDetallePedido")
public class InsertarModificadorDetallePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarModificadorDetallePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetros el iddetalle pedido padre al cual va asociado el modificador, adicionalmente
	 * se recibe para que mitad y en que cantidad es la modificación. Lo anterior invocando al método InsertarModificadorDetallePedido
	 * de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession();
		int iddetallepedidopadre = Integer.parseInt(request.getParameter("iddetallepedidopadre"));
        int  idproductoespecialidad1;
        int  idproductoespecialidad2;
        try
        {
        	idproductoespecialidad1 = Integer.parseInt(request.getParameter("idproductoespecialidad1"));
        }catch(Exception e)
        {
        	idproductoespecialidad1 = 0;
        }
        try
        {
        	idproductoespecialidad2 = Integer.parseInt(request.getParameter("idproductoespecialidad2"));
        }catch(Exception e)
        {
        	idproductoespecialidad2 = 0;
        }
        double cantidad;
        try
        {
        	cantidad =  Double.parseDouble(request.getParameter("cantidad"));
        }catch(Exception e)
        {
        	cantidad = 0;
        }
        int iddetallepedido;
        try
        {
        	iddetallepedido =  Integer.parseInt(request.getParameter("iddetallepedido"));
        }catch(Exception e)
        {
        	iddetallepedido = 0;
        }
        logger.info("Llamado a servicio InsertarModificadorDetallePedido con parámetros iddetallepedidopadre: "
        		+ iddetallepedidopadre +  " idproductoespecialidad1: " + idproductoespecialidad1
        		+ " idproductoespecialidad2: " + idproductoespecialidad2 + " cantidad: " + cantidad );
        PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.InsertarModificadorDetallePedido(iddetallepedidopadre, idproductoespecialidad1, idproductoespecialidad2, cantidad, iddetallepedido);
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
