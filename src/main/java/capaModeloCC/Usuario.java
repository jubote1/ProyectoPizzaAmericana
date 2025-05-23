package capaModeloCC;

/**
 * Clase que implementa la entidad Usuario.
 * @author JuanDavid
 *
 */
public class Usuario {
	
	private int id;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	private String nombreUsuario;
	private String contrasena;
	private String nombreLargo;
	private String plataforma;
	private boolean activo;
	
	public boolean isActivo() {
		return activo;
	}
	public void setActivo(boolean activo) {
		this.activo = activo;
	}
	public String getPlataforma() {
		return plataforma;
	}
	public void setPlataforma(String plataforma) {
		this.plataforma = plataforma;
	}
	public String getNombreUsuario() {
		return nombreUsuario;
	}
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	public String getContrasena() {
		return contrasena;
	}
	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}
	public String getNombreLargo() {
		return nombreLargo;
	}
	public void setNombreLargo(String nombreLargo) {
		this.nombreLargo = nombreLargo;
	}
	public Usuario(String nombreUsuario, String contrasena, String nombreLargo) {
		super();
		this.nombreUsuario = nombreUsuario;
		this.contrasena = contrasena;
		this.nombreLargo = nombreLargo;
	}
	public Usuario(String nombreUsuario) {
		super();
		this.nombreUsuario = nombreUsuario;
	}
	
	public Usuario() {
		
	
	}
	
	
	

}
