package capaModeloCC;

public class TiendaFranquiciaWompi {
	
	private int idTienda;
	private String apiLlavePublica;
	private String apiLlavePrivada;
	private String correo;
	
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getApiLlavePublica() {
		return apiLlavePublica;
	}
	public void setApiLlavePublica(String apiLlavePublica) {
		this.apiLlavePublica = apiLlavePublica;
	}
	public String getApiLlavePrivada() {
		return apiLlavePrivada;
	}
	public void setApiLlavePrivada(String apiLlavePrivada) {
		this.apiLlavePrivada = apiLlavePrivada;
	}
	
	
	public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
	public TiendaFranquiciaWompi(int idTienda, String apiLlavePublica, String apiLlavePrivada, String correo) {
		super();
		this.idTienda = idTienda;
		this.apiLlavePublica = apiLlavePublica;
		this.apiLlavePrivada = apiLlavePrivada;
		this.correo = correo;
	}

	
}
