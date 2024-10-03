package capaSeguridad.modelo;

import java.sql.Timestamp;

public class Token {
    private int id;
    private int userId;
    private String token;
    private boolean isValid;
    private java.sql.Timestamp createdAt;
    private java.sql.Timestamp expiration;
    private int session_id;
    
    public Token(int id, int userId, String token, boolean isValid, Timestamp createdAt, Timestamp expiration,
			int session_id) {
		super();
		this.id = id;
		this.userId = userId;
		this.token = token;
		this.isValid = isValid;
		this.createdAt = createdAt;
		this.expiration = expiration;
		this.session_id = session_id;
	}



	public int getSession_id() {
		return session_id;
	}



	public void setSession_id(int session_id) {
		this.session_id = session_id;
	}


	

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public java.sql.Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.sql.Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public java.sql.Timestamp getExpiration() {
        return expiration;
    }

    public void setExpiration(java.sql.Timestamp expiration) {
        this.expiration = expiration;
    }
}
