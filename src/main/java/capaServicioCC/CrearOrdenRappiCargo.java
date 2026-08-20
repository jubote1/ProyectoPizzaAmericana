package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.TercerizadoDomicilioCtrl;

@WebServlet("/CrearOrdenRappiCargo")
public class CrearOrdenRappiCargo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public CrearOrdenRappiCargo() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;charset=UTF-8");

		Logger logger = Logger.getLogger("log_file");
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
		

		TercerizadoDomicilioCtrl terceridadoCtrl =
		        new TercerizadoDomicilioCtrl();

		String respuesta = terceridadoCtrl.crearOrdenRappiCargo(
		        idpedido,
		        idtienda,
		        numposheader
		);

		PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
