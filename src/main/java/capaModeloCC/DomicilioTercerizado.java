package capaModeloCC;

public class DomicilioTercerizado {
	
	private String domicilioTercerizado;
	private String empresaTercerizada;
	public String getDomicilioTercerizado() {
		return domicilioTercerizado;
	}
	public void setDomicilioTercerizado(String domicilioTercerizado) {
		this.domicilioTercerizado = domicilioTercerizado;
	}
	public String getEmpresaTercerizada() {
		return empresaTercerizada;
	}
	public void setEmpresaTercerizada(String empresaTercerizada) {
		this.empresaTercerizada = empresaTercerizada;
	}
	public DomicilioTercerizado(String domicilioTercerizado, String empresaTercerizada) {
		super();
		this.domicilioTercerizado = domicilioTercerizado;
		this.empresaTercerizada = empresaTercerizada;
	}
	public DomicilioTercerizado() {
		super();
	}
	
	
}
