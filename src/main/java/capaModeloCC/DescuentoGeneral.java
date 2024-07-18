package capaModeloCC;

public class DescuentoGeneral {
	
	private int idDescuento;
	private int idProducto;
	private String aplicaExcepcion;
	private int valorPorcentaje;
	private double valorPesos;
	private String tipoDescuento;
	
	
	
	public String getTipoDescuento() {
		return tipoDescuento;
	}
	public void setTipoDescuento(String tipoDescuento) {
		this.tipoDescuento = tipoDescuento;
	}
	public int getIdDescuento() {
		return idDescuento;
	}
	public void setIdDescuento(int idDescuento) {
		this.idDescuento = idDescuento;
	}
	public int getIdProducto() {
		return idProducto;
	}
	public void setIdProducto(int idProducto) {
		this.idProducto = idProducto;
	}
	public String getAplicaExcepcion() {
		return aplicaExcepcion;
	}
	public void setAplicaExcepcion(String aplicaExcepcion) {
		this.aplicaExcepcion = aplicaExcepcion;
	}
	public int getValorPorcentaje() {
		return valorPorcentaje;
	}
	public void setValorPorcentaje(int valorPorcentaje) {
		this.valorPorcentaje = valorPorcentaje;
	}
	public double getValorPesos() {
		return valorPesos;
	}
	public void setValorPesos(double valorPesos) {
		this.valorPesos = valorPesos;
	}
	public DescuentoGeneral(int idDescuento, int idProducto, String aplicaExcepcion, int valorPorcentaje,
			double valorPesos, String tipoDescuento) {
		super();
		this.idDescuento = idDescuento;
		this.idProducto = idProducto;
		this.aplicaExcepcion = aplicaExcepcion;
		this.valorPorcentaje = valorPorcentaje;
		this.valorPesos = valorPesos;
		this.tipoDescuento = tipoDescuento;
	}
	
}
