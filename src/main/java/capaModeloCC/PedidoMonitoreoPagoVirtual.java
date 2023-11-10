package capaModeloCC;

public class PedidoMonitoreoPagoVirtual {
	
	private int idPedido;
	private String tienda;
	private String nombre;
	private String telefono;
	private String telefonoCelular;
	private String email;
	private String totalNeto;
	private String idLink;
	private String fechaInsercion;
	private int minutos;
	private int idFormaPago;
	private int idCliente;
	
	public int getIdFormaPago() {
		return idFormaPago;
	}
	public void setIdFormaPago(int idFormaPago) {
		this.idFormaPago = idFormaPago;
	}
	public int getIdCliente() {
		return idCliente;
	}
	public void setIdCliente(int idCliente) {
		this.idCliente = idCliente;
	}
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getTelefonoCelular() {
		return telefonoCelular;
	}
	public void setTelefonoCelular(String telefonoCelular) {
		this.telefonoCelular = telefonoCelular;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getTotalNeto() {
		return totalNeto;
	}
	public void setTotalNeto(String totalNeto) {
		this.totalNeto = totalNeto;
	}
	public String getFechaInsercion() {
		return fechaInsercion;
	}
	public void setFechaInsercion(String fechaInsercion) {
		this.fechaInsercion = fechaInsercion;
	}
	public int getMinutos() {
		return minutos;
	}
	public void setMinutos(int minutos) {
		this.minutos = minutos;
	}

	public String getIdLink() {
		return idLink;
	}
	public void setIdLink(String idLink) {
		this.idLink = idLink;
	}
	public PedidoMonitoreoPagoVirtual(int idPedido, String tienda, String nombre, String telefono,
			String telefonoCelular, String email, String totalNeto, String idLink, String fechaInsercion, int minutos,
			int idFormaPago, int idCliente) {
		super();
		this.idPedido = idPedido;
		this.tienda = tienda;
		this.nombre = nombre;
		this.telefono = telefono;
		this.telefonoCelular = telefonoCelular;
		this.email = email;
		this.totalNeto = totalNeto;
		this.idLink = idLink;
		this.fechaInsercion = fechaInsercion;
		this.minutos = minutos;
		this.idFormaPago = idFormaPago;
		this.idCliente = idCliente;
	}
	
	

}
