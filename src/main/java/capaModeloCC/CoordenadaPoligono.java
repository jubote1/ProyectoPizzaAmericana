package capaModeloCC;

public class CoordenadaPoligono {

	private int idCoordenada;
	private int idPoligono;
	private double latitud;
	private double longitud;
	public int getIdCoordenada() {
		return idCoordenada;
	}
	public void setIdCoordenada(int idCoordenada) {
		this.idCoordenada = idCoordenada;
	}
	public int getIdPoligono() {
		return idPoligono;
	}
	public void setIdPoligono(int idPoligono) {
		this.idPoligono = idPoligono;
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
	public CoordenadaPoligono(int idCoordenada, int idPoligono, double latitud, double longitud) {
		super();
		this.idCoordenada = idCoordenada;
		this.idPoligono = idPoligono;
		this.latitud = latitud;
		this.longitud = longitud;
	}
	
	
}
