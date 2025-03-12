package capaModeloCC;

public class SolicitudConciliacion {
	
	private int idSolicitud;
	private String fecha;
	private String origen;
	private String descripcion;
	private int idTienda;
	private String categoria;
	private double valorAnalizar;
	private String estado;
	private double valorFinal;
	private String telefono;
	private int idPedidoTienda;
	public int getIdSolicitud() {
		return idSolicitud;
	}
	public void setIdSolicitud(int idSolicitud) {
		this.idSolicitud = idSolicitud;
	}
	public String getOrigen() {
		return origen;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getCategoria() {
		return categoria;
	}
	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}
	public double getValorAnalizar() {
		return valorAnalizar;
	}
	public void setValorAnalizar(double valorAnalizar) {
		this.valorAnalizar = valorAnalizar;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public double getValorFinal() {
		return valorFinal;
	}
	public void setValorFinal(double valorFinal) {
		this.valorFinal = valorFinal;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public int getIdPedidoTienda() {
		return idPedidoTienda;
	}
	public void setIdPedidoTienda(int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public SolicitudConciliacion(String fecha, String origen, String descripcion, int idTienda, String categoria,
			double valorAnalizar, String telefono, int idPedidoTienda) {
		super();
		this.fecha = fecha;
		this.origen = origen;
		this.descripcion = descripcion;
		this.idTienda = idTienda;
		this.categoria = categoria;
		this.valorAnalizar = valorAnalizar;
		this.telefono = telefono;
		this.idPedidoTienda = idPedidoTienda;
	}
	
	
	

}
