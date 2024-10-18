package capaSeguridad;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.crypto.bcrypt.BCrypt;
import capaSeguridad.modelo.Token;
import capaSeguridad.modelo.UserWithRole;
import conexionCC.ConexionBaseDatos;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

public class Accesos {

	private static final String SECRET_KEY = System.getenv("SECRET_KEY") != null ? System.getenv("SECRET_KEY") : "";
    private static final SecretKey KEY;

    static {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalStateException("SECRET_KEY must be set in the environment variables.");
        }
        KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    private static final String EncryptionKey = System.getenv("ENCRYPTION_KEY") != null ? System.getenv("ENCRYPTION_KEY") : "";

	
	  public  static boolean isValidUser(String username,String password){
		  boolean respuesta = false;
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDSeguridad();
			
		    String query = "SELECT password FROM users WHERE username = ?";
	        try (PreparedStatement statement = con1.prepareStatement(query)) {
	            statement.setString(1, username);
	            try (ResultSet resultSet = statement.executeQuery()) {
	                if (resultSet.next()) {
	                    String storedPassword = resultSet.getString("password");
	                    // Verifica la contraseña ingresada usando BCrypt
	                    respuesta = BCrypt.checkpw(password, storedPassword);
	                }
	            }
	            
	        } catch (SQLException e) {
	            System.err.println("Error: " + e.getMessage());

	        } finally {
	        	 closeConnection(con1);
	        }
	        return respuesta;
	        
	  }
	  
	  public static boolean saveAccessToken(String username, String token, long expirationLong, String session, String userAgent, String ipAddress, int existsession, int idNewSession) {
		    return upsertToken("access_tokens", username, token, expirationLong, session, userAgent, ipAddress, existsession, idNewSession);
		}

		public static boolean saveRefreshToken(String username, String token, long expirationLong, String session, String userAgent, String ipAddress, int existsession, int idNewSession) {
		    return upsertToken("refresh_tokens", username, token, expirationLong, session, userAgent, ipAddress, existsession, idNewSession);
		}

		private static boolean upsertToken(String tableName, String username, String token, long expirationLong, String session, String userAgent, String ipAddress, int existsession, int idNewSession) {
		    boolean respuesta = false;
		    int userId = getUserIdByUsername(username);
		    String encryptedToken;
		    
		    try {
		        encryptedToken = TokenEncryptor.encrypt(token, EncryptionKey);
		    } catch (Exception e) {
		        System.err.println("Error encrypting token: " + e.getMessage());
		        return false;
		    }

		    ConexionBaseDatos con = new ConexionBaseDatos();
		    try (Connection con1 = con.obtenerConexionBDSeguridad()) {
		        java.sql.Timestamp currentTime = new java.sql.Timestamp(System.currentTimeMillis());
		        java.sql.Timestamp expirationTime = new java.sql.Timestamp(expirationLong);

		        // Determina si se debe hacer un `UPDATE` o un `INSERT`
		        boolean shouldUpdate = existsession != -1 && doesTokenSessionExist(tableName, userId, existsession);
		        String query = shouldUpdate ?
		            "UPDATE " + tableName + " SET token = ?, created_at = ?, expiration = ?, user_agent = ?, ip_address = ? WHERE user_id = ? AND session_id = ?" :
		            "INSERT INTO " + tableName + " (user_id, token, created_at, expiration, user_agent, ip_address, session_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
		        
		        try (PreparedStatement stmt = con1.prepareStatement(query)) {
		   
		            if (shouldUpdate) {
		                // Orden de parámetros para UPDATE
		                stmt.setString(1, encryptedToken); // Token encriptado
		                stmt.setTimestamp(2, currentTime); // Fecha actual
		                stmt.setTimestamp(3, expirationTime); // Fecha de expiración
		                stmt.setString(4, userAgent); // User Agent
		                stmt.setString(5, ipAddress); // Dirección IP
		                stmt.setInt(6, userId); // ID de usuario
		                stmt.setInt(7, existsession); // ID de la sesión existente
		            } else {
		                // Orden de parámetros para INSERT
		                stmt.setInt(1, userId); // ID de usuario
		                stmt.setString(2, encryptedToken); // Token encriptado
		                stmt.setTimestamp(3, currentTime); // Fecha actual
		                stmt.setTimestamp(4, expirationTime); // Fecha de expiración
		                stmt.setString(5, userAgent); // User Agent
		                stmt.setString(6, ipAddress); // Dirección IP
		                if(idNewSession != -1) {
		                	   stmt.setInt(7, idNewSession); // ID de nueva sesión
		                }else {
		                	 stmt.setInt(7, existsession); // ID de nueva sesión
		                }
		             
		            }

		            respuesta = stmt.executeUpdate() > 0;
		        } catch (SQLException e) {
		            System.err.println("Database error: " + e.getMessage());
		        }
		    } catch (SQLException e) {
		        System.err.println("Error obtaining database connection: " + e.getMessage());
		    }
		    return respuesta;
		}

		public static boolean doesTokenSessionExist(String tableName, int userId, int sessionId) {
		    String selectQuery = "SELECT id FROM " + tableName + " WHERE user_id = ? AND session_id = ?";
		    boolean exists = false;

		    ConexionBaseDatos con = new ConexionBaseDatos();
		    try (Connection con1 = con.obtenerConexionBDSeguridad();
		         PreparedStatement stmt = con1.prepareStatement(selectQuery)) {
		        stmt.setInt(1, userId);
		        stmt.setInt(2, sessionId);

		        try (ResultSet rs = stmt.executeQuery()) {
		            exists = rs.next();
		        }
		    } catch (SQLException e) {
		        System.err.println("Error en la base de datos: " + e.getMessage());
		    }
		    return exists;
		}

		public static int doesSessionExist(String username, String sessionId) {
		    String selectQuery = "SELECT id FROM sessions WHERE user_id = ? AND valor = ?";
		    int id = -1;

		    ConexionBaseDatos con = new ConexionBaseDatos();
		    int userId = getUserIdByUsername(username);
		    try (Connection con1 = con.obtenerConexionBDSeguridad();
		         PreparedStatement stmt = con1.prepareStatement(selectQuery)) {
		        stmt.setInt(1, userId);
		        stmt.setString(2, sessionId);

		        try (ResultSet rs = stmt.executeQuery()) {
		            if (rs.next()) {
		                id = rs.getInt("id");
		            }
		        }
		    } catch (SQLException e) {
		        System.err.println("Error en la base de datos: " + e.getMessage());
		    }
		    return id;
		}

		

	    public Accesos() {
			super();
			// TODO Auto-generated constructor stub
		}
		public  static int insertSession(String username, String session) {
		    ConexionBaseDatos con = new ConexionBaseDatos();
		    Connection con1 = con.obtenerConexionBDSeguridad();
		    String insertQuery = "INSERT INTO sessions (user_id, valor) VALUES (?, ?)";
		    int generatedId = -1;

		    try (PreparedStatement insertStatement = con1.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
		        // Establecer los parámetros de la consulta
		    	int userId = getUserIdByUsername(username);
		        insertStatement.setInt(1, userId);
		        insertStatement.setString(2, session);

		        // Ejecutar la inserción
		        int rowsInserted = insertStatement.executeUpdate();

		        if (rowsInserted > 0) {
		            // Obtener las claves generadas
		            try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
		                if (generatedKeys.next()) {
		                    // Obtener el ID generado
		                    generatedId = generatedKeys.getInt(1);
		                }
		            }
		        }
		    } catch (SQLException e) {
		        System.err.println("Error en la base de datos: " + e.getMessage());
		    } finally {
		        closeConnection(con1);
		    }

		    return generatedId;
		}


	    public static int  getUserIdByUsername(String username) {
			ConexionBaseDatos con = new ConexionBaseDatos();
			Connection con1 = con.obtenerConexionBDSeguridad();
	        String query = "SELECT id FROM users WHERE username = ?";
	        try (
	             PreparedStatement statement = con1.prepareStatement(query)) {
	            statement.setString(1, username);
	            try (ResultSet resultSet = statement.executeQuery()) {
	                if (resultSet.next()) {
	                    return resultSet.getInt("id");
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        finally {
	        	
	        	 closeConnection(con1);
	        }
	        return -1; // Indica que el usuario no fue encontrado
	    }
	    
	    public static  UserWithRole getUserIdAndRoleByUsername(String username) {
	        ConexionBaseDatos con = new ConexionBaseDatos();
	        Connection con1 = con.obtenerConexionBDSeguridad();
	        String query = "SELECT id, (SELECT role_name FROM roles WHERE id = users.role_id) AS rol FROM users WHERE username = ?";

	        try (PreparedStatement statement = con1.prepareStatement(query)) {
	            statement.setString(1, username);
	            try (ResultSet resultSet = statement.executeQuery()) {
	                if (resultSet.next()) {
	                    int userId = resultSet.getInt("id");
	                    String role = resultSet.getString("rol");
	                    return new UserWithRole(userId, role);
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        } finally {
	            closeConnection(con1); // Asegúrate de cerrar la conexión correctamente
	        }

	        return null; // Retorna null si no se encuentra el usuario
	    }


	    public  static String generateToken(long expirationlog,String username,String session) {
	        Date issuedAt = new Date(System.currentTimeMillis());
	        UserWithRole usuario = getUserIdAndRoleByUsername(username); 
	        Date expiration = new Date(expirationlog);
	        // Claims adicionales
	        Map<String, Object> claims = new HashMap<>();
	        claims.put("id", usuario.getId());
	        claims.put("roles",usuario.getRol());
	        claims.put("session", session); // Identificador único del token
	        claims.put("jti", UUID.randomUUID().toString()); 
	        MacAlgorithm signatureAlgorithm = Jwts.SIG.HS256;

	        return Jwts
	                .builder()
	                .header()
	                .type("JWT")
	                .and()
	                 .subject(username)
	                .claims(claims)
	                .issuedAt(issuedAt)
	                .expiration(expiration)
	                .signWith(KEY, signatureAlgorithm)
	                .compact();
	    }
	    
	    
	    // Método para crear un nuevo usuario con contraseña encriptada
	    public static  boolean createUser(String username, String password,String email,int rol) {
	        String encryptedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); // Encripta la contraseña
	        
	        ConexionBaseDatos con = new ConexionBaseDatos();
	        Connection con1 = con.obtenerConexionBDSeguridad();
	        String query = "INSERT INTO users (username, password,email,role_id) VALUES (?, ?,?,?)";
	        
	        try (PreparedStatement statement = con1.prepareStatement(query)) {
	            statement.setString(1, username);
	            statement.setString(2, encryptedPassword);
	            statement.setString(3, email);
	            statement.setInt(4, rol);
	            int rowsInserted = statement.executeUpdate();
	            return rowsInserted > 0;
	        } catch (SQLException e) {
	            System.err.println("Error: " + e.getMessage());
	            return false;
	        } finally {
	            closeConnection(con1);
	        }
	    }
	    
	 // Método para cerrar la conexión a la base de datos
	    private static  void closeConnection(Connection con1) {
	        if (con1 != null) {
	            try {
	                con1.close();
	            } catch (SQLException e) {
	                System.err.println("Error al cerrar la conexión: " + e.getMessage());
	            }
	        }
	    }
	    
	    
	    public static Token getValidAccessToken(String username, String token, String session) {
	    	return getValidToken("access_tokens" ,username, token, session);
	    }
	    
	    public static Token getValidRefreshToken(String username, String token, String session) {
	    	return getValidToken("refresh_tokens" ,username, token, session);
	    }
	    
	    public static Token getValidToken(String tableName ,String username, String token, String session) {
	    	Token tokenObject = null;
	        ConexionBaseDatos con = new ConexionBaseDatos();
	        Connection con1 = con.obtenerConexionBDSeguridad();
	        String query = "SELECT * FROM "+tableName+" t INNER JOIN sessions s ON t.session_id = s.id WHERE t.user_id = ? AND t.is_valid = TRUE AND s.valor = ?";
	        
	        try (PreparedStatement statement = con1.prepareStatement(query)) {
	            
	            int userId = getUserIdByUsername(username);
	            statement.setInt(1, userId);
	            statement.setString(2, session);
	            
	            try (ResultSet resultSet = statement.executeQuery()) {
	                if (resultSet.next()) {
	                    String encryptedToken = resultSet.getString("token");
	                    String decryptedToken = TokenEncryptor.decrypt(encryptedToken, EncryptionKey);

	                    if (decryptedToken.equals(token)) {
	                     	Timestamp expiration = resultSet.getTimestamp("expiration");
	                        // Comprobar si el token ha expirado
	                        if (expiration.after(new Date())) {
	                            int id = resultSet.getInt("id");
	                            boolean isValid = resultSet.getBoolean("is_valid");
	                            java.sql.Timestamp createdAt = resultSet.getTimestamp("created_at");
	                            int sessionId = resultSet.getInt("session_id");

	                            // Crear el objeto Token con los datos obtenidos
	                            tokenObject = new Token(id, userId, decryptedToken, isValid, createdAt, expiration, sessionId);
	                        }
	                    }
	                }
	            }
	        } catch (Exception e) {
	            // Considera usar un logger en lugar de e.printStackTrace()
	            System.err.println("Error en la base de datos: " + e.getMessage());
	        }finally {
	            closeConnection(con1);
	        }

	        return tokenObject; // El token no es válido o ha expirado
	    }


	        
	    public static boolean DeleteAccessTokens(String username) {
	    	return  DeleteTokens("access_tokens" ,username);
	    	
	    }
	    
	    public static boolean DeleteRefreshTokens(String username) {
	    	return  DeleteTokens("refresh_tokens" ,username);
	    	
	    }
	    
	    public static boolean DeleteTokens(String tableName ,String username) {
	    	boolean respuesta = false;
	        ConexionBaseDatos con = new ConexionBaseDatos();
	        Connection con1 = con.obtenerConexionBDSeguridad();
	        
	        // Query para eliminar tokens expirados
	        String deleteExpiredTokensQuery = "DELETE FROM " + tableName + " WHERE user_id = ? AND expiration < ?";

	        
	        
	        try (
	            PreparedStatement deleteStatement = con1.prepareStatement(deleteExpiredTokensQuery)) {
	            
	            // Obtener el ID del usuario
	            int userId = getUserIdByUsername(username);
	            java.sql.Timestamp currentTime = new java.sql.Timestamp(System.currentTimeMillis());
	            // Primero, eliminar los tokens expirados del usuario
	            deleteStatement.setInt(1, userId);
	            deleteStatement.setTimestamp(2,currentTime);
	            int deletedRows = deleteStatement.executeUpdate();
	            respuesta = true;
	            System.out.println("Number of expired access tokens deleted: " + deletedRows);
	            
	        } catch (SQLException e) {
	            System.err.println("Error al  eliminar access tokens: " + e.getMessage());
	        } finally {
	            closeConnection(con1);
	        }
	        return respuesta;
	    }


	    
        public static void main(String[] args) {
        	createUser("wordpress","Pzz2024American4","a.desarrollosi@gmail.com",2);
   

        	
        	
        }

}
