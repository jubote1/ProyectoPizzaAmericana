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
    private static final String SECRET_KEY = "rLRo7wEjYWqQ0MXheR3Fa2e4ynWSA1W8O4YynRJXg6g="; // Define tu clave secreta aquí
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialización si es necesaria
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String authHeader = req.getHeader(AUTH_HEADER);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());

            try {
                // Validar el token JWT
                Claims claims = Jwts.parser()
                        .verifyWith(KEY)
                        .build() // <---
                        .parseSignedClaims(token)
                        .getPayload();
                // Verificar la fecha de expiración
                String username = claims.getSubject();
                String session= (String) claims.get("session");
                Token dbAccessToken = Accesos.getValidAccessToken(username, token,session);
                if (dbAccessToken == null ) {
                	res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired refresh token");
                    return;
                }

                // Token válido, continuar con la cadena de filtros
                chain.doFilter(request, response);

            } catch (Exception e) {
                // Token inválido
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token "+e);
            }
        } else {
            // No se proporcionó un token
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
        }
    }

    @Override
    public void destroy() {
        // Limpieza si es necesaria
    }
}
