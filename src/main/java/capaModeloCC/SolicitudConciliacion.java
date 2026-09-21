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
	public SolicitudConciliacion(int idSolicitud, String fecha, String origen, String descripcion, int idTienda,
			String categoria, double valorAnalizar, String estado, double valorFinal, String telefono,
			int idPedidoTienda) {
		super();
		this.idSolicitud = idSolicitud;
		this.fecha = fecha;
		this.origen = origen;
		this.descripcion = descripcion;
		this.idTienda = idTienda;
		this.categoria = categoria;
		this.valorAnalizar = valorAnalizar;
		this.estado = estado;
		this.valorFinal = valorFinal;
		this.telefono = telefono;
		this.idPedidoTienda = idPedidoTienda;
	}

	/*
	 * Lo que la pantalla necesita mostrar y que no estaba en la tabla.
	 *
	 * nombreTienda para no obligar a la pantalla a cruzar el id contra el
	 * selector, y dias para que se vea de una cuanto lleva la diferencia sin
	 * resolver: al 2026-09-15 la mas vieja llevaba 422 dias.
	 */
	private String nombreTienda = "";
	private int dias = 0;

	/** Quien la cerro y cuando. Antes no se guardaba. */
	private String usuarioProcesa = "";
	private String fechaProcesa = "";
	private String observacionCierre = "";

	public String getNombreTienda() {
		return this.nombreTienda;
	}

	public void setNombreTienda(final String nombreTienda) {
		this.nombreTienda = nombreTienda;
	}

	public int getDias() {
		return this.dias;
	}

	public void setDias(final int dias) {
		this.dias = dias;
	}

	public String getUsuarioProcesa() {
		return this.usuarioProcesa;
	}

	public void setUsuarioProcesa(final String usuarioProcesa) {
		this.usuarioProcesa = usuarioProcesa;
	}

	public String getFechaProcesa() {
		return this.fechaProcesa;
	}

	public void setFechaProcesa(final String fechaProcesa) {
		this.fechaProcesa = fechaProcesa;
	}

	public String getObservacionCierre() {
		return this.observacionCierre;
	}

	public void setObservacionCierre(final String observacionCierre) {
		this.observacionCierre = observacionCierre;
	}

	public SolicitudConciliacion() {
		super();
	}
}
