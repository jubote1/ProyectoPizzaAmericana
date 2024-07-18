package PedidoInfoAdicional;

public class MarcacionPedido {
	
	private int idPedido;
	private int idMarcacion;
	private String observacion;
	private double descuento;
	private String motivo;
	private String nombreMarcacion;
	
	public String getNombreMarcacion() {
		return nombreMarcacion;
	}
	public void setNombreMarcacion(String nombreMarcacion) {
		this.nombreMarcacion = nombreMarcacion;
	}
	public double getDescuento() {
		return descuento;
	}
	public void setDescuento(double descuento) {
		this.descuento = descuento;
	}
	public String getMotivo() {
		return motivo;
	}
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
	public int getIdPedido() {
		return idPedido;
	}
	public void setIdPedido(int idPedido) {
		this.idPedido = idPedido;
	}
	public int getIdMarcacion() {
		return idMarcacion;
	}
	public void setIdMarcacion(int idMarcacion) {
		this.idMarcacion = idMarcacion;
	}
	public String getObservacion() {
		return observacion;
	}
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	public MarcacionPedido(int idPedido, int idMarcacion, String observacion, double descuento, String motivo) {
		super();
		this.idPedido = idPedido;
		this.idMarcacion = idMarcacion;
		this.observacion = observacion;
		this.descuento = descuento;
		this.motivo = motivo;
	}
    
	
	

}
