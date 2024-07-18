package capaModeloCC;

public class PedidoLinkWompi {
	
	private String tienda;
	private int idTienda;
	private String pedido;
	private String link;
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public String getPedido() {
		return pedido;
	}
	public void setPedido(String pedido) {
		this.pedido = pedido;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public PedidoLinkWompi(String tienda, int idTienda, String pedido, String link) {
		super();
		this.tienda = tienda;
		this.idTienda = idTienda;
		this.pedido = pedido;
		this.link = link;
	}
	

}
