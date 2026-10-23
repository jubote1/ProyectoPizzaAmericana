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

import capaControladorCC.AutenticacionCtrl;
import capaModeloCC.Usuario;

/**
 * Dice si la persona que tiene la cookie de sesion sigue autenticada y que perfil tiene.
 *
 * Ruta: /ProyectoPizzaAmericana/ValidarUsuarioAplicacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Lee la sesion pero no la exige.
 * Devuelve: application/json;charset=UTF-8; claves: nombreusuario, plataforma, respuesta, roles,
 * usu.
 * Lee: rol, usuario, usuario_rol.
 *
 * Trampas y riesgos conocidos:
 * - Responde siempre 200. Un error interno se registra y se devuelve el NOK por defecto.
 * - Depende de una sesion de navegador: no sirve para una llamada maquina a maquina.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ValidarUsuarioAplicacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ValidarUsuarioAplicacion")
public class ValidarUsuarioAplicacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ValidarUsuarioAplicacion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio retorna el atributo de tipo usuario y con base en este valida si el usuario si está logueado.Se 
	 * retornan tres posibles valores NOK si la validación del usuario no es correcta, OKA si es un usuario administrador
	 * y OK si es un usuario normal
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		Logger logger = Logger.getLogger("log_file");
		HttpSession miSesion = request.getSession(false);
		Usuario usuario = (miSesion != null) ? (Usuario) miSesion.getAttribute("usuario") : null;
		String resultado = "[{\"respuesta\":\"NOK\",\"nombreusuario\":\"\",\"plataforma\":\"\"}]";

		try {
			if (usuario != null && usuario.getNombreUsuario() != null) {
				String user = usuario.getNombreUsuario();
				logger.info("Validando validez de autenticacion de usuario " + user);
				AutenticacionCtrl aut = new AutenticacionCtrl();
				resultado = aut.validarAutenticacion(user);
				logger.info("resultado de validación de autenticación de usuario " + user + " " + resultado);
			} else {
				logger.info("No hay usuario autenticado en la sesión actual.");
			}
		} catch(Exception e) {
			logger.error("Error validando usuario en aplicación: " + e.toString(), e);
		}
		PrintWriter out = response.getWriter();
		out.write(resultado);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
