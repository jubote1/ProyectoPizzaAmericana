package capaModeloCC;

public class PedidoInfoAdicional {
	
	private String origen;
	private String programado;
	private String horaProgramado;
	private double descuento;
	private int idTipoPedido;
	
	public int getIdTipoPedido() {
		return idTipoPedido;
	}
	public void setIdTipoPedido(int idTipoPedido) {
		this.idTipoPedido = idTipoPedido;
	}
	public double getDescuento() {
		return descuento;
	}
	public void setDescuento(double descuento) {
		this.descuento = descuento;
	}
	public String getOrigen() {
		return origen;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public String getProgramado() {
		return programado;
	}
	public void setProgramado(String programado) {
		this.programado = programado;
	}
	public String getHoraProgramado() {
		return horaProgramado;
	}
	public void setHoraProgramado(String horaProgramado) {
		this.horaProgramado = horaProgramado;
	}
	public PedidoInfoAdicional(String origen, String programado, String horaProgramado, double descuento, int idTipoPedido) {
		super();
		this.origen = origen;
		this.programado = programado;
		this.horaProgramado = horaProgramado;
		this.descuento = descuento;
		this.idTipoPedido = idTipoPedido;
	}
	
	
}
