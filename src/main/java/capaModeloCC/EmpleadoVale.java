package capaModeloCC;

public class EmpleadoVale {
	
	private int idEmpleadoVale;
	private int idEmpleado;
	private String fecha;
	private double valor;
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
	public EmpleadoVale(int idEmpleadoVale, int idEmpleado, String fecha, double valor) {
		super();
		this.idEmpleadoVale = idEmpleadoVale;
		this.idEmpleado = idEmpleado;
		this.fecha = fecha;
		this.valor = valor;
	}
	
	

}
