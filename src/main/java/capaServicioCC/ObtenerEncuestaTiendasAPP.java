package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.EncuestaCtrl;
import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio que se encarga de retornar el precio adicional que tendran definidas especialidades. El servicio retorna en formato 
 * JSON el valor que se requiere.
 * Servlet implementation class ObtenerExcepcionEspecialidad
 */
@WebServlet("/ObtenerEncuestaTiendasAPP")
public class ObtenerEncuestaTiendasAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerEncuestaTiendasAPP() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		int idencuesta = Integer.parseInt(request.getParameter("idencuesta"));
		String  documento = request.getParameter("documento");
		EncuestaCtrl encCtrl = new EncuestaCtrl();
        String respuesta = encCtrl.obtenerEncuestaLaboral(idencuesta,documento);
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
