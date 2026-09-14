package capaModeloCC;

/**
 * Un renglon de la historia de un pago virtual.
 *
 * La historia de un pedido que no se pago estaba repartida en tres tablas que
 * nadie leia: pedido_pago_virtual -que le mandamos al cliente y cuando-,
 * log_evento_wompi -que contesto la pasarela- y pedido_gestion_link -que hizo
 * la persona que lo gestiono-. Por separado ninguna explica nada; juntas y en
 * orden cuentan exactamente por que se perdio la venta.
 *
 * Esta clase es el renglon comun para poder mezclarlas en una sola linea de
 * tiempo.
 */
public class TrazaPagoVirtual {

	/** AVISO, WOMPI o GESTION. Dice de donde salio el renglon. */
	private String fuente = "";

	/** Cuando paso. Formato de la base: yyyy-MM-dd HH:mm:ss. */
	private String fechaHora = "";

	/** Que paso, en palabras. */
	private String detalle = "";

	/** El estado que reporto Wompi, cuando el renglon viene de la pasarela. */
	private String estado = "";

	public TrazaPagoVirtual(final String fuente, final String fechaHora, final String detalle, final String estado) {
		super();
		this.fuente = (fuente == null ? "" : fuente);
		this.fechaHora = (fechaHora == null ? "" : fechaHora);
		this.detalle = (detalle == null ? "" : detalle);
		this.estado = (estado == null ? "" : estado);
	}

	public String getFuente() {
		return fuente;
	}

	public void setFuente(String fuente) {
		this.fuente = fuente;
	}

	public String getFechaHora() {
		return fechaHora;
	}

	public void setFechaHora(String fechaHora) {
		this.fechaHora = fechaHora;
	}

	public String getDetalle() {
		return detalle;
	}

	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}
}
