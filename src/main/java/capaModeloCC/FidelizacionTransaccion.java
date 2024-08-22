package capaModeloCC;

public class FidelizacionTransaccion {
	
	private String correo;
	private int idTienda;
	private int idPedidoTienda;
	private String fechaTransaccion;
	private double valorNeto;
	private double puntos;
	public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public int getIdPedidoTienda() {
		return idPedidoTienda;
	}
	public void setIdPedidoTienda(int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}
	public String getFechaTransaccion() {
		return fechaTransaccion;
	}
	public void setFechaTransaccion(String fechaTransaccion) {
		this.fechaTransaccion = fechaTransaccion;
	}
	public double getValorNeto() {
		return valorNeto;
	}
	public void setValorNeto(double valorNeto) {
		this.valorNeto = valorNeto;
	}
	public double getPuntos() {
		return puntos;
	}
	public void setPuntos(double puntos) {
		this.puntos = puntos;
	}
	public FidelizacionTransaccion(String correo, int idTienda, int idPedidoTienda, double valorNeto, double puntos) {
		super();
		this.correo = correo;
		this.idTienda = idTienda;
		this.idPedidoTienda = idPedidoTienda;
		this.valorNeto = valorNeto;
		this.puntos = puntos;
	}
	
	

}
