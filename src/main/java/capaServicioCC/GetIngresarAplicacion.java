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
 * Inicia sesion (login) con usuario y contrasena.
 *
 * Ruta: /ProyectoPizzaAmericana/GetIngresarAplicacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): txtUsuario (string), txtPassword (string).
 * Devuelve: texto.
 * Lee: rol, usuario, usuario_rol.
 *
 * Trampas y riesgos conocidos:
 * - La contrasena viaja en la URL (por GET) y por lo tanto queda en los registros de acceso del
 *   servidor y del navegador.
 * - La consulta compara password = ? con la clave tal como llega: no se ve ninguna funcion de
 *   hash en el servidor.
 * - En el codigo leido no se ve bloqueo por intentos fallidos.
 * - Acepta GET y POST.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetIngresarAplicacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetIngresarAplicacion")
public class GetIngresarAplicacion extends HttpServlet {
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
		String user = request.getParameter("txtUsuario");
        String pass = request.getParameter("txtPassword");
        AutenticacionCtrl aut = new AutenticacionCtrl();
        logger.info("Solicitud Logueo del usuario " + user);
        //Esperamos en esta variable manejar el idEmpleado;
        int idEmpleado = 0;
        boolean resultado = aut.autenticarUsuario(user, pass);
        PrintWriter out = response.getWriter();
        if (resultado){
        		Usuario usuario = aut.obtenerUsuarioParaSesion(user);
        		sesion.setAttribute("usuario", usuario);
        		sesion.setMaxInactiveInterval(60*20);
        		logger.info("El logueo del usuario " + user + " ha sido satisfactorio");
        		out.write("OK");
        		//response.sendRedirect("http://localhost:8080/ProyectoPizzaAmericana/Pedidos.html");
        }
        else{
        	logger.error("El logueo del usuario " + user + "no ha sido satisfactorio");
        	out.write("Error en el Logueo, favor verifique usuario y contraseña");
        	
        }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
		
	}
}
