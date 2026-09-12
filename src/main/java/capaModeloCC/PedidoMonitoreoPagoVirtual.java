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

	//Lo que hace falta para que la pantalla de monitoreo pueda decir QUE paso con
	//el pago y no solo cuantos minutos lleva esperando. Todo esto ya estaba en la
	//base -en log_evento_wompi, en pedido_pago_virtual y en el propio pedido- pero
	//no habia por donde sacarlo.
	private String fechaPedido = "";
	private String fechaPago = "";
	private String origen = "";
	private int idEstadoPedido;
	/** La descripcion del estado del pedido, tal como la tiene el catalogo. */
	private String estadoPedido = "";
	/** Cuantos eventos ha devuelto Wompi para este link. */
	private int eventos;
	/** Cuantos de esos eventos fueron un pago rechazado. */
	private int rechazos;
	/** El ultimo estado de Wompi: APPROVED, DECLINED, PENDING, VOIDED, ERROR. */
	private String ultimoEstado = "";
	private String ultimoEvento = "";
	/** Cuantas veces se le aviso al cliente: link, recordatorio, cancelacion. */
	private int avisos;
	/** Cuantas observaciones de gestion tiene el pedido. */
	private int gestiones;
	/**
	 * El codigo del estado: no cambia con el caso, sirve para filtrar.
	 *
	 * El texto de abajo si cambia -"RECHAZADO 2 VECES", "SE VENCE EN 8 MIN"- porque
	 * esta escrito para que lo lea una persona.
	 */
	private String codigoEstado = "";

	/** El estado en palabras, ya resuelto: PAGADO, RECHAZADO, CANCELADO SIN PAGO... */
	private String estado = "";
	/** 1 pagado, 2 en tiempo, 3 paso el aviso, 4 por vencerse o rechazado, 5 perdido. */
	private int nivel;

	public String getFechaPedido() { return fechaPedido; }
	public void setFechaPedido(String fechaPedido) { this.fechaPedido = fechaPedido; }
	public String getFechaPago() { return fechaPago; }
	public void setFechaPago(String fechaPago) { this.fechaPago = fechaPago; }
	public String getOrigen() { return origen; }
	public void setOrigen(String origen) { this.origen = origen; }
	public int getIdEstadoPedido() { return idEstadoPedido; }
	public void setIdEstadoPedido(int idEstadoPedido) { this.idEstadoPedido = idEstadoPedido; }
	public String getEstadoPedido() { return estadoPedido; }
	public void setEstadoPedido(String estadoPedido) { this.estadoPedido = estadoPedido; }
	public int getEventos() { return eventos; }
	public void setEventos(int eventos) { this.eventos = eventos; }
	public int getRechazos() { return rechazos; }
	public void setRechazos(int rechazos) { this.rechazos = rechazos; }
	public String getUltimoEstado() { return ultimoEstado; }
	public void setUltimoEstado(String ultimoEstado) { this.ultimoEstado = ultimoEstado; }
	public String getUltimoEvento() { return ultimoEvento; }
	public void setUltimoEvento(String ultimoEvento) { this.ultimoEvento = ultimoEvento; }
	public int getAvisos() { return avisos; }
	public void setAvisos(int avisos) { this.avisos = avisos; }
	public int getGestiones() { return gestiones; }
	public void setGestiones(int gestiones) { this.gestiones = gestiones; }
	public String getCodigoEstado() { return codigoEstado; }
	public void setCodigoEstado(String codigoEstado) { this.codigoEstado = codigoEstado; }
	public String getEstado() { return estado; }
	public void setEstado(String estado) { this.estado = estado; }
	public int getNivel() { return nivel; }
	public void setNivel(int nivel) { this.nivel = nivel; }
	
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
