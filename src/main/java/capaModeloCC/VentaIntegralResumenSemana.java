package capaModeloCC;

/**
 * Cierre semanal (lunes a domingo) de una categoria de Venta Integral en una
 * tienda: lo que llena el proceso de Servicios cada lunes, y lo que consulta
 * la pantalla de Monitoreo en el central.
 */
public class VentaIntegralResumenSemana {

	private int idResumen;
	private int idTienda;
	private String nombreTienda;
	private int idCategoria;
	private String nombreCategoria;
	private String semanaInicio;
	private String semanaFin;
	private double cantidadTienda;
	private double cantidadContactCenter;
	private double cantidadTotal;
	private String fechaProceso;

	public VentaIntegralResumenSemana() {
	}

	public VentaIntegralResumenSemana(int idTienda, int idCategoria, String semanaInicio, String semanaFin,
			double cantidadTienda, double cantidadContactCenter) {
		this.idTienda = idTienda;
		this.idCategoria = idCategoria;
		this.semanaInicio = semanaInicio;
		this.semanaFin = semanaFin;
		this.cantidadTienda = cantidadTienda;
		this.cantidadContactCenter = cantidadContactCenter;
		this.cantidadTotal = cantidadTienda + cantidadContactCenter;
	}

	public int getIdResumen() {
		return idResumen;
	}

	public void setIdResumen(int idResumen) {
		this.idResumen = idResumen;
	}

	public int getIdTienda() {
		return idTienda;
	}

	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}

	public String getNombreTienda() {
		return nombreTienda;
	}

	public void setNombreTienda(String nombreTienda) {
		this.nombreTienda = nombreTienda;
	}

	public int getIdCategoria() {
		return idCategoria;
	}

	public void setIdCategoria(int idCategoria) {
		this.idCategoria = idCategoria;
	}

	public String getNombreCategoria() {
		return nombreCategoria;
	}

	public void setNombreCategoria(String nombreCategoria) {
		this.nombreCategoria = nombreCategoria;
	}

	public String getSemanaInicio() {
		return semanaInicio;
	}

	public void setSemanaInicio(String semanaInicio) {
		this.semanaInicio = semanaInicio;
	}

	public String getSemanaFin() {
		return semanaFin;
	}

	public void setSemanaFin(String semanaFin) {
		this.semanaFin = semanaFin;
	}

	public double getCantidadTienda() {
		return cantidadTienda;
	}

	public void setCantidadTienda(double cantidadTienda) {
		this.cantidadTienda = cantidadTienda;
	}

	public double getCantidadContactCenter() {
		return cantidadContactCenter;
	}

	public void setCantidadContactCenter(double cantidadContactCenter) {
		this.cantidadContactCenter = cantidadContactCenter;
	}

	public double getCantidadTotal() {
		return cantidadTotal;
	}

	public void setCantidadTotal(double cantidadTotal) {
		this.cantidadTotal = cantidadTotal;
	}

	public String getFechaProceso() {
		return fechaProceso;
	}

	public void setFechaProceso(String fechaProceso) {
		this.fechaProceso = fechaProceso;
	}

}
