package capaModeloCC;

public class Estadistica {
	
	private String estadistica;
	private long cantidad;
	private double total;
	public String getEstadistica() {
		return estadistica;
	}
	public void setEstadistica(String estadistica) {
		this.estadistica = estadistica;
	}
	public long getCantidad() {
		return cantidad;
	}
	public void setCantidad(long cantidad) {
		this.cantidad = cantidad;
	}
	public double getTotal() {
		return total;
	}
	public void setTotal(double total) {
		this.total = total;
	}
	public Estadistica(String estadistica, long cantidad, double total) {
		super();
		this.estadistica = estadistica;
		this.cantidad = cantidad;
		this.total = total;
	}
	
}
