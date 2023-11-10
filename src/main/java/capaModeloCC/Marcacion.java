package capaModeloCC;

public class Marcacion {
	
	int idMarcacion;
	String nombreMarcacion;
	int estado;
	String marketplace;
	
	
	public String getMarketplace() {
		return marketplace;
	}
	public void setMarketplace(String marketplace) {
		this.marketplace = marketplace;
	}
	public int getIdMarcacion() {
		return idMarcacion;
	}
	public void setIdMarcacion(int idMarcacion) {
		this.idMarcacion = idMarcacion;
	}
	
	public int getEstado() {
		return estado;
	}
	public void setEstado(int estado) {
		this.estado = estado;
	}
	public String getNombreMarcacion() {
		return nombreMarcacion;
	}
	public void setNombreMarcacion(String nombreMarcacion) {
		this.nombreMarcacion = nombreMarcacion;
	}
	public Marcacion(int idMarcacion, String nombreMarcacion, int estado, String marketplace) {
		super();
		this.idMarcacion = idMarcacion;
		this.nombreMarcacion = nombreMarcacion;
		this.estado = estado;
		this.marketplace = marketplace;
	}

	
	
	
}
