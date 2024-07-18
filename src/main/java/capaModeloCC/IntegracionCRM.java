package capaModeloCC;

public class IntegracionCRM {
	
	private String CRM;
	private int idTienda;
	private String accessToken;
	private String freshToken;
	private String clientID;
	private String appShopID;
	
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getClientID() {
		return clientID;
	}
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getCRM() {
		return CRM;
	}
	public void setCRM(String cRM) {
		CRM = cRM;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getFreshToken() {
		return freshToken;
	}
	public void setFreshToken(String freshToken) {
		this.freshToken = freshToken;
	}
	
	public String getAppShopID() {
		return appShopID;
	}
	public void setAppShopID(String appShopID) {
		this.appShopID = appShopID;
	}
	public IntegracionCRM(String cRM, int idTienda, String accessToken, String freshToken, String clientID, String appShopID) {
		super();
		this.CRM = cRM;
		this.idTienda = idTienda;
		this.accessToken = accessToken;
		this.freshToken = freshToken;
		this.clientID = clientID;
		this.appShopID = appShopID;
	}
}
