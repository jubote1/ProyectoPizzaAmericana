package PedidoInfoAdicional;

public class Marcacion {
	
	int idMarcacion;
	String nombreMarcacion;
	int estado;
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
	public Marcacion(int idMarcacion, String nombreMarcacion, int estado) {
		super();
		this.idMarcacion = idMarcacion;
		this.nombreMarcacion = nombreMarcacion;
		this.estado = estado;
	}

	
	
	
}
