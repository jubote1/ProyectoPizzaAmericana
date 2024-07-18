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
import capaModeloCC.Usuario;

/**
 * Servlet implementation class CRUDFormaPago
 * Servicio que implementa el CRUD de la entidad Forma Pago.
 */
@WebServlet("/CRUDTiempoPedido")
public class CRUDTiempoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDTiempoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibirá como parámetro principal el idoperacion con base en los siguientes valores 
	 *  idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar, de acuerdo a la id operacion se pedirán los otros parámetros y se invocará el método en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 actualizar 2 consultar
		response.addHeader("Access-Control-Allow-Origin", "*");		
		HttpSession sesion = request.getSession();
		//Capturamos la información de la sesion del usuario que está ejecutando la inserción
		Usuario usuario = (Usuario) sesion.getAttribute("usuario");
		String user = "" ;
		//Al no existir el usuario logueado es posible que produza una excepcion
		try
		{
			user = usuario.getNombreUsuario();
		}catch(Exception e)
		{
		
		}
		String idoperacion = request.getParameter("idoperacion");
		ParametrosCtrl ParametrosCtrl = new ParametrosCtrl();
		int operacion;
		String respuesta="";
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			int nuevotiempo = Integer.parseInt(request.getParameter("nuevotiempo"));
			int idtienda = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = ParametrosCtrl.actualizarTiempoPedido(nuevotiempo,idtienda, user);
		}else if (operacion ==2)
		{
			respuesta = ParametrosCtrl.retornarTiempoPedido();
		}else if (operacion ==3)
		{
			int idtienda = Integer.parseInt(request.getParameter("idtienda"));
			respuesta = ParametrosCtrl.retornarTiempoPedidoTienda(idtienda);
		}
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
