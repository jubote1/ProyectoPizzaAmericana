package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.Usuario;

/**
 * Servlet implementation class InsertarEspecialidad
 * Método que se encarga recibir los parámetros para la inserción de una especialidad.
 */
@WebServlet("/IngresarObsGestionLink")
public class IngresarObsGestionLink extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public IngresarObsGestionLink() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los parámetros para llamar al método insertarEspecialidad de la capa Parametros controlador,
	 * e insertar la especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json; charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		//La observacion la firma quien esta en sesion. Antes quedaba sin autor y sin
		//fecha, asi que no se podia saber si a un pedido lo habian llamado ni quien.
		HttpSession sesion = request.getSession(false);
		Usuario usuario = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");
		if (usuario == null) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"La sesion se vencio. Vuelva a entrar.\"}");
			return;
		}
		int idPedido;
		try {
			idPedido = Integer.parseInt(request.getParameter("idpedido"));
		} catch (Exception e) {
			idPedido = 0;
		}
		if (idPedido <= 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"No se indico el pedido.\"}");
			return;
		}
		String observacion = request.getParameter("observacion");
		PedidoCtrl pedCtrl = new PedidoCtrl();
		out.write(pedCtrl.ingresarObsGestionLink(idPedido, observacion, usuario.getNombreUsuario()));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
