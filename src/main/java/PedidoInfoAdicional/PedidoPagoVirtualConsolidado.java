package PedidoInfoAdicional;

public class PedidoPagoVirtualConsolidado {
	
	private int idPedidoVirtual;
	private int idTienda;
	private int idPedidoTienda;
	private double valorPagoVirtual;
	private double valorPedido;
	private String idLink;
	private String estado;
	private String tipoPago;
	private String notificado;
	private String fechaPagoVirtual;
	public int getIdPedidoVirtual() {
		return idPedidoVirtual;
	}
	public void setIdPedidoVirtual(int idPedidoVirtual) {
		this.idPedidoVirtual = idPedidoVirtual;
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
	public double getValorPagoVirtual() {
		return valorPagoVirtual;
	}
	public void setValorPagoVirtual(double valorPagoVirtual) {
		this.valorPagoVirtual = valorPagoVirtual;
	}
	public double getValorPedido() {
		return valorPedido;
	}
	public void setValorPedido(double valorPedido) {
		this.valorPedido = valorPedido;
	}
	public String getIdLink() {
		return idLink;
	}
	public void setIdLink(String idLink) {
		this.idLink = idLink;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public String getTipoPago() {
		return tipoPago;
	}
	public void setTipoPago(String tipoPago) {
		this.tipoPago = tipoPago;
	}
	public String getNotificado() {
		return notificado;
	}
	public void setNotificado(String notificado) {
		this.notificado = notificado;
	}
	public String getFechaPagoVirtual() {
		return fechaPagoVirtual;
	}
	public void setFechaPagoVirtual(String fechaPagoVirtual) {
		this.fechaPagoVirtual = fechaPagoVirtual;
	}
	public PedidoPagoVirtualConsolidado(int idPedidoVirtual, int idTienda, int idPedidoTienda, double valorPagoVirtual,
			double valorPedido, String idLink, String estado, String tipoPago, String notificado,
			String fechaPagoVirtual) {
		super();
		this.idPedidoVirtual = idPedidoVirtual;
		this.idTienda = idTienda;
		this.idPedidoTienda = idPedidoTienda;
		this.valorPagoVirtual = valorPagoVirtual;
		this.valorPedido = valorPedido;
		this.idLink = idLink;
		this.estado = estado;
		this.tipoPago = tipoPago;
		this.notificado = notificado;
		this.fechaPagoVirtual = fechaPagoVirtual;
	}
	
	

}
