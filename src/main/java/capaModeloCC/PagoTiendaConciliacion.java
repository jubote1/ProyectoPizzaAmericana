package capaModeloCC;

/**
 * Un pago QR o Datafono encontrado en vivo en la base local de una tienda,
 * para que el usuario compare a ojo contra la solicitud de conciliacion que
 * esta resolviendo (ver capaDAOCC.ConciliacionTiendaDAO).
 */
public class PagoTiendaConciliacion {

	private int idPedidoTienda;
	private String fecha;
	private String nombreCliente;
	private String telefono;
	private double valorFormaPago;
	private double totalNeto;
	private String referenciaDatafono;
	private boolean anulado;
	private String estacion;

	public PagoTiendaConciliacion() {
	}

	public int getIdPedidoTienda() {
		return idPedidoTienda;
	}

	public void setIdPedidoTienda(int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}

	public String getFecha() {
		return fecha;
	}

	public void setFecha(String fecha) {
		this.fecha = fecha;
	}

	public String getNombreCliente() {
		return nombreCliente;
	}

	public void setNombreCliente(String nombreCliente) {
		this.nombreCliente = nombreCliente;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public double getValorFormaPago() {
		return valorFormaPago;
	}

	public void setValorFormaPago(double valorFormaPago) {
		this.valorFormaPago = valorFormaPago;
	}

	public double getTotalNeto() {
		return totalNeto;
	}

	public void setTotalNeto(double totalNeto) {
		this.totalNeto = totalNeto;
	}

	public String getReferenciaDatafono() {
		return referenciaDatafono;
	}

	public void setReferenciaDatafono(String referenciaDatafono) {
		this.referenciaDatafono = referenciaDatafono;
	}

	public boolean isAnulado() {
		return anulado;
	}

	public void setAnulado(boolean anulado) {
		this.anulado = anulado;
	}

	public String getEstacion() {
		return estacion;
	}

	public void setEstacion(String estacion) {
		this.estacion = estacion;
	}

}
