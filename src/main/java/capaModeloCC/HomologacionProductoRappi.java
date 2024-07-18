package capaModeloCC;

public class HomologacionProductoRappi {
	
	private String nombre;
	private int idProducto;
	private int idEspecialidad;
	private int idExcepcion;
	private double precio;
	
	
	public double getPrecio() {
		return precio;
	}
	public void setPrecio(double precio) {
		this.precio = precio;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public int getIdProducto() {
		return idProducto;
	}
	public void setIdProducto(int idProducto) {
		this.idProducto = idProducto;
	}
	public int getIdEspecialidad() {
		return idEspecialidad;
	}
	public void setIdEspecialidad(int idEspecialidad) {
		this.idEspecialidad = idEspecialidad;
	}
	public int getIdExcepcion() {
		return idExcepcion;
	}
	public void setIdExcepcion(int idExcepcion) {
		this.idExcepcion = idExcepcion;
	}
	public HomologacionProductoRappi(String nombre, int idProducto, int idEspecialidad, int idExcepcion,
			double precio) {
		super();
		this.nombre = nombre;
		this.idProducto = idProducto;
		this.idEspecialidad = idEspecialidad;
		this.idExcepcion = idExcepcion;
		this.precio = precio;
	}
}
