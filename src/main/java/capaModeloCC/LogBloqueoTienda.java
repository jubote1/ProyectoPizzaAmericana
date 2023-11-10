package capaModeloCC;

public class LogBloqueoTienda {
	
	private int idLogBloqueo;
	private int idTienda;
	private String accion;
	private String fechaAccion;
	private String motivo;
	private String observacion;
	private String debloqueoEn;
	private String urlTienda;
	private String tienda;
	private String aprobado; 
	
	
	
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public String getAprobado() {
		return aprobado;
	}
	public void setAprobado(String aprobado) {
		this.aprobado = aprobado;
	}
	public int getIdLogBloqueo() {
		return idLogBloqueo;
	}
	public void setIdLogBloqueo(int idLogBloqueo) {
		this.idLogBloqueo = idLogBloqueo;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getAccion() {
		return accion;
	}
	public void setAccion(String accion) {
		this.accion = accion;
	}
	public String getFechaAccion() {
		return fechaAccion;
	}
	public void setFechaAccion(String fechaAccion) {
		this.fechaAccion = fechaAccion;
	}
	public String getMotivo() {
		return motivo;
	}
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
	public String getObservacion() {
		return observacion;
	}
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	public String getDebloqueoEn() {
		return debloqueoEn;
	}
	public void setDebloqueoEn(String debloqueoEn) {
		this.debloqueoEn = debloqueoEn;
	}
	
	public String getUrlTienda() {
		return urlTienda;
	}
	public void setUrlTienda(String urlTienda) {
		this.urlTienda = urlTienda;
	}
	public LogBloqueoTienda(int idLogBloqueo, int idTienda, String accion, String fechaAccion, String motivo,
			String observacion, String debloqueoEn, String urlTienda) {
		super();
		this.idLogBloqueo = idLogBloqueo;
		this.idTienda = idTienda;
		this.accion = accion;
		this.fechaAccion = fechaAccion;
		this.motivo = motivo;
		this.observacion = observacion;
		this.debloqueoEn = debloqueoEn;
		this.urlTienda = urlTienda;
	}
	public LogBloqueoTienda() {
		super();
	}
	
}
