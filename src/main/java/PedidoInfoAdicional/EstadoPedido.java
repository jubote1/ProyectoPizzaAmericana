package PedidoInfoAdicional;

/**
 * Clase que implementa la entidad EstadoPedido
 * @author JuanDavid
 *
 */
public class EstadoPedido {
	
	private int idestadopedido;
	private String descripcion;
	
	
	public int getIdestadopedido() {
		return idestadopedido;
	}
	public void setIdestadopedido(int idestadopedido) {
		this.idestadopedido = idestadopedido;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public EstadoPedido(int idestadopedido, String descripcion) {
		super();
		this.idestadopedido = idestadopedido;
		this.descripcion = descripcion;
	}
	public EstadoPedido(String descripcion) {
		super();
		this.descripcion = descripcion;
	}
	
	

}
