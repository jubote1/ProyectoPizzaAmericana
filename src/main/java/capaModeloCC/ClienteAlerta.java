package capaModeloCC;

public class ClienteAlerta {
	
	private String telefono;
	private String tipoAlerta;
	private String mensaje;
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getTipoAlerta() {
		return tipoAlerta;
	}
	public void setTipoAlerta(String tipoAlerta) {
		this.tipoAlerta = tipoAlerta;
	}
	public String getMensaje() {
		return mensaje;
	}
	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}
	public ClienteAlerta(String telefono, String tipoAlerta, String mensaje) {
		super();
		this.telefono = telefono;
		this.tipoAlerta = tipoAlerta;
		this.mensaje = mensaje;
	}
	
	public ClienteAlerta() {
		super();
	}

}
