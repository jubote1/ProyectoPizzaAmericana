package capaModeloCC;

public class ClienteFidelizacion {
	
	private String correo;
	private String fecha;
	private String activo;
	private double puntosVigentes;
	public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public String getActivo() {
		return activo;
	}
	public void setActivo(String activo) {
		this.activo = activo;
	}
	public double getPuntosVigentes() {
		return puntosVigentes;
	}
	public void setPuntosVigentes(double puntosVigentes) {
		this.puntosVigentes = puntosVigentes;
	}
	public ClienteFidelizacion() {
		super();
	}
	public ClienteFidelizacion(String correo, String fecha, String activo, double puntosVigentes) {
		super();
		this.correo = correo;
		this.fecha = fecha;
		this.activo = activo;
		this.puntosVigentes = puntosVigentes;
	}
	
	
	
}
