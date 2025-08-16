package capaModeloCC;

public class PrioridadPqrs {
	

	private int idprioridad;
	private String descripcion;
	private int t_resp_min;
	private int t_resp_max;
	private String color;
	
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public int getIdprioridad() {
		return idprioridad;
	}
	public void setIdprioridad(int idprioridad) {
		this.idprioridad = idprioridad;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public int getT_resp_min() {
		return t_resp_min;
	}
	public void setT_resp_min(int t_resp_min) {
		this.t_resp_min = t_resp_min;
	}
	public int getT_resp_max() {
		return t_resp_max;
	}
	public void setT_resp_max(int t_resp_max) {
		this.t_resp_max = t_resp_max;
	}
	

	
	public PrioridadPqrs() {
		super();
	
	}
	
}
