package capaModeloCC;

public class CodigoRedencionPuntos {
	
private int idCodigo;
private String correo;
private String codigo;
private String fecha;
private double puntos;
private String validado;
private String fechaSistema;
public int getIdCodigo() {
	return idCodigo;
}
public void setIdCodigo(int idCodigo) {
	this.idCodigo = idCodigo;
}
public String getCorreo() {
	return correo;
}
public void setCorreo(String correo) {
	this.correo = correo;
}
public String getCodigo() {
	return codigo;
}
public void setCodigo(String codigo) {
	this.codigo = codigo;
}
public String getFecha() {
	return fecha;
}
public void setFecha(String fecha) {
	this.fecha = fecha;
}
public double getPuntos() {
	return puntos;
}
public void setPuntos(double puntos) {
	this.puntos = puntos;
}
public String getValidado() {
	return validado;
}
public void setValidado(String validado) {
	this.validado = validado;
}
public String getFechaSistema() {
	return fechaSistema;
}
public void setFechaSistema(String fechaSistema) {
	this.fechaSistema = fechaSistema;
}
public CodigoRedencionPuntos(int idCodigo, String correo, String codigo, String fecha, double puntos, String validado,
		String fechaSistema) {
	super();
	this.idCodigo = idCodigo;
	this.correo = correo;
	this.codigo = codigo;
	this.fecha = fecha;
	this.puntos = puntos;
	this.validado = validado;
	this.fechaSistema = fechaSistema;
}
public CodigoRedencionPuntos() {
	super();
}



}
