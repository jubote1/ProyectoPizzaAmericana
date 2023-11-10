package capaModeloCC;

public class EnvioPos {
	
	private int idproductoext;
	private double cantidad;
	
	public int getIdproductoext() {
		return idproductoext;
	}
	public void setIdproductoext(int idproductoext) {
		this.idproductoext = idproductoext;
	}
	public double getCantidad() {
		return cantidad;
	}
	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}
	public EnvioPos(int idproductoext, double cantidad) {
		super();
		this.idproductoext = idproductoext;
		this.cantidad = cantidad;
	}

	
	
	
}
