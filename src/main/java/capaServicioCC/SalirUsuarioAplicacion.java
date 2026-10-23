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
 * Cierra la sesion del usuario.
 *
 * Ruta: /ProyectoPizzaAmericana/SalirUsuarioAplicacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: texto.
 *
 * Trampas y riesgos conocidos:
 * - Se puede llamar por GET; cualquier pagina externa podria cerrar la sesion de quien la
 *   visite.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha SalirUsuarioAplicacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/SalirUsuarioAplicacion")
public class SalirUsuarioAplicacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SalirUsuarioAplicacion() {
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
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				Logger logger = Logger.getLogger("log_file");
				HttpSession miSesion = (HttpSession) request.getSession();
				miSesion.setAttribute("usuario", null);
				String resultado ="" ;
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
