package capaModeloCC;

import java.util.Date;

public class ClienteClub {
	private int idcliente;
	private String nombre;
	private String telefono;
	private String nombreTienda;
	private int puntos;
	private Date fechaVinculado;
	private String correo;
	private String activo;
	private String nombrecompania;

	
	public int getIdcliente() {
		return idcliente;
	}

	public void setIdcliente(int idcliente) {
		this.idcliente = idcliente;
	}
	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getNombreTienda() {
		return nombreTienda;
	}

	public void setNombreTienda(String nombreTienda) {
		this.nombreTienda = nombreTienda;
	}

	public int getPuntos() {
		return puntos;
	}

	public void setPuntos(int puntos) {
		this.puntos = puntos;
	}

	public Date getFechaVinculado() {
		return fechaVinculado;
	}

	public void setFechaVinculado(Date fechaVinculado) {
		this.fechaVinculado = fechaVinculado;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getActivo() {
		return activo;
	}

	public void setActivo(String activo) {
		this.activo = activo;
	}

	public String getNombrecompania() {
		return nombrecompania;
	}

	public void setNombrecompania(String nombrecompania) {
		this.nombrecompania = nombrecompania;
	}

	public ClienteClub() {

	}
}
