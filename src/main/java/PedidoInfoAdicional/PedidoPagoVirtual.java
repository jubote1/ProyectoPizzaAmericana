package PedidoInfoAdicional;

public class PedidoPagoVirtual {
	
	private int idPedidoPagoVirtual;
	private int idPedido;
	private String email;
	private String telefonoCelular;
	private String fechaHora;
	private String observacion;
	public int getIdPedidoPagoVirtual() {
		return idPedidoPagoVirtual;
	}
	public void setIdPedidoPagoVirtual(int idPedidoPagoVirtual) {
		this.idPedidoPagoVirtual = idPedidoPagoVirtual;
	}
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTelefonoCelular() {
		return telefonoCelular;
	}
	public void setTelefonoCelular(String telefonoCelular) {
		this.telefonoCelular = telefonoCelular;
	}
	public String getFechaHora() {
		return fechaHora;
	}
	public void setFechaHora(String fechaHora) {
		this.fechaHora = fechaHora;
	}
	public String getObservacion() {
		return observacion;
	}
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	public PedidoPagoVirtual(int idPedido, String email, String telefonoCelular, String observacion) {
		super();
		this.idPedido = idPedido;
		this.email = email;
		this.telefonoCelular = telefonoCelular;
		this.observacion = observacion;
	}
	
	
	
}
