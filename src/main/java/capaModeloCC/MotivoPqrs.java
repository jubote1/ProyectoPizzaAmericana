package capaModeloCC;

public class MotivoPqrs {

	private int idmotivo;
	private String descripcion;
	private int idprioridad;
	private String tiposolicitud;
	
	
	public MotivoPqrs() {
		super();
	
	}


	public int getIdmotivo() {
		return idmotivo;
	}


	public void setIdmotivo(int idmotivo) {
		this.idmotivo = idmotivo;
	}


	public String getDescripcion() {
		return descripcion;
	}


	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}


	public int getIdprioridad() {
		return idprioridad;
	}


	public void setIdprioridad(int idprioridad) {
		this.idprioridad = idprioridad;
	}


	public String getTiposolicitud() {
		return tiposolicitud;
	}


	public void setTiposolicitud(String tiposolicitud) {
		this.tiposolicitud = tiposolicitud;
	}
}
