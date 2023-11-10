package capaModeloCC;

/**
 * Clase de la capa modelo que define los atributos de la clase SolicitudPQRS
 * @author JuanDavid
 *
 */
public class SolicitudPQRS {
	
	private int idsolicitud;
	private String fechaSolicitud;
	private String tipoSolicitud;
	private int idcliente;
	private int idtienda;
	private String nombres;
	private String apellidos;
	private String telefono;
	private String direccion;
	private String zona;
	private int idmunicipio;
	private String comentario;
	private int idOrigen;
	private String origen;
	private int idFoco;
	private String foco;
	private String tipo;
	private String areaResponsable;
	private int imagenes;
	
	
	public int getImagenes() {
		return imagenes;
	}
	public void setImagenes(int imagenes) {
		this.imagenes = imagenes;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public String getAreaResponsable() {
		return areaResponsable;
	}
	public void setAreaResponsable(String areaResponsable) {
		this.areaResponsable = areaResponsable;
	}
	public String getFoco() {
		return foco;
	}
	public void setFoco(String foco) {
		this.foco = foco;
	}
	public int getIdFoco() {
		return idFoco;
	}
	public void setIdFoco(int idFoco) {
		this.idFoco = idFoco;
	}
	public String getOrigen() {
		return origen;
	}
	public void setOrigen(String origen) {
		this.origen = origen;
	}
	public int getIdOrigen() {
		return idOrigen;
	}
	public void setIdOrigen(int idOrigen) {
		this.idOrigen = idOrigen;
	}
	public int getIdsolicitud() {
		return idsolicitud;
	}
	public void setIdsolicitud(int idsolicitud) {
		this.idsolicitud = idsolicitud;
	}
	public String getFechaSolicitud() {
		return fechaSolicitud;
	}
	public void setFechaSolicitud(String fechaSolicitud) {
		this.fechaSolicitud = fechaSolicitud;
	}
	public String getTipoSolicitud() {
		return tipoSolicitud;
	}
	public void setTipoSolicitud(String tipoSolicitud) {
		this.tipoSolicitud = tipoSolicitud;
	}
	public int getIdcliente() {
		return idcliente;
	}
	public void setIdcliente(int idcliente) {
		this.idcliente = idcliente;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public String getNombres() {
		return nombres;
	}
	public void setNombres(String nombres) {
		this.nombres = nombres;
	}
	public String getApellidos() {
		return apellidos;
	}
	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getDireccion() {
		return direccion;
	}
	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}
	public String getZona() {
		return zona;
	}
	public void setZona(String zona) {
		this.zona = zona;
	}
	public int getIdmunicipio() {
		return idmunicipio;
	}
	public void setIdmunicipio(int idmunicipio) {
		this.idmunicipio = idmunicipio;
	}
	public String getComentario() {
		return comentario;
	}
	public void setComentario(String comentario) {
		this.comentario = comentario;
	}
	public SolicitudPQRS(int idsolicitud, String fechaSolicitud, String tipoSolicitud, int idcliente, int idtienda,
			String nombres, String apellidos, String telefono, String direccion, String zona, int idmunicipio,
			String comentario, int idOrigen, int idFoco, String tipo, String areaResponsable) {
		super();
		this.idsolicitud = idsolicitud;
		this.fechaSolicitud = fechaSolicitud;
		this.tipoSolicitud = tipoSolicitud;
		this.idcliente = idcliente;
		this.idtienda = idtienda;
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.telefono = telefono;
		this.direccion = direccion;
		this.zona = zona;
		this.idmunicipio = idmunicipio;
		this.comentario = comentario;
		this.idOrigen = idOrigen;
		this.idFoco = idFoco;
		this.tipo = tipo;
		this.areaResponsable = areaResponsable;
	}
	
	
	
	
	

}
