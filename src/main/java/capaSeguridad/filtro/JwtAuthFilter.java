package capaSeguridad.filtro;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaSeguridad.Accesos;
import capaSeguridad.modelo.Token;

import java.io.IOException;
import java.util.Date;


//@WebFilter(servletNames = {"ObtenerVacantes", "ObtenerContenidoVacante"})
public class JwtAuthFilter implements Filter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String SECRET_KEY = System.getenv("SECRET_KEY") != null ? System.getenv("SECRET_KEY") : "";
    private static final SecretKey KEY;

    static {
		if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
           // System.out.println("WARNING: SECRET_KEY not set. JWT filter will be disabled.");
            throw new IllegalStateException("SECRET_KEY must be set in the environment variables.");
           
        } 
            KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes()); 
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;

        // Si KEY es null -> bloquear todo acceso
        if (KEY == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Servicio no disponible: Falta configuración de seguridad.");
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        String authHeader = req.getHeader(AUTH_HEADER);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(KEY)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                String session = (String) claims.get("session");
                Token dbAccessToken = Accesos.getValidAccessToken(username, token, session);

                if (dbAccessToken == null) {
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }

                chain.doFilter(request, response);

            } catch (Exception e) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            }
        } else {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
        }
    }

}

