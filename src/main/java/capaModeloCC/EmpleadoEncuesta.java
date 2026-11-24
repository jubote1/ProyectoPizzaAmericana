package capaModeloCC;

public class EmpleadoEncuesta {
	
	private int idEmpleadoEncuesta;
	private int id;
	private int idEvaluar;
	private int idEncuesta;
	private String fechaIngreso;
	private int idTienda;
	private String nombreRecibe;
	private String cargoRecibe;
	private String medidasCorrectivas;
	private String firmaRecibe;
	private String firmaInspector;

	public String getCargoRecibe() {
		return cargoRecibe;
	}
	public void setCargoRecibe(String cargoRecibe) {
		this.cargoRecibe = cargoRecibe;
	}

	public String getFirmaInspector() {
		return firmaInspector;
	}
	public void setFirmaInspector(String firmaInspector) {
		this.firmaInspector = firmaInspector;
	}

	public String getNombreRecibe() {
		return nombreRecibe;
	}
	public void setNombreRecibe(String nombreRecibe) {
		this.nombreRecibe = nombreRecibe;
	}
	public String getMedidasCorrectivas() {
		return medidasCorrectivas;
	}
	public void setMedidasCorrectivas(String medidasCorrectivas) {
		this.medidasCorrectivas = medidasCorrectivas;
	}
	public String getFirmaRecibe() {
		return firmaRecibe;
	}
	public void setFirmaRecibe(String firmaRecibe) {
		this.firmaRecibe = firmaRecibe;
	}
	
	

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
