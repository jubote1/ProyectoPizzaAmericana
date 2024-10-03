package capaSeguridad.servicio;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaSeguridad.Accesos;

import javax.servlet.annotation.WebServlet;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // Key should be loaded from a secure source, e.g., environment variable or a secrets manager
    //private static final String SECRET_KEY = System.getenv("JWT_SECRET_KEY");
    private static final long JWT_EXPIRATION_ACCESS_TOKEN = 1000L * 60 * 60 * 2; // 2 hours
    private static final long JWT_EXPIRATION_REFRESH_TOKEN = 1000L * 60 * 60 * 24 * 7; // 7 days



    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String sesionOld = request.getParameter("session");
        String userAgentFromRequest = request.getHeader("User-Agent");
        String ipFromRequest = request.getRemoteAddr();

        if (isValidCredentials(username, password)) {
            // Generate tokens and save them
            Map<String, Object> tokens = generateTokens(username, sesionOld, userAgentFromRequest, ipFromRequest,response);
        
            if (tokens == null) {
                // Token generation failed, response is already handled in generateTokens method
                return;  // End execution early since the error is already sent
            }

            // Respond with the tokens and their expiration times
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(tokensToJson(tokens));
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
        }
    }

    private boolean isValidCredentials(String username, String password) {
        return Accesos.isValidUser(username, password);
    }

    private Map<String, Object> generateTokens(String username, String sesionOld, String userAgent, String ip, HttpServletResponse response) throws IOException {
    	String session = (sesionOld != null && !sesionOld.isEmpty()) ? sesionOld : UUID.randomUUID().toString();

        long expirationLongAccessToken = System.currentTimeMillis() + JWT_EXPIRATION_ACCESS_TOKEN;
        long expirationLongRefreshToken = System.currentTimeMillis() + JWT_EXPIRATION_REFRESH_TOKEN;
        
        int existsession = Accesos.doesSessionExist(username, sesionOld);
        int idNewSession = -1;

        if (existsession == -1) {
        	session = UUID.randomUUID().toString();
            idNewSession = Accesos.insertSession(username, session);
        }


        String accessToken = Accesos.generateToken(expirationLongAccessToken, username, session);
        String refreshToken = Accesos.generateToken(expirationLongRefreshToken, username, session);

 
        boolean saveAccess = Accesos.saveAccessToken(username, accessToken, expirationLongAccessToken, session, userAgent, ip,existsession,idNewSession);
        boolean saveRefresh = Accesos.saveRefreshToken(username, refreshToken, expirationLongRefreshToken, session, userAgent, ip,existsession,idNewSession);
        System.out.println(saveAccess);
        System.out.println(saveRefresh);
        if(!saveAccess || !saveRefresh) {
        	return null;
        }
        
        //aqui se eliminan los tokens relacionados con el usuario que ya han expirado.
        Accesos.DeleteAccessTokens(username);
        Accesos.DeleteRefreshTokens(username);

        // Construir el mapa de tokens para la respuesta
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("session", session);
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("accessTokenExpiration", expirationLongAccessToken);
        tokens.put("refreshTokenExpiration", expirationLongRefreshToken);

        return tokens;
    }



    private String tokensToJson(Map<String, Object> tokens) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(tokens);
    }

}
