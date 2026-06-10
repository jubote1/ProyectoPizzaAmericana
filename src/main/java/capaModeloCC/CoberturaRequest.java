package capaModeloCC;

import com.google.gson.annotations.SerializedName;

public class CoberturaRequest {
    private String direccion;
    private String municipio;

	private String barrio;

    @SerializedName("tipo_cliente")
    private String tipoCliente;

    private String lead;
    private Double latitud;
    private Double longitud;
    private Integer idTienda;
    private Integer idcliente;
    private String  telefono;
    
    public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	private String direccionProveedor;

    
    public Integer getIdcliente() {
		return idcliente;
	}

	public void setIdcliente(Integer idcliente) {
		this.idcliente = idcliente;
	}

    public String getDireccionProveedor() {
        return direccionProveedor == null ? "" : direccionProveedor.trim();
    }

    public void setDireccionProveedor(String direccionProveedor) {
        this.direccionProveedor = direccionProveedor;
    }

    public boolean tieneDireccionProveedor() {
        return direccionProveedor != null && !direccionProveedor.trim().isEmpty();
    }
    
    public String getDireccion() {
        return direccion == null ? "" : direccion.trim();
    }

    public String getMunicipio() {
        return municipio == null ? "" : municipio.trim();
    }

    public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public void setMunicipio(String municipio) {
		this.municipio = municipio;
	}

	public void setBarrio(String barrio) {
		this.barrio = barrio;
	}

	public void setTipoCliente(String tipoCliente) {
		this.tipoCliente = tipoCliente;
	}

	public void setLead(String lead) {
		this.lead = lead;
	}

	public void setLatitud(Double latitud) {
		this.latitud = latitud;
	}

	public void setLongitud(Double longitud) {
		this.longitud = longitud;
	}

	public void setIdTienda(Integer idTienda) {
		this.idTienda = idTienda;
	}

	public String getBarrio() {
        return barrio == null ? "" : barrio.trim();
    }

    public String getTipoCliente() {
        return tipoCliente == null ? null : tipoCliente.trim();
    }

    public String getLead() {
        return lead == null ? null : lead.trim();
    }
    
    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public Integer getIdTienda() {
        return idTienda;
    }

    public boolean tieneCoordenadasValidas() {
        return latitud != null
                && longitud != null
                && latitud != 0
                && longitud != 0
                && latitud >= -90
                && latitud <= 90
                && longitud >= -180
                && longitud <= 180;
    }

    public boolean tieneIdTiendaValido() {
        return idTienda != null && idTienda > 0;
    }
}