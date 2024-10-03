package capaSeguridad.filtro;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Permitir el origen específico
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost/pizzaamericana");

        // Permitir los métodos HTTP necesarios
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // Permitir los encabezados necesarios
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        // Si se necesitan credenciales, habilitarlo
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        // Verificar si es una solicitud preflight (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            // En las solicitudes preflight, puedes devolver inmediatamente el estado 200
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return; // Terminar el proceso aquí para solicitudes preflight
        }

        // Continuar con los demás filtros si no es preflight
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() { }
}
