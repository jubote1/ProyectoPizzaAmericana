package PedidoInfoAdicional;

/**
 * Clase qeu implementa la entidad SaborLiquido.
 * @author JuanDavid
 *
 */
public class SaborLiquido {
	
	private int idSaborTipoLiquido;
	private String descripcionSabor;
	private int idLiquido;
	private String descripcionLiquido;
	private int idProducto;
	private String nombreProducto;
	private int idExcepcion;
	private String descripcionExcepcion;
	
	
	public int getIdExcepcion() {
		return idExcepcion;
	}
	public void setIdExcepcion(int idExcepcion) {
		this.idExcepcion = idExcepcion;
	}
	public String getDescripcionExcepcion() {
		return descripcionExcepcion;
	}
	public void setDescripcionExcepcion(String descripcionExcepcion) {
		this.descripcionExcepcion = descripcionExcepcion;
	}
	public int getIdSaborTipoLiquido() {
		return idSaborTipoLiquido;
	}
	public void setIdSaborTipoLiquido(int idSaborTipoLiquido) {
		this.idSaborTipoLiquido = idSaborTipoLiquido;
	}
	public String getDescripcionSabor() {
		return descripcionSabor;
	}
	public void setDescripcionSabor(String descripcionSabor) {
		this.descripcionSabor = descripcionSabor;
	}
	public int getIdLiquido() {
		return idLiquido;
	}
	public void setIdLiquido(int idLiquido) {
		this.idLiquido = idLiquido;
	}
	public String getDescripcionLiquido() {
		return descripcionLiquido;
	}
	public void setDescripcionLiquido(String descripcionLiquido) {
		this.descripcionLiquido = descripcionLiquido;
	}
	public int getIdProducto() {
		return idProducto;
	}
	public void setIdProducto(int idProducto) {
		this.idProducto = idProducto;
	}
	public String getNombreProducto() {
		return nombreProducto;
	}
	public void setNombreProducto(String nombreProducto) {
		this.nombreProducto = nombreProducto;
	}
	public SaborLiquido(int idSaborTipoLiquido, String descripcionSabor, int idLiquido, String descripcionLiquido,
			int idProducto, String nombreProducto) {
		super();
		this.idSaborTipoLiquido = idSaborTipoLiquido;
		this.descripcionSabor = descripcionSabor;
		this.idLiquido = idLiquido;
		this.descripcionLiquido = descripcionLiquido;
		this.idProducto = idProducto;
		this.nombreProducto = nombreProducto;
	}
	public SaborLiquido(int idSaborTipoLiquido, String descripcionSabor, int idLiquido, String descripcionLiquido, String descripcionExcepcion, int idExcepcion ) {
		super();
		this.idSaborTipoLiquido = idSaborTipoLiquido;
		this.descripcionSabor = descripcionSabor;
		this.idLiquido = idLiquido;
		this.descripcionLiquido = descripcionLiquido;
		this.idExcepcion = idExcepcion;
		this.descripcionExcepcion = descripcionExcepcion;
	}
	public SaborLiquido(int idSaborTipoLiquido, String descripcionSabor, int idLiquido, String descripcionliquido) {
		super();
		this.idSaborTipoLiquido = idSaborTipoLiquido;
		this.descripcionSabor = descripcionSabor;
		this.idLiquido = idLiquido;
		this.descripcionLiquido = descripcionliquido;
	}
	
	public SaborLiquido(int idSaborTipoLiquido, String descripcionSabor, int idLiquido) {
		super();
		this.idSaborTipoLiquido = idSaborTipoLiquido;
		this.descripcionSabor = descripcionSabor;
		this.idLiquido = idLiquido;
	}
	
	
	
	
	
	
	
	

}
