package capaModeloCC;

public class OfertaCliente {
	
	private int idOfertaCliente;
	private int idOferta;
	private String nombreOferta;
	private int idCliente;
	private String nombreCliente;
	private String utilizada;
	private int PQRS;
	private String ingresoOferta;
	private String usuarioIngreso;
	private String usoOferta;
	private String usuarioUso;
	private String observacion;
	private String codigoPromocion;
	private String fechaMensaje;
	private String fechaCaducidad;
	private int idClienteRedimio;
	private double saldo;
	private String redParcial;
	
	

	public String getRedParcial() {
		return redParcial;
	}
	public void setRedParcial(String redParcial) {
		this.redParcial = redParcial;
	}
	public double getSaldo() {
		return saldo;
	}
	public void setSaldo(double saldo) {
		this.saldo = saldo;
	}
	public String getFechaMensaje() {
		return fechaMensaje;
	}
	public void setFechaMensaje(String fechaMensaje) {
		this.fechaMensaje = fechaMensaje;
	}
	public int getIdClienteRedimio() {
		return idClienteRedimio;
	}
	public void setIdClienteRedimio(int idClienteRedimio) {
		this.idClienteRedimio = idClienteRedimio;
	}
	public String getNombreCliente() {
		return nombreCliente;
	}
	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}
	public String getFechaCaducidad() {
		return fechaCaducidad;
	}
	public void setFechaCaducidad(String fechaCaducidad) {
		this.fechaCaducidad = fechaCaducidad;
	}
	public String getUsuarioIngreso() {
		return usuarioIngreso;
	}
	public void setUsuarioIngreso(String usuarioIngreso) {
		this.usuarioIngreso = usuarioIngreso;
	}
	public String getUsuarioUso() {
		return usuarioUso;
	}
	public void setUsuarioUso(String usuarioUso) {
		this.usuarioUso = usuarioUso;
	}
	public String getCodigoPromocion() {
		return codigoPromocion;
	}
	public void setCodigoPromocion(String codigoPromocion) {
		this.codigoPromocion = codigoPromocion;
	}
	public int getPQRS() {
		return PQRS;
	}
	public void setPQRS(int pQRS) {
		PQRS = pQRS;
	}
	public String getObservacion() {
		return observacion;
	}
	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}
	public String getNombreOferta() {
		return nombreOferta;
	}
	public void setNombreOferta(String nombreOferta) {
		this.nombreOferta = nombreOferta;
	}
	public String getIngresoOferta() {
		return ingresoOferta;
	}
	public void setIngresoOferta(String ingresoOferta) {
		this.ingresoOferta = ingresoOferta;
	}
	public String getUsoOferta() {
		return usoOferta;
	}
	public void setUsoOferta(String usoOferta) {
		this.usoOferta = usoOferta;
	}
	public int getIdOfertaCliente() {
		return idOfertaCliente;
	}
	public void setIdOfertaCliente(int idOfertaCliente) {
		this.idOfertaCliente = idOfertaCliente;
	}
	public int getIdOferta() {
		return idOferta;
	}
	public void setIdOferta(int idOferta) {
		this.idOferta = idOferta;
	}
	public int getIdCliente() {
		return idCliente;
	}
	public void setIdCliente(int idCliente) {
		this.idCliente = idCliente;
	}
	public String getUtilizada() {
		return utilizada;
	}
	public void setUtilizada(String utilizada) {
		this.utilizada = utilizada;
	}

	public OfertaCliente(int idOfertaCliente, int idOferta, int idCliente, String utilizada, int pqrs, String ingresoOferta,
			String usoOferta, String observacion, String usuarioIngreso) {
		super();
		this.idOfertaCliente = idOfertaCliente;
		this.idOferta = idOferta;
		this.idCliente = idCliente;
		this.utilizada = utilizada;
		this.ingresoOferta = ingresoOferta;
		this.usoOferta = usoOferta;
		this.observacion = observacion;
		this.PQRS = pqrs;
		this.usuarioIngreso = usuarioIngreso;
	}
	
	
	

}
