package capaModeloCC;

public class ResumenVentaEmpresarial {
	
	private String asesor;
	private double totalVenta;
	private double comision;
	public String getAsesor() {
		return asesor;
	}
	public void setAsesor(String asesor) {
		this.asesor = asesor;
	}
	public double getTotalVenta() {
		return totalVenta;
	}
	public void setTotalVenta(double totalVenta) {
		this.totalVenta = totalVenta;
	}
	public double getComision() {
		return comision;
	}
	public void setComision(double comision) {
		this.comision = comision;
	}
	public ResumenVentaEmpresarial(String asesor, double totalVenta, double comision) {
		super();
		this.asesor = asesor;
		this.totalVenta = totalVenta;
		this.comision = comision;
	}
	
	
}
