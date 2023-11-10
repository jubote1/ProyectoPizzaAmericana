package PedidoInfoAdicional;
/**
 * Clase que implementa la entidad Tipo Liquido
 * @author JuanDavid
 *
 */
public class TipoLiquido {

	private int idtipo_liquido;
	private String nombre;
	private String capacidad;
	public int getIdtipo_liquido() {
		return idtipo_liquido;
	}
	public void setIdtipo_liquido(int idtipo_liquido) {
		this.idtipo_liquido = idtipo_liquido;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getCapacidad() {
		return capacidad;
	}
	public void setCapacidad(String capacidad) {
		this.capacidad = capacidad;
	}
	public TipoLiquido(String nombre, String capacidad) {
		super();
		this.nombre = nombre;
		this.capacidad = capacidad;
	}
	public TipoLiquido(int idtipo_liquido, String nombre, String capacidad) {
		super();
		this.idtipo_liquido = idtipo_liquido;
		this.nombre = nombre;
		this.capacidad = capacidad;
	}
	
	
}
