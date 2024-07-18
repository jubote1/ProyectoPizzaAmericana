package PedidoInfoAdicional;

public class Poligono {
	
	private int idPoligono;
	private String nombrePoligo;
	private String ubicacionMapa;
	public int getIdPoligono() {
		return idPoligono;
	}
	public void setIdPoligono(int idPoligono) {
		this.idPoligono = idPoligono;
	}
	public String getNombrePoligo() {
		return nombrePoligo;
	}
	public void setNombrePoligo(String nombrePoligo) {
		this.nombrePoligo = nombrePoligo;
	}
	public String getUbicacionMapa() {
		return ubicacionMapa;
	}
	public void setUbicacionMapa(String ubicacionMapa) {
		this.ubicacionMapa = ubicacionMapa;
	}
	public Poligono(int idPoligono, String nombrePoligo, String ubicacionMapa) {
		super();
		this.idPoligono = idPoligono;
		this.nombrePoligo = nombrePoligo;
		this.ubicacionMapa = ubicacionMapa;
	}
	
	

}
