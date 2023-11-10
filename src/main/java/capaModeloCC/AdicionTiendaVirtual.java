package capaModeloCC;

public class AdicionTiendaVirtual {
	
	private int idProductoAdicion;
	private double cantidad;
	private int idDetallePedido;
	private int posicionPizza;

	public int getIdProductoAdicion() {
		return idProductoAdicion;
	}
	public void setIdProductoAdicion(int idProductoAdicion) {
		this.idProductoAdicion = idProductoAdicion;
	}
	public double getCantidad() {
		return cantidad;
	}
	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	
	public int getIdDetallePedido() {
		return idDetallePedido;
	}
	public void setIdDetallePedido(int idDetallePedido) {
		this.idDetallePedido = idDetallePedido;
	}
	
	public int getPosicionPizza() {
		return posicionPizza;
	}
	public void setPosicionPizza(int posicionPizza) {
		this.posicionPizza = posicionPizza;
	}
	public AdicionTiendaVirtual()
	{
		
	}
	

}
