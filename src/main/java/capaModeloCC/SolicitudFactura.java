package capaModeloCC;

public class SolicitudFactura {
	
	private int idSolicitud;
	private int idPedidoContact;
	private int idPedidoTienda;
	private double valor;
	private String nit;
	private String correo;
	private String empresa;
	private String telefono;
	private String fechaPedido;
	private String fechaSolicitud;
	private String estado;
	private String usuario;
		
	public String getUsuario() {
		return usuario;
	}
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}
	public String getFechaSolicitud() {
		return fechaSolicitud;
	}
	public void setFechaSolicitud(String fechaSolicitud) {
		this.fechaSolicitud = fechaSolicitud;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public int getIdSolicitud() {
		return idSolicitud;
	}
	public void setIdSolicitud(int idSolicitud) {
		this.idSolicitud = idSolicitud;
	}
	public int getIdPedidoContact() {
		return idPedidoContact;
	}
	public void setIdPedidoContact(int idPedidoContact) {
		this.idPedidoContact = idPedidoContact;
	}
	public int getIdPedidoTienda() {
		return idPedidoTienda;
	}
	public void setIdPedidoTienda(int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	public String getNit() {
		return nit;
	}
	public void setNit(String nit) {
		this.nit = nit;
	}
	public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
	public String getEmpresa() {
		return empresa;
	}
	public void setEmpresa(String empresa) {
		this.empresa = empresa;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getFechaPedido() {
		return fechaPedido;
	}
	public void setFechaPedido(String fechaPedido) {
		this.fechaPedido = fechaPedido;
	}
	public SolicitudFactura(int idSolicitud, int idPedidoContact, int idPedidoTienda, double valor, String nit,
			String correo, String empresa, String telefono, String fechaPedido, String fechaSolicitud, String estado, String usuario) {
		super();
		this.idSolicitud = idSolicitud;
		this.idPedidoContact = idPedidoContact;
		this.idPedidoTienda = idPedidoTienda;
		this.valor = valor;
		this.nit = nit;
		this.correo = correo;
		this.empresa = empresa;
		this.telefono = telefono;
		this.fechaPedido = fechaPedido;
		this.fechaSolicitud = fechaSolicitud;
		this.estado = estado;
		this.usuario = usuario;
	}

	

}
