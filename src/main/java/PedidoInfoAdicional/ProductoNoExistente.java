package PedidoInfoAdicional;

public class ProductoNoExistente {
	
	private int idtienda;
	private String tienda;
	private int idproducto;
	private String producto;
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public int getIdproducto() {
		return idproducto;
	}
	public void setIdproducto(int idproducto) {
		this.idproducto = idproducto;
	}
	public String getProducto() {
		return producto;
	}
	public void setProducto(String producto) {
		this.producto = producto;
	}
	public ProductoNoExistente(int idtienda, String tienda, int idproducto, String producto) {
		super();
		this.idtienda = idtienda;
		this.tienda = tienda;
		this.idproducto = idproducto;
		this.producto = producto;
	}
	
	
	

}
