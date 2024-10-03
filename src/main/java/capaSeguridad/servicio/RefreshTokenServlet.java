package capaSeguridad.servicio;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import io.jsonwebtoken.security.MacAlgorithm;


@WebServlet("/refreshToken")
public class RefreshTokenServlet extends HttpServlet {

    private static final String SECRET_KEY = "rLRo7wEjYWqQ0MXheR3Fa2e4ynWSA1W8O4YynRJXg6g=";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    private static final long JWT_EXPIRATION_ACCESS_TOKEN = 1000L * 60 * 60 * 2; // 2 hours

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String refreshToken = request.getParameter("refresh_token");
        String userAgentFromRequest = request.getHeader("User-Agent");
        String ipFromRequest = request.getRemoteAddr();

        try {
            // Parse and validate the refresh token
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String username = claims.getSubject();
            String session = (String) claims.get("session");


            // Verify if the refresh token is valid and not expired in the database
            Token dbRefreshToken = Accesos.getValidRefreshToken(username, refreshToken, session);
            if (dbRefreshToken == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired refresh token");
                return;
            }

            // Generate a new access token
            long expirationLong = System.currentTimeMillis() + JWT_EXPIRATION_ACCESS_TOKEN;
            String newAccessToken = Accesos.generateToken(expirationLong, username, session);
 
            // Update the access token in the database and verify if the update was successful
            boolean  saveAccessToken = Accesos.saveAccessToken(username, newAccessToken, expirationLong, session, userAgentFromRequest, ipFromRequest,dbRefreshToken.getSession_id(),-1);
            if (!saveAccessToken) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Failed to generate new access token");
                return;
            }else

            // Respond with the new tokens
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            Map<String, Object> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("accessTokenExpiration", expirationLong);
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