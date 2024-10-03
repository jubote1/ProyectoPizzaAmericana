package capaSeguridad.servicio;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaSeguridad.Accesos;
import capaSeguridad.modelo.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@WebServlet("/NewRefreshTokenServlet")
public class NewRefreshTokenServlet extends HttpServlet {

	private static final String SECRET_KEY = System.getenv("SECRET_KEY") != null ? System.getenv("SECRET_KEY") : "";
    private static final SecretKey KEY;

    static {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalStateException("SECRET_KEY must be set in the environment variables.");
        }
        KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    private static final long JWT_EXPIRATION_ACCESS_TOKEN = 1000L * 60 * 60 * 2;  // 2 hours
    private static final long JWT_EXPIRATION_REFRESH_TOKEN = 1000L * 60 * 60 * 24 * 7;  // 7 days

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String refreshToken = request.getParameter("refresh_token");
        String userAgentFromRequest = request.getHeader("User-Agent");
        String ipFromRequest = request.getRemoteAddr();

        try {
            // Validate the refresh token
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String username = claims.getSubject();
            String session = (String) claims.get("session");

            // Check if the refresh token is valid and not expired
            Token dbRefreshToken = Accesos.getValidRefreshToken(username, refreshToken, session);
            if (dbRefreshToken == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired refresh token");
                return;
            }

            // Generate new tokens
            long expirationLongAccessToken = System.currentTimeMillis() + JWT_EXPIRATION_ACCESS_TOKEN;
            long expirationLongRefreshToken = System.currentTimeMillis() + JWT_EXPIRATION_REFRESH_TOKEN;

            String newAccessToken = Accesos.generateToken(expirationLongAccessToken, username, session);
            String newRefreshToken = Accesos.generateToken(expirationLongRefreshToken, username, session);

            
            // Update the access and refresh tokens in the database
            boolean saveAccessToken = Accesos.saveAccessToken(username, newAccessToken, expirationLongAccessToken, session, userAgentFromRequest, ipFromRequest,dbRefreshToken.getSession_id(),-1);
            boolean saveRefreshToken = Accesos.saveRefreshToken(username, newRefreshToken, expirationLongRefreshToken, session, userAgentFromRequest, ipFromRequest,dbRefreshToken.getSession_id(),-1);

            if (!saveAccessToken || !saveRefreshToken) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update tokens in the database");
                return;
            }

            // Respond with the new tokens
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            Map<String, Object> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);
            tokens.put("accessTokenExpiration", expirationLongAccessToken);
            tokens.put("refreshTokenExpiration", expirationLongRefreshToken);
            
            
            response.getWriter().write(tokensToJson(tokens));
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid refresh token");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing the request");
        }
    }
    
    private String tokensToJson(Map<String, Object> tokens) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(tokens);
    }
}

    
