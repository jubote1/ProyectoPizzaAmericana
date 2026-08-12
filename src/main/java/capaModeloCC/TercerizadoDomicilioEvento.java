package capaModeloCC;

public class TercerizadoDomicilioEvento {
	
	private int idPedido;
	private String urlSeguimiento;
	private String fechaEntrega;
	private String fechaCancelacion;

	private String idPedidoLogistico;

	public String getIdPedidoLogistico() {
	    return idPedidoLogistico;
	}

	public void setIdPedidoLogistico(String idPedidoLogistico) {
	    this.idPedidoLogistico = idPedidoLogistico;
	}

	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public String getUrlSeguimiento() {
		return urlSeguimiento;
	}
	public void setUrlSeguimiento(String urlSeguimiento) {
		this.urlSeguimiento = urlSeguimiento;
	}
	public String getFechaEntrega() {
		return fechaEntrega;
	}
	public void setFechaEntrega(String fechaEntrega) {
		this.fechaEntrega = fechaEntrega;
	}
	public String getFechaCancelacion() {
		return fechaCancelacion;
	}
	public void setFechaCancelacion(String fechaCancelacion) {
		this.fechaCancelacion = fechaCancelacion;
	}
	public TercerizadoDomicilioEvento() {
		super();
	}
	

}

