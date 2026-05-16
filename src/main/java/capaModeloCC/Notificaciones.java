package capaModeloCC;


public class Notificaciones {
	private int id;
	private String mensaje;
	private String fechaHora;
	private String notificado;
	private String origen;
	private int idpedido;
	
	public String getOrigen() {
		return origen;
	}
	public int getIdpedido() {
		return idpedido;
	}
	public void setIdpedido(int idpedido) {
		this.idpedido = idpedido;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getMensaje() {
		return mensaje;
	}
	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}
	public String getFechaHora() {
		return fechaHora;
	}
	public void setFechaHora(String fechaHora) {
		this.fechaHora = fechaHora;
	}
	public String getNotificado() {
		return notificado;
	}
	public void setNotificado(String notificado) {
		this.notificado = notificado;
	}
	public Notificaciones(int id, String mensaje, String fechaHora, String notificado , String origen, int idpedido) {
		super();
		this.id = id;
		this.mensaje = mensaje;
		this.fechaHora = fechaHora;
		this.notificado = notificado;
		this.origen = origen;
		this.idpedido = idpedido;
	}
	public Notificaciones(String mensaje) {
		super();
		this.mensaje = mensaje;
	}
	
	
	

}
