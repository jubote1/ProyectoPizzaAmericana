package capaModeloCC;

/**
 * Rol de seguridad (Operario, Lider Administrativo, Administrador
 * PQRS/Contact Center...). Reemplaza el booleano usuario.administrador
 * (N/S/P) por un catalogo abierto: agregar o renombrar un rol es un INSERT,
 * no un cambio de codigo.
 */
public class Rol {

	private int idRol;
	private String nombre;
	private String descripcion;
	private String activo;

	public Rol() {
	}

	public Rol(int idRol, String nombre, String descripcion, String activo) {
		this.idRol = idRol;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.activo = activo;
	}

	public Rol(String nombre, String descripcion) {
		this.nombre = nombre;
		this.descripcion = descripcion;
	}

	public int getIdRol() {
		return idRol;
	}

	public void setIdRol(int idRol) {
		this.idRol = idRol;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getActivo() {
		return activo;
	}

	public void setActivo(String activo) {
		this.activo = activo;
	}

}
