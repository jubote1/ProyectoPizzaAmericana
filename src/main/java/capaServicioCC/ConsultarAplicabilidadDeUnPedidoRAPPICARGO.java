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
@WebServlet("/ConsultarAplicabilidadDeUnPedidoRAPPICARGO")
public class ConsultarAplicabilidadDeUnPedidoRAPPICARGO extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarAplicabilidadDeUnPedidoRAPPICARGO() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio se encarga de recibir como par�metros una fecha inicial, una fecha final y una tienda, esto con el fin de consultar los pedidos
	 * tomados bajo estos par�metros, se invoca en la capa controlador al m�todo ConsultaIntegradaPedidos.
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Logger logger = Logger.getLogger("log_file");
        PedidoCtrl consultapedido = new PedidoCtrl();

		int idpedido = 0;
		int idtienda = 0;
		int numposheader = 0;

		try {
		    if (request.getParameter("idpedido") != null) {
		        idpedido = Integer.parseInt(request.getParameter("idpedido"));
		    }
		} catch (Exception e) {
		    idpedido = 0;
		}

		try {
		    if (request.getParameter("idtienda") != null) {
		    	idtienda = Integer.parseInt(request.getParameter("idtienda"));
		    }
		} catch (Exception e) {
			idtienda = 0;
		}
		
		try {
		    if (request.getParameter("numposheader") != null) {
		    	numposheader = Integer.parseInt(request.getParameter("numposheader"));
		    }
		} catch (Exception e) {
			numposheader = 0;
		}
		

        String respuestaConsulta =
                consultapedido.consultarAplicabilidadPedidoRAPPICARGO(idpedido, numposheader, idtienda);

        PrintWriter out = response.getWriter();
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
