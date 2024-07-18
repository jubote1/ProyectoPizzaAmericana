package capaModeloCC;

/**
 * Clase que implementa la entidad Municipio
 * @author JuanDavid
 *
 */
public class Municipio {
	
	private int idmunicipio;
	private String nombre;
	public int getIdmunicipio() {
		return idmunicipio;
	}
	public void setIdmunicipio(int idmunicipio) {
		this.idmunicipio = idmunicipio;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public Municipio(int idmunicipio, String nombre) {
		super();
		this.idmunicipio = idmunicipio;
		this.nombre = nombre;
	}
	
	

}
