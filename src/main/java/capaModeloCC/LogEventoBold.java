package capaModeloCC;

/** Un evento que Bold notifico al webhook, con lo que se pudo extraer de su JSON. */
public class LogEventoBold {

	private String hashCuerpo;
	private String idNotificacion;
	private String tipoEvento;
	private String paymentId;
	private String merchantId;
	private String paymentMethod;
	private java.math.BigDecimal montoTotal;
	private String moneda;
	private String referencia;
	private String terminalId;
	private String fechaEvento;
	private boolean firmaValida;
	private String motivoFirma;
	private String sellerEmail;
	private String boldUserId;
	private long idLog;
	private int idTienda;
	private boolean entregadoTienda;
	private String ipOrigen;
	private String jsonEvento;

	public String getHashCuerpo() {
		return hashCuerpo;
	}

	public void setHashCuerpo(String hashCuerpo) {
		this.hashCuerpo = hashCuerpo;
	}

	public String getIdNotificacion() {
		return idNotificacion;
	}

	public void setIdNotificacion(String idNotificacion) {
		this.idNotificacion = idNotificacion;
	}

	public String getTipoEvento() {
		return tipoEvento;
	}

	public void setTipoEvento(String tipoEvento) {
		this.tipoEvento = tipoEvento;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public java.math.BigDecimal getMontoTotal() {
		return montoTotal;
	}

	public void setMontoTotal(java.math.BigDecimal montoTotal) {
		this.montoTotal = montoTotal;
	}

	public String getMoneda() {
		return moneda;
	}

	public void setMoneda(String moneda) {
		this.moneda = moneda;
	}

	public String getReferencia() {
		return referencia;
	}

	public void setReferencia(String referencia) {
		this.referencia = referencia;
	}

	public String getTerminalId() {
		return terminalId;
	}

	public void setTerminalId(String terminalId) {
		this.terminalId = terminalId;
	}

	public String getFechaEvento() {
		return fechaEvento;
	}

	public void setFechaEvento(String fechaEvento) {
		this.fechaEvento = fechaEvento;
	}

	public boolean isFirmaValida() {
		return firmaValida;
	}

	public void setFirmaValida(boolean firmaValida) {
		this.firmaValida = firmaValida;
	}

	public String getMotivoFirma() {
		return motivoFirma;
	}

	public void setMotivoFirma(String motivoFirma) {
		this.motivoFirma = motivoFirma;
	}

	public String getIpOrigen() {
		return ipOrigen;
	}

	public void setIpOrigen(String ipOrigen) {
		this.ipOrigen = ipOrigen;
	}

	public String getSellerEmail() {
		return sellerEmail;
	}

	public void setSellerEmail(String sellerEmail) {
		this.sellerEmail = sellerEmail;
	}

	public String getBoldUserId() {
		return boldUserId;
	}

	public void setBoldUserId(String boldUserId) {
		this.boldUserId = boldUserId;
	}

	public long getIdLog() {
		return idLog;
	}

	public void setIdLog(long idLog) {
		this.idLog = idLog;
	}

	public int getIdTienda() {
		return idTienda;
	}

	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}

	public boolean isEntregadoTienda() {
		return entregadoTienda;
	}

	public void setEntregadoTienda(boolean entregadoTienda) {
		this.entregadoTienda = entregadoTienda;
	}

	public String getJsonEvento() {
		return jsonEvento;
	}

	public void setJsonEvento(String jsonEvento) {
		this.jsonEvento = jsonEvento;
	}

}
