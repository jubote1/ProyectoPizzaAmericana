package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.HomologacionSalesManago;

/**
 * Servicio que se encarga de retornar el precio adicional que tendran definidas especialidades. El servicio retorna en formato 
 * JSON el valor que se requiere.
 * Servlet implementation class ObtenerExcepcionEspecialidad
 */
@WebServlet("/ObtenerHomologacionSalesManago")
public class ObtenerHomologacionSalesManago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerHomologacionSalesManago() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		String tamano = request.getParameter("tamano");
		if(tamano == null)
		{
			tamano = "";
		}
		String name = request.getParameter("name");
		if(name == null)
		{
			name = "";
		}
		String especialidad = request.getParameter("especialidad");
		if(especialidad == null)
		{
			especialidad = "";
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		HomologacionSalesManago homol = new HomologacionSalesManago(tamano,name,especialidad);
        String respuesta = pedCtrl.obtenerHomologacionSalesManago(homol);
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
