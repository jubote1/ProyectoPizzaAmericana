package capaModeloCC;

import java.util.List;

public class PendientePqrs {
	
	private int idsolicitud;
	private String tiposolicitud;
	private String nombres;
	private String nombre_origen;
	
	public String getNombre_origen() {
		return nombre_origen;
	}
	public void setNombre_origen(String nombre_origen) {
		this.nombre_origen = nombre_origen;
	}
	public List<ComentarioPqrs> getComentarios() {
		return comentarios;
	}
	public void setComentarios(List<ComentarioPqrs> comentarios) {
		this.comentarios = comentarios;
	}


	private List<ComentarioPqrs> comentarios; // ← nuevo
	  
	  
	public int getIdsolicitud() {
		return idsolicitud;
	}
	public void setIdsolicitud(int idsolicitud) {
		this.idsolicitud = idsolicitud;
	}
	public String getTiposolicitud() {
		return tiposolicitud;
	}
	public void setTiposolicitud(String tiposolicitud) {
		this.tiposolicitud = tiposolicitud;
	}
	public String getNombres() {
		return nombres;
	}
	public void setNombres(String nombres) {
		this.nombres = nombres;
	}
	
	
    public PendientePqrs() {
    	super();
    }

}
