package PedidoInfoAdicional;

public class LogEventoWompi {
	
	private int idLogEventoWompi;
	private String indLink;
	private String fechaHora;
	private String evento;
	private String estado;
	public int getIdLogEventoWompi() {
		return idLogEventoWompi;
	}
	public void setIdLogEventoWompi(int idLogEventoWompi) {
		this.idLogEventoWompi = idLogEventoWompi;
	}
	public String getIndLink() {
		return indLink;
	}
	public void setIndLink(String indLink) {
		this.indLink = indLink;
	}
	public String getFechaHora() {
		return fechaHora;
	}
	public void setFechaHora(String fechaHora) {
		this.fechaHora = fechaHora;
	}
	public String getEvento() {
		return evento;
	}
	public void setEvento(String evento) {
		this.evento = evento;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public LogEventoWompi(String indLink, String evento, String estado) {
		super();
		this.indLink = indLink;
		this.evento = evento;
		this.estado = estado;
	}
	
	
	

}
