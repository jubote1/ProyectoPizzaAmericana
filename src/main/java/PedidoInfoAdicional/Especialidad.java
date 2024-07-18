package PedidoInfoAdicional;

/**
 * Clase que implementa la entidad Especialidad
 * @author JuanDavid
 *
 */
public class Especialidad {

	private int idespecialidad;
	private String nombre;
	private String abreviatura;
	
	public int getIdespecialidad() {
		return idespecialidad;
	}
	public void setIdespecialidad(int idespecialidad) {
		this.idespecialidad = idespecialidad;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getAbreviatura() {
		return abreviatura;
	}
	public void setAbreviatura(String abreviatura) {
		this.abreviatura = abreviatura;
	}
	public Especialidad(int idespecialidad, String nombre, String abreviatura) {
		super();
		this.idespecialidad = idespecialidad;
		this.nombre = nombre;
		this.abreviatura = abreviatura;
	}
	public Especialidad(String nombre, String abreviatura) {
		super();
		this.nombre = nombre;
		this.abreviatura = abreviatura;
	}
	
	
	
	
}
