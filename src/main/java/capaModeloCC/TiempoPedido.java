package capaModeloCC;

public class TiempoPedido {
	
	private int idtienda;
	private String Tienda;
	private int minutosPedido;
	private String estado;
	
	
	
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public String getTienda() {
		return Tienda;
	}
	public void setTienda(String tienda) {
		Tienda = tienda;
	}
	public int getMinutosPedido() {
		return minutosPedido;
	}
	public void setMinutosPedido(int minutosPedido) {
		this.minutosPedido = minutosPedido;
	}
	public TiempoPedido(int idtienda, String tienda, int minutosPedido, String estado) {
		super();
		this.idtienda = idtienda;
		Tienda = tienda;
		this.minutosPedido = minutosPedido;
		this.estado = estado;
	}
	
	
	

}
