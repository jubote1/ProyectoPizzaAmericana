package capaModeloCC;

public class MensajeTexto {
	
	private int idOferta;
	private int idOfertaCliente;
	private String nombreCliente;
	private String apellidoCliente;
	private String numeroCelular;
	private String telefono;
	private String mensaje1;
	private String mensaje2;
	private String codigoPromocion;
	private String email;
	
	
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCodigoPromocion() {
		return codigoPromocion;
	}
	public void setCodigoPromocion(String codigoPromocion) {
		this.codigoPromocion = codigoPromocion;
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
	public String getNombreCliente() {
		return nombreCliente;
	}
	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}
	public String getApellidoCliente() {
		return apellidoCliente;
	}
	public void setApellidoCliente(String apellidoCliente) {
		this.apellidoCliente = apellidoCliente;
	}
	public String getNumeroCelular() {
		return numeroCelular;
	}
	public void setNumeroCelular(String numeroCelular) {
		this.numeroCelular = numeroCelular;
	}
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getMensaje1() {
		return mensaje1;
	}
	public void setMensaje1(String mensaje1) {
		this.mensaje1 = mensaje1;
	}
	public String getMensaje2() {
		return mensaje2;
	}
	public void setMensaje2(String mensaje2) {
		this.mensaje2 = mensaje2;
	}
	public MensajeTexto(int idOferta, int idOfertaCliente, String nombreCliente, String apellidoCliente, String numeroCelular,
			String telefono, String mensaje1, String mensaje2, String codigoPromocion) {
		super();
		this.idOferta = idOferta;
		this.idOfertaCliente = idOfertaCliente;
		this.nombreCliente = nombreCliente;
		this.apellidoCliente = apellidoCliente;
		this.numeroCelular = numeroCelular;
		this.telefono = telefono;
		this.mensaje1 = mensaje1;
		this.mensaje2 = mensaje2;
		this.codigoPromocion = codigoPromocion;
	}
	
	
	

}
