package capaModeloCC;

public class HorarioEmpleado {
	private int id;
	private String nombre;
	private String tipoEvento;
	private String fechaHora;
	private String tienda;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getTipoEvento() {
		return tipoEvento;
	}
	public void setTipoEvento(String tipoEvento) {
		this.tipoEvento = tipoEvento;
	}
	public String getFechaHora() {
		return fechaHora;
	}
	public void setFechaHora(String fechaHora) {
		this.fechaHora = fechaHora;
	}
	
	
	public String getTienda() {
		return tienda;
	}
	public void setTienda(String tienda) {
		this.tienda = tienda;
	}
	public HorarioEmpleado(int id, String nombre, String tipoEvento, String fechaHora, String tienda) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.tipoEvento = tipoEvento;
		this.fechaHora = fechaHora;
		this.tienda = tienda;
	}
}
