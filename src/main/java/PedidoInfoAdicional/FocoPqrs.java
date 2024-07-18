package PedidoInfoAdicional;

public class FocoPqrs {
	
	private int idFoco;
	private String nombreFoco;
	public int getIdFoco() {
		return idFoco;
	}
	public void setIdFoco(int idFoco) {
		this.idFoco = idFoco;
	}
	public String getNombreFoco() {
		return nombreFoco;
	}
	public void setNombreFoco(String nombreFoco) {
		this.nombreFoco = nombreFoco;
	}
	public FocoPqrs(int idFoco, String nombreFoco) {
		super();
		this.idFoco = idFoco;
		this.nombreFoco = nombreFoco;
	}

	
	

}
