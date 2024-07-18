package capaModeloCC;

public class EncuestaLaboral {
	
	private int idEncuesta;
	private String descripcion;
	private String dependencia;
	private String nombreEncuesta;
	private String codigo;
	private String version;
	private String encabezado;
	
	
	
	public String getEncabezado() {
		return encabezado;
	}
	public void setEncabezado(String encabezado) {
		this.encabezado = encabezado;
	}
	public int getIdEncuesta() {
		return idEncuesta;
	}
	public void setIdEncuesta(int idEncuesta) {
		this.idEncuesta = idEncuesta;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public String getDependencia() {
		return dependencia;
	}
	public void setDependencia(String dependencia) {
		this.dependencia = dependencia;
	}
	public String getNombreEncuesta() {
		return nombreEncuesta;
	}
	public void setNombreEncuesta(String nombreEncuesta) {
		this.nombreEncuesta = nombreEncuesta;
	}
	public String getCodigo() {
		return codigo;
	}
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public EncuestaLaboral(int idEncuesta, String descripcion, String dependencia, String nombreEncuesta, String codigo,
			String version, String encabezado) {
		super();
		this.idEncuesta = idEncuesta;
		this.descripcion = descripcion;
		this.dependencia = dependencia;
		this.nombreEncuesta = nombreEncuesta;
		this.codigo = codigo;
		this.version = version;
		this.encabezado = encabezado;
	}
	
	
	

}
