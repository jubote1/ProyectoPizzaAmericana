package capaModeloCC;

public class LogEncuestaServicio {
	
	private int idEncuesta;
	private int numeroPedido;
	private String nombreCliente;
	private String telefono;
	private int idTienda;
	private String horaEnvio;
	private String contestado;
	private String horaLlenado;
	public int getIdEncuesta() {
		return idEncuesta;
	}
	public void setIdEncuesta(int idEncuesta) {
		this.idEncuesta = idEncuesta;
	}
	public int getNumeroPedido() {
		return numeroPedido;
	}
	public void setNumeroPedido(int numeroPedido) {
		this.numeroPedido = numeroPedido;
	}
	public String getNombreCliente() {
		return nombreCliente;
	}
	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getHoraEnvio() {
		return horaEnvio;
	}
	public void setHoraEnvio(String horaEnvio) {
		this.horaEnvio = horaEnvio;
	}
	public String getContestado() {
		return contestado;
	}
	public void setContestado(String contestado) {
		this.contestado = contestado;
	}
	public String getHoraLlenado() {
		return horaLlenado;
	}
	public void setHoraLlenado(String horaLlenado) {
		this.horaLlenado = horaLlenado;
	}
	public LogEncuestaServicio(int idEncuesta, int numeroPedido, String nombreCliente, String telefono, int idTienda,
			String horaEnvio, String contestado, String horaLlenado) {
		super();
		this.idEncuesta = idEncuesta;
		this.numeroPedido = numeroPedido;
		this.nombreCliente = nombreCliente;
		this.telefono = telefono;
		this.idTienda = idTienda;
		this.horaEnvio = horaEnvio;
		this.contestado = contestado;
		this.horaLlenado = horaLlenado;
	}	
	
	

}
