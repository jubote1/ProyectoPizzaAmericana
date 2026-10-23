package capaSeguridad.filtro;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Obliga al navegador a PREGUNTAR antes de usar un .js, .css o .html que ya tenga
 * guardado.
 *
 * EL PROBLEMA QUE RESUELVE
 *
 * Cada pantalla trae su script con un numero de version a mano -por ejemplo
 * "js/pedidos.js?v=49"-. Mientras ese numero no cambie, el navegador sirve el archivo
 * que ya tiene y NO vuelve a pedirlo, por mas que el servidor tenga uno nuevo. Eso
 * funciona solo si quien cambia el .js se acuerda de subirle el numero al .html, y
 * basta con que se olvide una vez para que un cambio quede invisible.
 *
 * Paso de verdad: la pregunta de la campana de Cinepolis se agrego a pedidos.js el
 * 2026-10-20 sin tocar el "?v=49" del Pedidos.html. De 32 pedidos del contact center
 * que tenian el Combo Insuperable en esos dias, la pregunta salio en UNO. Los demas
 * agentes siguieron con el archivo viejo, y a esos clientes nadie les ofrecio la boleta.
 *
 * COMO LO RESUELVE
 *
 * "no-cache" NO quiere decir "no guardes": quiere decir "guardalo, pero preguntame
 * antes de usarlo". El navegador manda la peticion con la fecha o el ETag de lo que
 * tiene, y si no cambio nada Tomcat responde 304 sin cuerpo. O sea que el costo es una
 * peticion vacia por archivo, y a cambio un despliegue se ve de una, sin que nadie
 * tenga que borrar cache ni entrar en modo incognito.
 *
 * Los numeros de version pueden quedarse donde estan: ya no hacen falta, pero tampoco
 * estorban.
 */
public class NoCacheEstaticosFilter implements Filter {

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		if (response instanceof HttpServletResponse) {
			final HttpServletResponse http = (HttpServletResponse) response;
			// must-revalidate es para los proxys del camino; max-age=0 es para los
			// navegadores viejos que ignoran no-cache.
			http.setHeader("Cache-Control", "no-cache, must-revalidate, max-age=0");
			// Pragma y Expires son para HTTP/1.0; no estorban y cubren casos raros.
			http.setHeader("Pragma", "no-cache");
			http.setDateHeader("Expires", 0L);
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
