package capaModeloCC;

/**
 * Clase que implementa la entidad DetallePedidoPixel la cual tiene los parámetros para detallar al sistema Pos el pedido
 * @author JuanDavid
 *
 */
public class DetallePedidoPixel {
	
	private int idproductoext;
	private double cantidad;
	private boolean esMaster;
	private int idMaster;
	private int idModificador;
	private int idDetallePedido;
	private int idDetallePedIns;
	private double valor;
	
	
	public double getValor() {
		return valor;
	}
	public void setValor(double valor) {
		this.valor = valor;
	}
	public int getIdDetallePedIns() {
		return idDetallePedIns;
	}
	public void setIdDetallePedIns(int idDetallePedIns) {
		this.idDetallePedIns = idDetallePedIns;
	}
	public int getIdDetallePedido() {
		return idDetallePedido;
	}
	public void setIdDetallePedido(int idDetallePedido) {
		this.idDetallePedido = idDetallePedido;
	}
	public boolean isEsMaster() {
		return esMaster;
	}
	public void setEsMaster(boolean esMaster) {
		this.esMaster = esMaster;
	}
	public int getIdMaster() {
		return idMaster;
	}
	public void setIdMaster(int idMaster) {
		this.idMaster = idMaster;
	}
	public int getIdModificador() {
		return idModificador;
	}
	public void setIdModificador(int idModificador) {
		this.idModificador = idModificador;
	}
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
	public DetallePedidoPixel(int idproductoext, double cantidad) {
		super();
		this.idproductoext = idproductoext;
		this.cantidad = cantidad;
	}
	public DetallePedidoPixel(int idproductoext, double cantidad, boolean esMaster, int idMaster, int idModificador,
			int idDetallePedido) {
		super();
		this.idproductoext = idproductoext;
		this.cantidad = cantidad;
		this.esMaster = esMaster;
		this.idMaster = idMaster;
		this.idModificador = idModificador;
		this.idDetallePedido = idDetallePedido;
	}
	
	public DetallePedidoPixel(int idproductoext, double cantidad, boolean esMaster, int idMaster, int idModificador,
			int idDetallePedido, double valor) {
		super();
		this.idproductoext = idproductoext;
		this.cantidad = cantidad;
		this.esMaster = esMaster;
		this.idMaster = idMaster;
		this.idModificador = idModificador;
		this.idDetallePedido = idDetallePedido;
		this.valor = valor;
	}

	
	
	
	
}
