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
 * Servlet implementation class ConsultaIntegradaPedidos
 */
@WebServlet("/ConsultarAplicabilidadPedidoRAPPICARGO")
public class ConsultarAplicabilidadPedidoRAPPICARGO extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarAplicabilidadPedidoRAPPICARGO() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como par�metros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos par�metros, se invoca en la capa controlador al m�todo ConsultaIntegradaPedidos.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession();
		PedidoCtrl consultapedido = new PedidoCtrl();
        String respuestaConsulta = consultapedido.consultarAplicabilidadPedidoRAPPICARGO();
        PrintWriter out = response.getWriter();
        //Comentamos resultado de la consulta debido a que consultas grandes pueden generar mucha informaci�n
        //logger.debug(respuestaConsulta);
		out.write(respuestaConsulta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
