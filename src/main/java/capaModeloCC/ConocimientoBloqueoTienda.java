package capaModeloCC;

import java.io.Serializable;

/**
 * Modelo de Conocimiento Semanal de Bloqueo de Tiendas.
 *
 * Captura la fotografia operativa en el instante exacto del bloqueo:
 * - Tiempos y duracion (bloqueo, desbloqueo, minutos y horas)
 * - Carga de cocina: cantidad de pedidos en proceso ("pixeles") y pizzas en cola
 * - Capacidad logistica: domiciliarios en turno activo
 * - Ratio de presion operativa (pedidos / domiciliario)
 * - Venta estimada perdida durante la ventana de bloqueo
 * - Patron predictivo detectado y sugerencia preventiva accionable
 *
 * Almacenado en la tabla `pizzaamericana.conocimiento_bloqueo_tienda` para
 * analisis historico y soporte a algoritmos predictivos de operacion.
 */
public class ConocimientoBloqueoTienda implements Serializable {

	private static final long serialVersionUID = 1L;

	private int idConocimiento;
	private int idLogBloqueo;
	private int idTienda;
	private String nombreTienda;
	private String periodoSemana;
	private String fechaBloqueo;
	private String fechaDesbloqueo;
	private long minutosBloqueo;
	private double horasBloqueo;
	private String motivo;
	private String observacion;
	private String debloqueoEn;

	// Analitica contextual
	private String diaSemana;
	private boolean esFinSemana;
	private String franjaHoraria;
	private int horaDelDia;

	// Metricas operativas al momento del bloqueo
	private int pedidosActivosPixel;
	private int pizzasEnProceso;
	private int domiciliariosActivos;
	private double ratioPedidosDomiciliario;
	private int pedidosDomicilioActivos;
	private int pedidosMostradorActivos;
	private double ventaHoraPromedio;
	private double ventaEstimadaPerdida;

	// Diagnostico y Predictivo
	private String patronDetectado;
	private String nivelCriticidad;
	private String sugerenciaPredictiva;
	private String fechaRegistro;

	public ConocimientoBloqueoTienda() {
		this.nombreTienda = "";
		this.periodoSemana = "";
		this.fechaBloqueo = "";
		this.fechaDesbloqueo = "";
		this.motivo = "";
		this.observacion = "";
		this.debloqueoEn = "";
		this.diaSemana = "";
		this.franjaHoraria = "";
		this.patronDetectado = "";
		this.nivelCriticidad = "MEDIA";
		this.sugerenciaPredictiva = "";
	}

	public int getIdConocimiento() {
		return idConocimiento;
	}

	public void setIdConocimiento(int idConocimiento) {
		this.idConocimiento = idConocimiento;
	}

	public int getIdLogBloqueo() {
		return idLogBloqueo;
	}

	public void setIdLogBloqueo(int idLogBloqueo) {
		this.idLogBloqueo = idLogBloqueo;
	}

	public int getIdTienda() {
		return idTienda;
	}

	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}

	public String getNombreTienda() {
		return nombreTienda;
	}

	public void setNombreTienda(String nombreTienda) {
		this.nombreTienda = nombreTienda != null ? nombreTienda : "";
	}

	public String getPeriodoSemana() {
		return periodoSemana;
	}

	public void setPeriodoSemana(String periodoSemana) {
		this.periodoSemana = periodoSemana != null ? periodoSemana : "";
	}

	public String getFechaBloqueo() {
		return fechaBloqueo;
	}

	public void setFechaBloqueo(String fechaBloqueo) {
		this.fechaBloqueo = fechaBloqueo != null ? fechaBloqueo : "";
	}

	public String getFechaDesbloqueo() {
		return fechaDesbloqueo;
	}

	public void setFechaDesbloqueo(String fechaDesbloqueo) {
		this.fechaDesbloqueo = fechaDesbloqueo != null ? fechaDesbloqueo : "";
	}

	public long getMinutosBloqueo() {
		return minutosBloqueo;
	}

	public void setMinutosBloqueo(long minutosBloqueo) {
		this.minutosBloqueo = minutosBloqueo;
		this.horasBloqueo = ((double) minutosBloqueo) / 60.0;
	}

	public double getHorasBloqueo() {
		return horasBloqueo;
	}

	public void setHorasBloqueo(double horasBloqueo) {
		this.horasBloqueo = horasBloqueo;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo != null ? motivo : "";
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion != null ? observacion : "";
	}

	public String getDebloqueoEn() {
		return debloqueoEn;
	}

	public void setDebloqueoEn(String debloqueoEn) {
		this.debloqueoEn = debloqueoEn != null ? debloqueoEn : "";
	}

	public String getDiaSemana() {
		return diaSemana;
	}

	public void setDiaSemana(String diaSemana) {
		this.diaSemana = diaSemana != null ? diaSemana : "";
	}

	public boolean isEsFinSemana() {
		return esFinSemana;
	}

	public void setEsFinSemana(boolean esFinSemana) {
		this.esFinSemana = esFinSemana;
	}

	public String getFranjaHoraria() {
		return franjaHoraria;
	}

	public void setFranjaHoraria(String franjaHoraria) {
		this.franjaHoraria = franjaHoraria != null ? franjaHoraria : "";
	}

	public int getHoraDelDia() {
		return horaDelDia;
	}

	public void setHoraDelDia(int horaDelDia) {
		this.horaDelDia = horaDelDia;
	}

	public int getPedidosActivosPixel() {
		return pedidosActivosPixel;
	}

	public void setPedidosActivosPixel(int pedidosActivosPixel) {
		this.pedidosActivosPixel = pedidosActivosPixel;
	}

	public int getPizzasEnProceso() {
		return pizzasEnProceso;
	}

	public void setPizzasEnProceso(int pizzasEnProceso) {
		this.pizzasEnProceso = pizzasEnProceso;
	}

	public int getDomiciliariosActivos() {
		return domiciliariosActivos;
	}

	public void setDomiciliariosActivos(int domiciliariosActivos) {
		this.domiciliariosActivos = domiciliariosActivos;
	}

	public double getRatioPedidosDomiciliario() {
		return ratioPedidosDomiciliario;
	}

	public void setRatioPedidosDomiciliario(double ratioPedidosDomiciliario) {
		this.ratioPedidosDomiciliario = ratioPedidosDomiciliario;
	}

	public int getPedidosDomicilioActivos() {
		return pedidosDomicilioActivos;
	}

	public void setPedidosDomicilioActivos(int pedidosDomicilioActivos) {
		this.pedidosDomicilioActivos = pedidosDomicilioActivos;
	}

	public int getPedidosMostradorActivos() {
		return pedidosMostradorActivos;
	}

	public void setPedidosMostradorActivos(int pedidosMostradorActivos) {
		this.pedidosMostradorActivos = pedidosMostradorActivos;
	}

	public double getVentaHoraPromedio() {
		return ventaHoraPromedio;
	}

	public void setVentaHoraPromedio(double ventaHoraPromedio) {
		this.ventaHoraPromedio = ventaHoraPromedio;
	}

	public double getVentaEstimadaPerdida() {
		return ventaEstimadaPerdida;
	}

	public void setVentaEstimadaPerdida(double ventaEstimadaPerdida) {
		this.ventaEstimadaPerdida = ventaEstimadaPerdida;
	}

	public String getPatronDetectado() {
		return patronDetectado;
	}

	public void setPatronDetectado(String patronDetectado) {
		this.patronDetectado = patronDetectado != null ? patronDetectado : "";
	}

	public String getNivelCriticidad() {
		return nivelCriticidad;
	}

	public void setNivelCriticidad(String nivelCriticidad) {
		this.nivelCriticidad = nivelCriticidad != null ? nivelCriticidad : "MEDIA";
	}

	public String getSugerenciaPredictiva() {
		return sugerenciaPredictiva;
	}

	public void setSugerenciaPredictiva(String sugerenciaPredictiva) {
		this.sugerenciaPredictiva = sugerenciaPredictiva != null ? sugerenciaPredictiva : "";
	}

	public String getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(String fechaRegistro) {
		this.fechaRegistro = fechaRegistro != null ? fechaRegistro : "";
	}
}
