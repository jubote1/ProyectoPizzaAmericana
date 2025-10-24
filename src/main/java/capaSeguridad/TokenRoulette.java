package capaSeguridad;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import capaSeguridad.modelo.UserWithRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

public class TokenRoulette {
	
	private static final String SECRET_KEY_ROULETTE = System.getenv("SECRET_KEY_ROULETTE") != null ? System.getenv("SECRET_KEY_ROULETTE") : "9sqyZ26hairlgImDnv9cpSmMyh33KaMHNnhpI8LQcv4=";
    private static final SecretKey KEY;

    static {
        if (SECRET_KEY_ROULETTE == null || SECRET_KEY_ROULETTE.isEmpty()) {
            throw new IllegalStateException("SECRET_KEY_ROULETTE must be set in the environment variables.");
        }
        KEY = Keys.hmacShaKeyFor(SECRET_KEY_ROULETTE.getBytes());
    }
    
    public static  String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
   

    public static String generateToken(int indice, int idpedido, int idregistro) {
        Date issuedAt = new Date(System.currentTimeMillis());

        Map<String, Object> claims = new HashMap<>();
        claims.put("indice", indice);
        claims.put("idregistro", idregistro);
        claims.put("jti", UUID.randomUUID().toString());

        MacAlgorithm signatureAlgorithm = Jwts.SIG.HS256;

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .subject(String.valueOf(idpedido))
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(new Date(System.currentTimeMillis() + 31536000000L)) // 1 año
                .signWith(KEY, signatureAlgorithm)
                .compact();
    }

    public static boolean validarToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token);

            int indice = claims.getPayload().get("indice", Integer.class);
            String idpedido = claims.getPayload().getSubject();
            int idregistro = claims.getPayload().get("idregistro", Integer.class);

            System.out.println("Token válido. idpedido=" + idpedido + ", idregistro=" + idregistro);
            return true;
        } catch (JwtException e) {
            System.out.println("Token inválido o manipulado: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
  	
    }

    

}
