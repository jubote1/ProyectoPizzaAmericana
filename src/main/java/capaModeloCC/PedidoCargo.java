package capaModeloCC;

/** Un pedido llevado por Rappi Cargo, con sus tiempos, para el Dashboard Cargo. */
public class PedidoCargo {

	private int idPedido;
	private int idTienda;
	private String nombreTienda;
	private int numPosHeader;
	private String fechaPedido;
	private String fechaInsercion;
	private String fechaEntregado;
	private String fechaCancelacion;
	private boolean entregado;
	private boolean cancelado;
	private Double minutosTotal;

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

	public String getFechaInsercion() {
		return fechaInsercion;
	}

	public void setFechaInsercion(String fechaInsercion) {
		this.fechaInsercion = fechaInsercion;
	}

	public String getFechaEntregado() {
		return fechaEntregado;
	}

	public void setFechaEntregado(String fechaEntregado) {
		this.fechaEntregado = fechaEntregado;
	}

	public String getFechaCancelacion() {
		return fechaCancelacion;
	}

	public void setFechaCancelacion(String fechaCancelacion) {
		this.fechaCancelacion = fechaCancelacion;
	}

	public boolean isEntregado() {
		return entregado;
	}

	public void setEntregado(boolean entregado) {
		this.entregado = entregado;
	}

	public boolean isCancelado() {
		return cancelado;
	}

	public void setCancelado(boolean cancelado) {
		this.cancelado = cancelado;
	}

	public Double getMinutosTotal() {
		return minutosTotal;
	}

	public void setMinutosTotal(Double minutosTotal) {
		this.minutosTotal = minutosTotal;
	}

}
