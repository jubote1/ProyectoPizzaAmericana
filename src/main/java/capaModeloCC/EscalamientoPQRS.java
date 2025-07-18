package capaModeloCC;

public class EscalamientoPQRS {
	
	private int idEscalamiento;
	private int idSolicitudPQRS;
	private String areaResponsable;
	private String fechaEscalamiento;
	private String fechaResolucion;
	private boolean solucionado;
	public int getIdEscalamiento() {
		return idEscalamiento;
	}
	public void setIdEscalamiento(int idEscalamiento) {
		this.idEscalamiento = idEscalamiento;
	}
	public int getIdSolicitudPQRS() {
		return idSolicitudPQRS;
	}
	public void setIdSolicitudPQRS(int idSolicitudPQRS) {
		this.idSolicitudPQRS = idSolicitudPQRS;
	}
	public String getAreaResponsable() {
		return areaResponsable;
	}
	public void setAreaResponsable(String areaResponsable) {
		this.areaResponsable = areaResponsable;
	}
	public String getFechaEscalamiento() {
		return fechaEscalamiento;
	}
	public void setFechaEscalamiento(String fechaEscalamiento) {
		this.fechaEscalamiento = fechaEscalamiento;
	}
	public String getFechaResolucion() {
		return fechaResolucion;
	}
	public void setFechaResolucion(String fechaResolucion) {
		this.fechaResolucion = fechaResolucion;
	}
	public boolean isSolucionado() {
		return solucionado;
	}
	public void setSolucionado(boolean solucionado) {
		this.solucionado = solucionado;
	}
	public EscalamientoPQRS(int idEscalamiento, int idSolicitudPQRS, String areaResponsable, String fechaEscalamiento,
			String fechaResolucion, boolean solucionado) {
		super();
		this.idEscalamiento = idEscalamiento;
		this.idSolicitudPQRS = idSolicitudPQRS;
		this.areaResponsable = areaResponsable;
		this.fechaEscalamiento = fechaEscalamiento;
		this.fechaResolucion = fechaResolucion;
		this.solucionado = solucionado;
	}
	public EscalamientoPQRS(int idSolicitudPQRS, String areaResponsable) {
		super();
		this.idSolicitudPQRS = idSolicitudPQRS;
		this.areaResponsable = areaResponsable;
	}
	
	

}
