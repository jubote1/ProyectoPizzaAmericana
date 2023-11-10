package capaModeloCC;

public class HomologaGaseosaIncluida {
	
	private int idtienda;
	private int idsabortipoliquido;
	private String nombre;
	private double precioGeneral;
	
	
	
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public double getPrecioGeneral() {
		return precioGeneral;
	}
	public void setPrecioGeneral(double precioGeneral) {
		this.precioGeneral = precioGeneral;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public int getIdsabortipoliquido() {
		return idsabortipoliquido;
	}
	public void setIdsabortipoliquido(int idsabortipoliquido) {
		this.idsabortipoliquido = idsabortipoliquido;
	}
	public HomologaGaseosaIncluida(int idtienda, int idsabortipoliquido) {
		super();
		this.idtienda = idtienda;
		this.idsabortipoliquido = idsabortipoliquido;
	}
	
	
	
	

}
