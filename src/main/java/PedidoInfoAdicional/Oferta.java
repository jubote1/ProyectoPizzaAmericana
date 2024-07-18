package PedidoInfoAdicional;

public class Oferta {
private int idOferta;
private String nombreOferta;
private int idExcepcion;
private String nombreExcepcion;
private String codigoPromocional;
private double descuentoFijoPorcentaje;
private double descuentoFijoValor;
private String mensaje1;
private String mensaje2;
private int diasCaducidad;
private String tipoCaducidad;



public double getDescuentoFijoPorcentaje() {
	return descuentoFijoPorcentaje;
}
public void setDescuentoFijoPorcentaje(double descuentoFijoPorcentaje) {
	this.descuentoFijoPorcentaje = descuentoFijoPorcentaje;
}
public double getDescuentoFijoValor() {
	return descuentoFijoValor;
}
public void setDescuentoFijoValor(double descuentoFijoValor) {
	this.descuentoFijoValor = descuentoFijoValor;
}
public int getDiasCaducidad() {
	return diasCaducidad;
}
public void setDiasCaducidad(int diasCaducidad) {
	this.diasCaducidad = diasCaducidad;
}
public String getTipoCaducidad() {
	return tipoCaducidad;
}
public void setTipoCaducidad(String tipoCaducidad) {
	this.tipoCaducidad = tipoCaducidad;
}
public String getCodigoPromocional() {
	return codigoPromocional;
}
public void setCodigoPromocional(String codigoPromocional) {
	this.codigoPromocional = codigoPromocional;
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
public String getNombreExcepcion() {
	return nombreExcepcion;
}
public void setNombreExcepcion(String nombreExcepcion) {
	this.nombreExcepcion = nombreExcepcion;
}
public int getIdOferta() {
	return idOferta;
}
public void setIdOferta(int idOferta) {
	this.idOferta = idOferta;
}
public String getNombreOferta() {
	return nombreOferta;
}
public void setNombreOferta(String nombreOferta) {
	this.nombreOferta = nombreOferta;
}
public int getIdExcepcion() {
	return idExcepcion;
}
public void setIdExcepcion(int idExcepcion) {
	this.idExcepcion = idExcepcion;
}
public Oferta(int idOferta, String nombreOferta, int idExcepcion) {
	super();
	this.idOferta = idOferta;
	this.nombreOferta = nombreOferta;
	this.idExcepcion = idExcepcion;
}



	
}
