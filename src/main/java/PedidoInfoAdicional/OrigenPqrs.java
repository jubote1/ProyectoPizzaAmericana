package PedidoInfoAdicional;

public class OrigenPqrs {
	
	private int idOrigen;
	private String nombreOrigen;
	
	public int getIdOrigen() {
		return idOrigen;
	}
	public void setIdOrigen(int idOrigen) {
		this.idOrigen = idOrigen;
	}
	public String getNombreOrigen() {
		return nombreOrigen;
	}
	public void setNombreOrigen(String nombreOrigen) {
		this.nombreOrigen = nombreOrigen;
	}
	public OrigenPqrs(int idOrigen, String nombreOrigen) {
		super();
		this.idOrigen = idOrigen;
		this.nombreOrigen = nombreOrigen;
	}
	
	
	

}
