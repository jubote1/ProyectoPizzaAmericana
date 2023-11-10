package capaModeloCC;

public class TipoPedido {

	private int idTipoPedido;
	private String nombre;
	private String tipoPago;
	private String validaDir;
	
	
	public String getValidaDir() {
		return validaDir;
	}
	public void setValidaDir(String validaDir) {
		this.validaDir = validaDir;
	}
	public int getIdTipoPedido() {
		return idTipoPedido;
	}
	public void setIdTipoPedido(int idTipoPedido) {
		this.idTipoPedido = idTipoPedido;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getTipoPago() {
		return tipoPago;
	}
	public void setTipoPago(String tipoPago) {
		this.tipoPago = tipoPago;
	}
	public TipoPedido(int idTipoPedido, String nombre, String tipoPago, String validaDir) {
		super();
		this.idTipoPedido = idTipoPedido;
		this.nombre = nombre;
		this.tipoPago = tipoPago;
		this.validaDir = validaDir;
	}
	
	
	
}
