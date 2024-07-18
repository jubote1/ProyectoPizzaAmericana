package capaModeloCC;

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
private String controlaHora;
private String horaInicio;
private String horaFin;
private String tipoOferta;
private String fechaDesde;
private String fechaHasta;
private String codigoGeneral;
private String redParcial;
private String reintegro;

public String getTipoOferta() {
	return tipoOferta;
}
public void setTipoOferta(String tipoOferta) {
	this.tipoOferta = tipoOferta;
}
public String getFechaDesde() {
	return fechaDesde;
}
public void setFechaDesde(String fechaDesde) {
	this.fechaDesde = fechaDesde;
}
public String getFechaHasta() {
	return fechaHasta;
}
public void setFechaHasta(String fechaHasta) {
	this.fechaHasta = fechaHasta;
}
public String getCodigoGeneral() {
	return codigoGeneral;
}
public void setCodigoGeneral(String codigoGeneral) {
	this.codigoGeneral = codigoGeneral;
}
public String getControlaHora() {
	return controlaHora;
}
public void setControlaHora(String controlaHora) {
	this.controlaHora = controlaHora;
}
public String getHoraInicio() {
	return horaInicio;
}
public void setHoraInicio(String horaInicio) {
	this.horaInicio = horaInicio;
}
public String getHoraFin() {
	return horaFin;
}
public void setHoraFin(String horaFin) {
	this.horaFin = horaFin;
}
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

public String getRedParcial() {
	return redParcial;
}
public void setRedParcial(String redParcial) {
	this.redParcial = redParcial;
}


public String getReintegro() {
	return reintegro;
}
public void setReintegro(String reintegro) {
	this.reintegro = reintegro;
}

public Oferta(int idOferta, String controlaHora, String horaInicio, String horaFin) {
	super();
	this.idOferta = idOferta;
	this.controlaHora = controlaHora;
	this.horaInicio = horaInicio;
	this.horaFin = horaFin;
}



	
}
