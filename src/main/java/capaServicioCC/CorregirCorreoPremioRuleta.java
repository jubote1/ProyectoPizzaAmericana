package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.RuletaCtrl;
import capaModeloCC.Usuario;

/**
 * Servicio que corrige el correo de un premio de la ruleta desde la pantalla de
 * dispersion.
 *
 * Hacia falta porque corregir el correo en la pantalla de Clientes NO cambiaba nada: la
 * consulta de dispersion se queda con resultado_ruleta.correo apenas tenga una arroba,
 * aunque este mal escrito, y nunca alcanzaba a mirar la ficha del cliente.
 *
 * Escribe en resultado_ruleta y, solo si la ficha del cliente no tiene un correo valido,
 * tambien alli. No pisa un correo bueno del cliente.
 *
 * Ruta: /ProyectoPizzaAmericana/CorregirCorreoPremioRuleta   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Actualizacion.  Acceso: Exige sesion de usuario.
 * Recibe (query/form): idresultado (integer), correo (string).
 * Devuelve: application/json; charset=UTF-8; claves: resultado, mensaje, correo, toco_cliente.
 * Escribe: resultado_ruleta, cliente, log_envio_premio_ruleta.
 */
@WebServlet("/CorregirCorreoPremioRuleta")
public class CorregirCorreoPremioRuleta extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		final PrintWriter out = response.getWriter();

		//Se exige sesion y se toma el usuario de ahi, no de un parametro: el registro de
		//quien corrigio un correo no sirve de nada si la pantalla lo puede inventar.
		final String usuario = usuarioSesion(request);
		if (usuario.length() == 0) {
			out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Debe iniciar sesion\"}");
			return;
		}

		int idResultado = 0;
		try {
			idResultado = Integer.parseInt(request.getParameter("idresultado"));
		} catch (final Exception e) {
			idResultado = 0;
		}

		final RuletaCtrl ctrl = new RuletaCtrl();
		out.write(ctrl.corregirCorreo(idResultado, request.getParameter("correo"), usuario));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private static String usuarioSesion(final HttpServletRequest request) {
		try {
			final HttpSession sesion = request.getSession(false);
			if (sesion != null && sesion.getAttribute("usuario") != null) {
				final Usuario usuarioSesion = (Usuario) sesion.getAttribute("usuario");
				return (usuarioSesion.getNombreUsuario() == null ? "" : usuarioSesion.getNombreUsuario());
			}
		} catch (final Exception e) {
			System.out.println("CorregirCorreoPremioRuleta, leyendo la sesion: " + e.toString());
		}
		return ("");
	}
}
