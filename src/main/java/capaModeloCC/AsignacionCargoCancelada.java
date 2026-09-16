package capaModeloCC;

/**
 * Un pedido que tuvo un evento de asignacion en tercerizado_domicilio_evento
 * pero que hoy NO tiene pedido.domicilio_tercerizado = 'S': la asignacion a
 * Cargo se cancelo o se revirtio.
 */
public class AsignacionCargoCancelada {

	private int idPedido;
	private int idTienda;
	private String nombreTienda;
	private int numPosHeader;
	private String fechaPedido;
	private String proveedor;
	private String ultimoEstadoCargo;
	private String fechaAsignacion;
	private String fechaCancelacionCargo;

	public int getIdPedido() {
		return idPedido;
	}

	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}

	public int getIdTienda() {
		return idTienda;
	}

	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}

	public String getNombreTienda() {
		return nombreTienda;
	}

	public void setNombreTienda(String nombreTienda) {
		this.nombreTienda = nombreTienda;
	}

	public int getNumPosHeader() {
		return numPosHeader;
	}

	public void setNumPosHeader(int numPosHeader) {
		this.numPosHeader = numPosHeader;
	}

	public String getFechaPedido() {
		return fechaPedido;
	}

	public void setFechaPedido(String fechaPedido) {
		this.fechaPedido = fechaPedido;
	}

	public String getProveedor() {
		return proveedor;
	}

	public void setProveedor(String proveedor) {
		this.proveedor = proveedor;
	}

	public String getUltimoEstadoCargo() {
		return ultimoEstadoCargo;
	}

	public void setUltimoEstadoCargo(String ultimoEstadoCargo) {
		this.ultimoEstadoCargo = ultimoEstadoCargo;
	}

	public String getFechaAsignacion() {
		return fechaAsignacion;
	}

	public void setFechaAsignacion(String fechaAsignacion) {
		this.fechaAsignacion = fechaAsignacion;
	}

	public String getFechaCancelacionCargo() {
		return fechaCancelacionCargo;
	}

	public void setFechaCancelacionCargo(String fechaCancelacionCargo) {
		this.fechaCancelacionCargo = fechaCancelacionCargo;
	}

}
