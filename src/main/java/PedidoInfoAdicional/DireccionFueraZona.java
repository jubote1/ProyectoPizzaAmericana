package PedidoInfoAdicional;

public class DireccionFueraZona {
	
	private int id;
	private String direccion;
	private String municipio;
	private int idCliente;
	private double latitud;
	private double longitud;
	private double valor;
	private String Telefono;
	private String nombre;
	private String apellido;
	private String fechaIngreso;
	
	
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	public String getFechaIngreso() {
		return fechaIngreso;
	}
	public void setFechaIngreso(String fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDireccion() {
		return direccion;
	}
	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}
	public String getMunicipio() {
		return municipio;
	}
	public void setMunicipio(String municipio) {
		this.municipio = municipio;
	}
	public int getIdCliente() {
		return idCliente;
	}
	public void setIdCliente(int idCliente) {
		this.idCliente = idCliente;
	}
	public double getLatitud() {
		return latitud;
	}
	public void setLatitud(double latitud) {
		this.latitud = latitud;
	}
	public double getLongitud() {
		return longitud;
	}
	public void setLongitud(double longitud) {
		this.longitud = longitud;
	}
	public String getTelefono() {
		return Telefono;
	}
	public void setTelefono(String telefono) {
		Telefono = telefono;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getApellido() {
		return apellido;
	}
	public void setApellido(String apellido) {
		this.apellido = apellido;
	}
	public DireccionFueraZona(int id, String direccion, String municipio, int idCliente, double latitud, double longitud,
			String telefono, String nombre, String apellido, double valor) {
		super();
		this.id = id;
		this.direccion = direccion;
		this.municipio = municipio;
		this.idCliente = idCliente;
		this.latitud = latitud;
		this.longitud = longitud;
		Telefono = telefono;
		this.nombre = nombre;
		this.apellido = apellido;
		this.valor = valor;
	}
}
