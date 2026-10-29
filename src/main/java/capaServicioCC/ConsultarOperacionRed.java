package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.OperacionRedCtrl;
import capaDAOCC.PantallaDAO;
import capaDAOCC.UsuarioRolDAO;
import capaModeloCC.Usuario;

/**
 * Servicio del reporte de operacion de domiciliarios de un dia en TODAS las tiendas.
 * Invoca OperacionRedCtrl.consultar.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarOperacionRed   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Exige sesion Y permiso sobre la pantalla ReporteOperacionRed.html.
 *
 * El permiso se comprueba AQUI, no solo en el menu. Ver toda la red es justamente lo que
 * no puede hacer un administrador de punto de venta, y esconder la opcion del menu no
 * impide escribir la URL. SeguridadFilter sigue en modo LOG, asi que no se puede delegar
 * en el.
 *
 * Recibe (query/form): fecha (string, dd/MM/yyyy o yyyy-MM-dd).
 * Devuelve: application/json; charset=UTF-8; claves: fecha, tiendas, sin_respuesta, total,
 * tiendas_consultadas, datos_hasta, error.
 * Lee: lo mismo que ConsultarOperacionDomiciliarios, una vez por tienda.
 */
@WebServlet("/ConsultarOperacionRed")
public class ConsultarOperacionRed extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/** La pantalla que respalda este servicio; quien no la tenga, no lo puede llamar. */
	private static final String PATRON_URL = "/ConsultarOperacionRed";

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();
		final Logger logger = Logger.getLogger("log_file");

		final HttpSession sesion = request.getSession(false);
		final Object objUsuario = (sesion == null ? null : sesion.getAttribute("usuario"));
		if (!(objUsuario instanceof Usuario)) {
			out.write("{\"error\":\"Debe iniciar sesion\"}");
			return;
		}
		final Usuario usuario = (Usuario) objUsuario;
		//Los roles se leen de la base, no del objeto de la sesion: esa lista se congela al
		//hacer login y una sesion abierta de antes de un despliegue llega vacia.
		if (!PantallaDAO.tienePermisoParaUrl(UsuarioRolDAO.listarNombresRolPorUsuario(usuario.getId()),
				PATRON_URL)) {
			logger.info("operacion red: " + usuario.getNombreUsuario() + " no tiene permiso");
			out.write("{\"error\":\"Usted no tiene permiso para ver la operacion de todas las tiendas.\"}");
			return;
		}

		final String fecha = aFechaSql(request.getParameter("fecha"));
		if (fecha.length() == 0) {
			out.write("{\"error\":\"La fecha no tiene un formato valido\"}");
			return;
		}
		try {
			final SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
			formato.setLenient(false);
			formato.parse(fecha);
		} catch (final Exception e) {
			out.write("{\"error\":\"La fecha no es valida\"}");
			return;
		}

		logger.info("operacion red fecha=" + fecha + " pedida por " + usuario.getNombreUsuario());
		final long arranco = System.currentTimeMillis();
		out.write(OperacionRedCtrl.consultar(fecha));
		logger.info("operacion red tardo " + (System.currentTimeMillis() - arranco) + " ms");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	/** De dd/MM/yyyy (o yyyy-MM-dd) a yyyy-MM-dd; vacio si no es ninguna de las dos. */
	private static String aFechaSql(final String fecha) {
		if (fecha == null) {
			return ("");
		}
		final String limpia = fecha.trim();
		if (limpia.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return (limpia);
		}
		if (limpia.matches("\\d{2}/\\d{2}/\\d{4}")) {
			return (limpia.substring(6, 10) + "-" + limpia.substring(3, 5) + "-" + limpia.substring(0, 2));
		}
		return ("");
	}
}
