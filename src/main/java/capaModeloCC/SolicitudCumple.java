package capaModeloCC;

public class SolicitudCumple {
	
	private int idSolicitudCumple;
	private String fecha;
	private int idTienda;
	private int idPedido;
	private String tienda;
	
	
	
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public int getIdSolicitudCumple() {
		return idSolicitudCumple;
	}
	public void setIdSolicitudCumple(int idSolicitudCumple) {
		this.idSolicitudCumple = idSolicitudCumple;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public SolicitudCumple(int idSolicitudCumple, String fecha, int idTienda, int idPedido) {
		super();
		this.idSolicitudCumple = idSolicitudCumple;
		this.fecha = fecha;
		this.idTienda = idTienda;
		this.idPedido = idPedido;
	}

}
