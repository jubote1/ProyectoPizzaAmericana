package capaModeloCC;

public class MarcacionPedido {
	
	private int idPedido;
	private int idMarcacion;
	private String observacion;
	private double descuento;
	private String motivo;
	private String nombreMarcacion;
	private String marketplace;
	private String descuentoAsumido;
	private double descuentoPlataforma;
	private double tarifaAdicional;
	private double propina;
	private String log;
	
	
	
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	public double getDescuentoPlataforma() {
		return descuentoPlataforma;
	}
	public void setDescuentoPlataforma(double descuentoPlataforma) {
		this.descuentoPlataforma = descuentoPlataforma;
	}
	public double getTarifaAdicional() {
		return tarifaAdicional;
	}
	public void setTarifaAdicional(double tarifaAdicional) {
		this.tarifaAdicional = tarifaAdicional;
	}
	public double getPropina() {
		return propina;
	}
	public void setPropina(double propina) {
		this.propina = propina;
	}
	public String getDescuentoAsumido() {
		return descuentoAsumido;
	}
	public void setDescuentoAsumido(String descuentoAsumido) {
		this.descuentoAsumido = descuentoAsumido;
	}
	public String getMarketplace() {
		return marketplace;
	}
	public void setMarketplace(String marketplace) {
		this.marketplace = marketplace;
	}
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
//	public MarcacionPedido(int idPedido, int idMarcacion, String observacion, double descuento, String motivo, String marketplace, String descuentoAsumido) {
//		super();
//		this.idPedido = idPedido;
//		this.idMarcacion = idMarcacion;
//		this.observacion = observacion;
//		this.descuento = descuento;
//		this.motivo = motivo;
//		this.marketplace = marketplace;
//		this.descuentoAsumido = descuentoAsumido;
//	}
	public MarcacionPedido(int idPedido, int idMarcacion, String observacion, double descuento, String motivo,
			 String marketplace, String descuentoAsumido, double descuentoPlataforma,
			double tarifaAdicional, double propina, String log) {
		super();
		this.idPedido = idPedido;
		this.idMarcacion = idMarcacion;
		this.observacion = observacion;
		this.descuento = descuento;
		this.motivo = motivo;
		this.marketplace = marketplace;
		this.descuentoAsumido = descuentoAsumido;
		this.descuentoPlataforma = descuentoPlataforma;
		this.tarifaAdicional = tarifaAdicional;
		this.propina = propina;
		this.log = log;
	}
	
    
	
	

}
