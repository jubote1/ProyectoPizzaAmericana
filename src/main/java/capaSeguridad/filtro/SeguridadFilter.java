package capaSeguridad.filtro;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaDAOCC.PantallaDAO;
import capaDAOCC.ParametrosDAO;
import capaModeloCC.Usuario;

/**
 * Reemplaza la "seguridad" actual (ocultar botones en un menu estatico segun
 * usuario.administrador) por una verificacion real en el servidor: si una URL
 * esta mapeada a una pantalla (pantalla_servlet), solo deja pasar si hay
 * sesion y el rol del usuario tiene esa pantalla asignada (rol_pantalla).
 *
 * DOS decisiones de diseno importantes, no accidentales:
 *
 * 1) Solo se evalua lo que YA esta mapeado en pantalla_servlet. De los ~280
 *    servlets de capaServicioCC, la mayoria son consumidos por el POS o la
 *    tablet -clientes sin sesion de navegador, ver el comentario de
 *    session-timeout en web.xml- y jamas deben quedar detras de un chequeo
 *    de sesion web. Una URL sin mapear pasa siempre, sea cual sea el modo:
 *    "sin mapear" no es lo mismo que "sin permiso". Solo se agregan a
 *    pantalla_servlet los endpoints que de verdad respaldan una pantalla del
 *    menu administrativo (ver sql/seguridad_roles_catalogo.sql).
 *
 * 2) Nunca falla cerrado. Cualquier excepcion (BD caida, bug del filtro)
 *    termina dejando pasar la request -igual que el comportamiento de hoy,
 *    que es cero verificacion-, nunca tumbando el sistema completo. El modo
 *    ENFORCE recien se activa cuando el mapeo este confiable.
 */
public class SeguridadFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger("log_file");

	/** SEGURIDADFILTROMODO en general.parametros: LOG (audita, nunca bloquea) o ENFORCE (bloquea de verdad). */
	private static final String PARAM_MODO = "SEGURIDADFILTROMODO";

	private static final String MODO_ENFORCE = "ENFORCE";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		try {
			String path = req.getServletPath();
			if (esRecursoEstatico(path)) {
				chain.doFilter(request, response);
				return;
			}

			if (!PantallaDAO.existeMapeoParaUrl(path)) {
				// No mapeado a proposito: no se toca (podria ser trafico de POS/tablet
				// sin sesion de navegador, o una pantalla que aun no se ha catalogado).
				chain.doFilter(request, response);
				return;
			}

			boolean enforce = MODO_ENFORCE.equalsIgnoreCase(obtenerModo());
			HttpSession sesion = req.getSession(false);
			Usuario usuario = (sesion == null) ? null : (Usuario) sesion.getAttribute("usuario");

			if (usuario == null) {
				LOGGER.info("[SeguridadFilter][" + (enforce ? "ENFORCE" : "LOG") + "] sin sesion para " + path);
				if (enforce) {
					res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sesion requerida");
					return;
				}
				chain.doFilter(request, response);
				return;
			}

			List<String> roles = usuario.getRoles();
			boolean permitido = PantallaDAO.tienePermisoParaUrl(roles, path);
			if (!permitido) {
				LOGGER.info("[SeguridadFilter][" + (enforce ? "ENFORCE" : "LOG") + "] usuario "
						+ usuario.getNombreUsuario() + " (roles " + roles + ") sin permiso para " + path);
				if (enforce) {
					res.sendError(HttpServletResponse.SC_FORBIDDEN, "No autorizado");
					return;
				}
			}
			chain.doFilter(request, response);
		} catch (Exception e) {
			// Fallar abierto: un bug o una BD caida en el filtro nunca debe tumbar
			// la aplicacion completa. Hoy no hay ninguna verificacion; un filtro
			// que fallara cerrado seria estrictamente peor que no tener filtro.
			LOGGER.error("SeguridadFilter fallo, se deja pasar la request: " + e, e);
			chain.doFilter(request, response);
		}
	}

	private String obtenerModo() {
		try {
			String modo = ParametrosDAO.retornarValorAlfanumerico(PARAM_MODO);
			return ((modo == null || modo.trim().isEmpty()) ? "LOG" : modo.trim());
		} catch (Exception e) {
			return ("LOG");
		}
	}

	private boolean esRecursoEstatico(String path) {
		if (path == null) {
			return (true);
		}
		String p = path.toLowerCase();
		return (p.endsWith(".html") || p.endsWith(".htm") || p.endsWith(".jsp") || p.endsWith(".js")
				|| p.endsWith(".css") || p.endsWith(".png") || p.endsWith(".jpg") || p.endsWith(".jpeg")
				|| p.endsWith(".gif") || p.endsWith(".svg") || p.endsWith(".ico") || p.endsWith(".woff")
				|| p.endsWith(".woff2") || p.endsWith(".ttf") || p.endsWith(".map"));
	}

	@Override
	public void destroy() {
	}

}
