package capaModeloCC;

public class EncuestaLaboralDetalle {
	
	private int idEncuestaDetalle;
	private int idEncuesta;
	private String descripcion;
	private String tipoRespuesta;
	private double valorInicial;
	private double valorFinal;
	private double valorEscala;
	private double valorDefecto;
	private String alertar;
	private String valorAlertar;
	private String obligatorio;
	
	
	
	public String getObligatorio() {
		return obligatorio;
	}
	public void setObligatorio(String obligatorio) {
		this.obligatorio = obligatorio;
	}
	public String getAlertar() {
		return alertar;
	}
	public void setAlertar(String alertar) {
		this.alertar = alertar;
	}
	public String getValorAlertar() {
		return valorAlertar;
	}
	public void setValorAlertar(String valorAlertar) {
		this.valorAlertar = valorAlertar;
	}
	public double getValorDefecto() {
		return valorDefecto;
	}
	public void setValorDefecto(double valorDefecto) {
		this.valorDefecto = valorDefecto;
	}
	public double getValorInicial() {
		return valorInicial;
	}
	public void setValorInicial(double valorInicial) {
		this.valorInicial = valorInicial;
	}
	public double getValorFinal() {
		return valorFinal;
	}
	public void setValorFinal(double valorFinal) {
		this.valorFinal = valorFinal;
	}
	public double getValorEscala() {
		return valorEscala;
	}
	public void setValorEscala(double valorEscala) {
		this.valorEscala = valorEscala;
	}
	public int getIdEncuestaDetalle() {
		return idEncuestaDetalle;
	}
	public void setIdEncuestaDetalle(int idEncuestaDetalle) {
		this.idEncuestaDetalle = idEncuestaDetalle;
	}
	public int getIdEncuesta() {
		return idEncuesta;
	}
	public void setIdEncuesta(int idEncuesta) {
		this.idEncuesta = idEncuesta;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public String getTipoRespuesta() {
		return tipoRespuesta;
	}
	public void setTipoRespuesta(String tipoRespuesta) {
		this.tipoRespuesta = tipoRespuesta;
	}
	public EncuestaLaboralDetalle(int idEncuestaDetalle, int idEncuesta, String descripcion, String tipoRespuesta,
			double valorInicial, double valorFinal, double valorEscala, double valorDefecto, String alertar, String valorAlertar) {
		super();
		this.idEncuestaDetalle = idEncuestaDetalle;
		this.idEncuesta = idEncuesta;
		this.descripcion = descripcion;
		this.tipoRespuesta = tipoRespuesta;
		this.valorInicial = valorInicial;
		this.valorFinal = valorFinal;
		this.valorEscala = valorEscala;
		this.valorDefecto = valorDefecto;
		this.alertar = alertar;
		this.valorAlertar = valorAlertar;
	}
	
	
	
}
