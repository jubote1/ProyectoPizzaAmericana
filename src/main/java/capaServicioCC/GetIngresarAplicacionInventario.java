package capaServicioCC;

import java.io.IOException;
import java.io.IOException.*;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.AutenticacionCtrl;
import capaModeloCC.Usuario;
/**
 * Servlet implementation class IngresarAplicacion
 * Servicio utilizado para el logueo de la aplicación, recibiendo usuario y password, validando el resultado de la operacion
 * y retornando un resultado para que el sistema interprete y continue la carga de la interface de pedidos.
 */
@WebServlet("/GetIngresarAplicacionInventario")
public class GetIngresarAplicacionInventario extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
  

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio para el logueo a la aplicación recibiendo los parámetros de usuario y login, e invocando al método autenticarUsuario 
	 * de la capa Autenticacion controlador, si el proceso es exitoso, se creará un objeto tipo usuario y se dejará dentro de la sesión.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		HttpSession sesion = request.getSession(true);
		String user = request.getParameter("nombre");
        String pass = request.getParameter("password");
        AutenticacionCtrl aut = new AutenticacionCtrl();
        logger.info("Solicitud Logueo del usuario " + user);
        //Esperamos en esta variable manejar el idEmpleado;
        String resultado = aut.autenticarUsuarioInventario(user, pass);
        PrintWriter out = response.getWriter();
        out.write(resultado);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
		
	}
}
