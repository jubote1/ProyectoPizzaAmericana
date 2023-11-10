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
 * Servlet implementation class ObtenerHomologaGaseosaIncluida
 */
@WebServlet("/ObtenerHomologacionProductoGaseosa")
public class ObtenerHomologacionProductoGaseosa extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerHomologacionProductoGaseosa() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				System.out.println(e.toString());
				idtienda = 0;
			}
			PedidoCtrl PedCtrl = new PedidoCtrl();
			String respuesta = PedCtrl.obtenerHomologacionProductoGaseosa(idtienda);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
		}catch(Exception e){
			System.out.println(e.getMessage());
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
