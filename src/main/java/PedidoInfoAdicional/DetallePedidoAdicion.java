package PedidoInfoAdicional;

/**
 * Clase que implementa la entidad Detalle Pedido Adición.
 * @author JuanDavid
 *
 */
public class DetallePedidoAdicion {
	
	private int idadicion;
	private int iddetallepedidopadre;
	private int iddetallepedidoadicion;
	private int idespecialidad1;
	private int idespecialidad2;
	private double cantidad1;
	private double cantidad2;
	private int idproducto;
	
	
	
	public int getIdproducto() {
		return idproducto;
	}
	public void setIdproducto(int idproducto) {
		this.idproducto = idproducto;
	}
	public int getIdadicion() {
		return idadicion;
	}
	public void setIdadicion(int idadicion) {
		this.idadicion = idadicion;
	}
	public int getIddetallepedidopadre() {
		return iddetallepedidopadre;
	}
	public void setIddetallepedidopadre(int iddetallepedidopadre) {
		this.iddetallepedidopadre = iddetallepedidopadre;
	}
	public int getIddetallepedidoadicion() {
		return iddetallepedidoadicion;
	}
	public void setIddetallepedidoadicion(int iddetallepedidoadicion) {
		this.iddetallepedidoadicion = iddetallepedidoadicion;
	}
	public int getIdespecialidad1() {
		return idespecialidad1;
	}
	public void setIdespecialidad1(int idespecialidad1) {
		this.idespecialidad1 = idespecialidad1;
	}
	public int getIdespecialidad2() {
		return idespecialidad2;
	}
	public void setIdespecialidad2(int idespecialidad2) {
		this.idespecialidad2 = idespecialidad2;
	}
	public double getCantidad1() {
		return cantidad1;
	}
	public void setCantidad1(double cantidad1) {
		this.cantidad1 = cantidad1;
	}
	public double getCantidad2() {
		return cantidad2;
	}
	public void setCantidad2(double cantidad2) {
		this.cantidad2 = cantidad2;
	}
	public DetallePedidoAdicion(int iddetallepedidopadre, int iddetallepedidoadicion, int idespecialidad1,
			int idespecialidad2, double cantidad1, double cantidad2) {
		super();
		this.iddetallepedidopadre = iddetallepedidopadre;
		this.iddetallepedidoadicion = iddetallepedidoadicion;
		this.idespecialidad1 = idespecialidad1;
		this.idespecialidad2 = idespecialidad2;
		this.cantidad1 = cantidad1;
		this.cantidad2 = cantidad2;
	}
	public DetallePedidoAdicion(int idadicion, int iddetallepedidopadre, int iddetallepedidoadicion,
			int idespecialidad1, int idespecialidad2, double cantidad1, double cantidad2) {
		super();
		this.idadicion = idadicion;
		this.iddetallepedidopadre = iddetallepedidopadre;
		this.iddetallepedidoadicion = iddetallepedidoadicion;
		this.idespecialidad1 = idespecialidad1;
		this.idespecialidad2 = idespecialidad2;
		this.cantidad1 = cantidad1;
		this.cantidad2 = cantidad2;
	}
	
	
	
	

}
