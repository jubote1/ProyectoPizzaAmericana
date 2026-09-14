package capaModeloCC;

public class Ubicacion {
	
	private double latitud;
	private double longitud;
	private String direccion;

	public String getDireccion() {
		return direccion;
	}
	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}
	public double getLatitud() {
		return latitud;
	}
	public void setLatitud(double latitud) {
		this.latitud = latitud;
	}
	public double getLongitud() {
		return longitud;
	}
	public void setLongitud(double longitud) {
		this.longitud = longitud;
	}
	public Ubicacion(double latitud, double longitud) {
		super();
		this.latitud = latitud;
		this.longitud = longitud;
	}
	public Ubicacion(double latitud, double longitud, String direccion) {
		super();
		this.latitud = latitud;
		this.longitud = longitud;
		this.direccion = direccion;
	}
	
	

}
