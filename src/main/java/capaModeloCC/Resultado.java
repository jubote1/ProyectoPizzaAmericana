package capaModeloCC;

public class Resultado {
	
	private String resultado;
	private String infoAdicional;
	private String estadoTienda;
	private String direccion;
	private double latitud;
	private double longitud;
	private String proveedorGeocodificacion;
	private String correccionAplicada;
	private String municipioOriginal;
	private String municipioCorregido;
	private String direccionCorregida;
	private String direccionOriginalNormalizada;
	
	public String getProveedorGeocodificacion() {
		return proveedorGeocodificacion;
	}

	public void setProveedorGeocodificacion(String proveedorGeocodificacion) {
		this.proveedorGeocodificacion = proveedorGeocodificacion;
	}

	public String getCorreccionAplicada() {
		return correccionAplicada;
	}

	public void setCorreccionAplicada(String correccionAplicada) {
		this.correccionAplicada = correccionAplicada;
	}

	public String getMunicipioOriginal() {
		return municipioOriginal;
	}

	public void setMunicipioOriginal(String municipioOriginal) {
		this.municipioOriginal = municipioOriginal;
	}

	public String getMunicipioCorregido() {
		return municipioCorregido;
	}

	public void setMunicipioCorregido(String municipioCorregido) {
		this.municipioCorregido = municipioCorregido;
	}

	public String getDireccionCorregida() {
		return direccionCorregida;
	}

	public void setDireccionCorregida(String direccionCorregida) {
		this.direccionCorregida = direccionCorregida;
	}

	public String getDireccionOriginalNormalizada() {
		return direccionOriginalNormalizada;
	}

	public void setDireccionOriginalNormalizada(String direccionOriginalNormalizada) {
		this.direccionOriginalNormalizada = direccionOriginalNormalizada;
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

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	private boolean success;
	
	

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getEstadoTienda() {
		return estadoTienda;
	}

	public void setEstadoTienda(String estadoTienda) {
		this.estadoTienda = estadoTienda;
	}

	public String getResultado() {
		return resultado;
	}
	
	public String getInfoAdicional() {
		return infoAdicional;
	}
	
	public void setInfoAdicional(String infoAdicional) {
		this.infoAdicional =infoAdicional;
	}


	public void setResultado(String resultado) {
		this.resultado = resultado;
	}
	

}