package capaModeloCC;

/**
 * Una pantalla funcional (una unidad del menu, ej. "Especialidades" ->
 * Especialidad.html), no un servlet crudo. El permiso se otorga a nivel de
 * pantalla (rol_pantalla); pantalla_servlet dice que endpoints la respaldan.
 */
public class Pantalla {

	private int idPantalla;
	private String nombre;
	private int idModulo;
	private String nombreModulo;
	private String urlHtml;
	private int orden;
	private String activo;

	public Pantalla() {
	}

	public Pantalla(int idPantalla, String nombre, int idModulo, String urlHtml, int orden, String activo) {
		this.idPantalla = idPantalla;
		this.nombre = nombre;
		this.idModulo = idModulo;
		this.urlHtml = urlHtml;
		this.orden = orden;
		this.activo = activo;
	}

	public int getIdPantalla() {
		return idPantalla;
	}

	public void setIdPantalla(int idPantalla) {
		this.idPantalla = idPantalla;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getIdModulo() {
		return idModulo;
	}

	public void setIdModulo(int idModulo) {
		this.idModulo = idModulo;
	}

	public String getNombreModulo() {
		return nombreModulo;
	}

	public void setNombreModulo(String nombreModulo) {
		this.nombreModulo = nombreModulo;
	}

	public String getUrlHtml() {
		return urlHtml;
	}

	public void setUrlHtml(String urlHtml) {
		this.urlHtml = urlHtml;
	}

	public int getOrden() {
		return orden;
	}

	public void setOrden(int orden) {
		this.orden = orden;
	}

	public String getActivo() {
		return activo;
	}

	public void setActivo(String activo) {
		this.activo = activo;
	}

}
