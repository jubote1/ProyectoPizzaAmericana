package capaModeloCC;

/**
 * Clase de la capa modelo que define los atributos de la clase SolicitudPQRS
 * @author JuanDavid
 *
 */
public class SolicitudPQRS {
	
	private int idsolicitud;
	public int getIdmotivo() {
		return idmotivo;
	}
	public void setIdmotivo(int idmotivo) {
		this.idmotivo = idmotivo;
	}
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
	private int idpedidotienda;
	private double valorPedido;
	private double valorDescuento;
	private int porcentajeDescuento;
	private boolean descuentoRedimido;
	private int idpedidoredencion;
	private int idusuarioRegistro;
	private int idusuarioRedencion;
	private int idestado;
	private String NombreEstado;
	private int idprioridad;
	private int idmotivo;
	private boolean ccVinculado;
	private String tienda;
	private String prioridad;
	

	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public String getPrioridad() {
		return prioridad;
	}
	public void setPrioridad(String prioridad) {
		this.prioridad = prioridad;
	}
	public boolean isCcVinculado() {
		return ccVinculado;
	}
	public void setCcVinculado(boolean ccVinculado) {
		this.ccVinculado = ccVinculado;
	}
	public int getIdprioridad() {
		return idprioridad;
	}
	public void setIdprioridad(int idprioridad) {
		this.idprioridad = idprioridad;
	}
	public String getNombreEstado() {
		return NombreEstado;
	}
	public void setNombreEstado(String nombreEstado) {
		NombreEstado = nombreEstado;
	}
	public int getIdestado() {
		return idestado;
	}
	public void setIdestado(int idestado) {
		this.idestado = idestado;
	}
	public int getIdusuarioRegistro() {
		return idusuarioRegistro;
	}
	public void setIdusuarioRegistro(int idusuarioRegistro) {
		this.idusuarioRegistro = idusuarioRegistro;
	}
	public int getIdusuarioRedencion() {
		return idusuarioRedencion;
	}
	public void setIdusuarioRedencion(int idusuarioRedencion) {
		this.idusuarioRedencion = idusuarioRedencion;
	}
	public int getIdpedidoredencion() {
		return idpedidoredencion;
	}
	public void setIdpedidoredencion(int idpedidoredencion) {
		this.idpedidoredencion = idpedidoredencion;
	}
	public boolean isDescuentoRedimido() {
		return descuentoRedimido;
	}
	public void setDescuentoRedimido(boolean descuentoRedimido) {
		this.descuentoRedimido = descuentoRedimido;
	}
	public int getPorcentajeDescuento() {
		return porcentajeDescuento;
	}
	public void setPorcentajeDescuento(int porcentajeDescuento) {
		this.porcentajeDescuento = porcentajeDescuento;
	}
	public int getIdpedidotienda() {
		return idpedidotienda;
	}
	public void setIdpedidotienda(int idpedidotienda) {
		this.idpedidotienda = idpedidotienda;
	}
	public double getValorPedido() {
		return valorPedido;
	}
	public void setValorPedido(double valorPedido) {
		this.valorPedido = valorPedido;
	}
	public double getValorDescuento() {
		return valorDescuento;
	}
	public void setValorDescuento(double valorDescuento) {
		this.valorDescuento = valorDescuento;
	}
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
			String comentario, int idOrigen, int idFoco, String tipo, String areaResponsable,int idpedidotienda, double valorPedido , double valorDescuento,int porcentajeDescuento,boolean descuentoRedimido,int idpedidoredencion,int idusuarioRegistro,int idusuarioRedencion,int idestado,int idprioridad,int idmotivo, boolean ccVinculado) {
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
		this.idpedidotienda = idpedidotienda;
		this.valorPedido = valorPedido;
		this.valorDescuento = valorDescuento;
		this.porcentajeDescuento = porcentajeDescuento;
		this.descuentoRedimido = descuentoRedimido;
		this.idpedidoredencion =idpedidoredencion;
		this.idusuarioRegistro =idusuarioRegistro;
		this.idusuarioRedencion =idusuarioRedencion;
		this.idestado =idestado;
		this.idprioridad =idprioridad;
		this.idmotivo =idmotivo;
		this.ccVinculado =ccVinculado;
		
		
		
	}
	
	
	
	
	

}
