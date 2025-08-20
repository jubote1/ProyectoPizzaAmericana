package capaSeguridad.filtro;


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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.io.IOException;
import java.util.Date;


//@WebFilter(servletNames = {"ObtenerVacantes", "ObtenerContenidoVacante"})
public class JwtAuthFilter implements Filter {

    private static final String AUTH_HEADER = "Authorization";
    private static final SecretKey KEY;

    static {
        String secret = System.getenv("SECRET_KEY");
        if (secret == null || secret.isEmpty()) {
            // Si no hay clave, la aplicación no debería arrancar
            throw new IllegalStateException("ERROR: SECRET_KEY no está definida en las variables de entorno.");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String authHeader = req.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        try {
        	Claims claims = Jwts.parserBuilder()
        	        .setSigningKey(KEY)
        	        .build()
        	        .parseClaimsJws(token)
        	        .getBody();

            String username = claims.getSubject();
            String session = (String) claims.get("session");

            Token dbAccessToken = Accesos.getValidAccessToken(username, token, session);
            if (dbAccessToken == null) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            // Token válido → continuar con la solicitud
            chain.doFilter(request, response);

        } catch (Exception e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token: " + e.getMessage());
        }
    }
}

