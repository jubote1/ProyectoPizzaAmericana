package capaModeloCC;

public class DomiciliarioPedido {
	
	private int idUsuario;
	private String fecha;
	private int idTienda;
	private int cantidad;
	public int getIdUsuario() {
		return idUsuario;
	}
	public void setIdUsuario(int idUsuario) {
		this.idUsuario = idUsuario;
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
	public int getCantidad() {
		return cantidad;
	}
	public void setCantidad(int cantidad) {
		this.cantidad = cantidad;
	}
	public DomiciliarioPedido(int idUsuario, String fecha, int idTienda, int cantidad) {
		super();
		this.idUsuario = idUsuario;
		this.fecha = fecha;
		this.idTienda = idTienda;
		this.cantidad = cantidad;
	}
	
}
