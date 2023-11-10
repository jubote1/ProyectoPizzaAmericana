package capaModeloCC;

public class HomologacionTiendaToken {
	
	private int token;
	private int idtienda;
	private String origen;
	public int getToken() {
		return token;
	}
	public void setToken(int token) {
		this.token = token;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public String getOrigen() {
		return origen;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public HomologacionTiendaToken(int token, int idtienda, String origen) {
		super();
		this.token = token;
		this.idtienda = idtienda;
		this.origen = origen;
	}
	
	

}
