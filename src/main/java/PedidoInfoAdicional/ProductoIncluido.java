package PedidoInfoAdicional;

public class ProductoIncluido {
	
	private int idproductoincluido;
	private int idproductopadre;
	private int idproductohijo;
	private double cantidad;
	private String nombre;
	private double preciogeneral;
	
	
	
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public double getPreciogeneral() {
		return preciogeneral;
	}
	public void setPreciogeneral(double preciogeneral) {
		this.preciogeneral = preciogeneral;
	}
	public int getIdproductoincluido() {
		return idproductoincluido;
	}
	public void setIdproductoincluido(int idproductoincluido) {
		this.idproductoincluido = idproductoincluido;
	}
	public int getIdproductopadre() {
		return idproductopadre;
	}
	public void setIdproductopadre(int idproductopadre) {
		this.idproductopadre = idproductopadre;
	}
	public int getIdproductohijo() {
		return idproductohijo;
	}
	public void setIdproductohijo(int idproductohijo) {
		this.idproductohijo = idproductohijo;
	}
	public double getCantidad() {
		return cantidad;
	}
	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	public ProductoIncluido(int idproductoincluido, int idproductopadre, int idproductohijo, double cantidad,
			String nombre, double preciogeneral) {
		super();
		this.idproductoincluido = idproductoincluido;
		this.idproductopadre = idproductopadre;
		this.idproductohijo = idproductohijo;
		this.cantidad = cantidad;
		this.nombre = nombre;
		this.preciogeneral = preciogeneral;
	}
	
	
	

}
