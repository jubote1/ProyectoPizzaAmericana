package capaModeloCC;

public class PedidoPrecioEmpleado {
	
	private int idEmpleado;
	private String fecha;
	private int idTienda;
	private double valor;
	private int idPedido;
	private String autorizado;
	private String codigo;
	private String fechaInsercion;
	private String nombreEmpleado;
	private String tienda;
	
	
	public String getFechaInsercion() {
		return fechaInsercion;
	}
	public void setFechaInsercion(String fechaInsercion) {
		this.fechaInsercion = fechaInsercion;
	}
	public String getNombreEmpleado() {
		return nombreEmpleado;
	}
	public void setNombreEmpleado(String nombreEmpleado) {
		this.nombreEmpleado = nombreEmpleado;
	}
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public int getIdEmpleado() {
		return idEmpleado;
	}
	public void setIdEmpleado(int idEmpleado) {
		this.idEmpleado = idEmpleado;
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
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public String getAutorizado() {
		return autorizado;
	}
	public void setAutorizado(String autorizado) {
		this.autorizado = autorizado;
	}
	public String getCodigo() {
		return codigo;
	}
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	public PedidoPrecioEmpleado(int idEmpleado, String fecha, int idTienda, double valor, int idPedido,
			String autorizado, String codigo) {
		super();
		this.idEmpleado = idEmpleado;
		this.fecha = fecha;
		this.idTienda = idTienda;
		this.valor = valor;
		this.idPedido = idPedido;
		this.autorizado = autorizado;
		this.codigo = codigo;
	}
	
	

}
