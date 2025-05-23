package capaModeloCC;

public class EstadoPqrs {
	
	private int idestado ;
	private String descripcion;
	
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	public EstadoPqrs() {
		super();
	
	}
	
	public EstadoPqrs(int idestado, String descripcion) {
		super();
		this.idestado = idestado;
		this.descripcion = descripcion;
	}
	public int getIdestado() {
		return idestado;
	}
	public void setIdestado(int idestado) {
		this.idestado = idestado;
	}
	

}
