package capaModeloCC;

public class EmpleadoVale {
	
	private int idEmpleadoVale;
	private int idEmpleado;
	private String fecha;
	private double valor;
	private String descuadre;
	private int idEgreso;
	
	
	
	
	public int getIdEgreso() {
		return idEgreso;
	}
	public void setIdEgreso(int idEgreso) {
		this.idEgreso = idEgreso;
	}
	public String getDescuadre() {
		return descuadre;
	}
	public void setDescuadre(String descuadre) {
		this.descuadre = descuadre;
	}
	public int getIdEmpleadoVale() {
		return idEmpleadoVale;
	}
	public void setIdEmpleadoVale(int idEmpleadoVale) {
		this.idEmpleadoVale = idEmpleadoVale;
	}
	public int getIdEmpleado() {
		return idEmpleado;
	}
	public void setIdEmpleado(int idEmpleado) {
		this.idEmpleado = idEmpleado;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	public EmpleadoVale(int idEmpleadoVale, int idEmpleado, String fecha, double valor, String descuadre, int idEgreso) {
		super();
		this.idEmpleadoVale = idEmpleadoVale;
		this.idEmpleado = idEmpleado;
		this.fecha = fecha;
		this.valor = valor;
		this.descuadre = descuadre;
		this.idEgreso = idEgreso;
	}
	
	

}
