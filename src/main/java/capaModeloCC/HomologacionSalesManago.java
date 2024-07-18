package capaModeloCC;

public class HomologacionSalesManago {
	
	private String id;
	private String nombre;
	private String tamano;
	private String name;
	private String especialidad;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getTamano() {
		return tamano;
	}
	public void setTamano(String tamano) {
		this.tamano = tamano;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEspecialidad() {
		return especialidad;
	}
	public void setEspecialidad(String especialidad) {
		this.especialidad = especialidad;
	}
	public HomologacionSalesManago(String tamano, String name, String especialidad) {
		super();
		this.tamano = tamano;
		this.name = name;
		this.especialidad = especialidad;
	}
	public HomologacionSalesManago(String id, String nombre, String tamano, String name, String especialidad) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.tamano = tamano;
		this.name = name;
		this.especialidad = especialidad;
	}
	
	public HomologacionSalesManago() {
		super();
	}
	
	

}
