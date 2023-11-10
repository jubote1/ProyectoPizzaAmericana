package capaModeloCC;

public class EmpleadoEncuesta {
	
	private int idEmpleadoEncuesta;
	private int id;
	private int idEvaluar;
	private int idEncuesta;
	private String fechaIngreso;
	private int idTienda;
	
	

	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public int getIdEvaluar() {
		return idEvaluar;
	}
	public void setIdEvaluar(int idEvaluar) {
		this.idEvaluar = idEvaluar;
	}
	public int getIdEmpleadoEncuesta() {
		return idEmpleadoEncuesta;
	}
	public void setIdEmpleadoEncuesta(int idEmpleadoEncuesta) {
		this.idEmpleadoEncuesta = idEmpleadoEncuesta;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getIdEncuesta() {
		return idEncuesta;
	}
	public void setIdEncuesta(int idEncuesta) {
		this.idEncuesta = idEncuesta;
	}
	public String getFechaIngreso() {
		return fechaIngreso;
	}
	public void setFechaIngreso(String fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	public EmpleadoEncuesta(int idEmpleadoEncuesta, int id, int idEncuesta, String fechaIngreso) {
		super();
		this.idEmpleadoEncuesta = idEmpleadoEncuesta;
		this.id = id;
		this.idEvaluar = 0;
		this.idEncuesta = idEncuesta;
		this.fechaIngreso = fechaIngreso;
	}
	
	public EmpleadoEncuesta(int idEmpleadoEncuesta, int id, int idEvaluar, int idEncuesta, String fechaIngreso, int idTienda) {
		super();
		this.idEmpleadoEncuesta = idEmpleadoEncuesta;
		this.id = id;
		this.idEvaluar = idEvaluar;
		this.idEncuesta = idEncuesta;
		this.fechaIngreso = fechaIngreso;
		this.idTienda = idTienda;
	}
	
	

}
