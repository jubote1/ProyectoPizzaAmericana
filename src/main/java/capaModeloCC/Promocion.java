package capaModeloCC;

public class Promocion {

	private int idPromocion;
	private String descripcion;
	public int getIdPromocion() {
		return idPromocion;
	}
	public void setIdPromocion(int idPromocion) {
		this.idPromocion = idPromocion;
	}
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	public Promocion(int idPromocion, String descripcion) {
		super();
		this.idPromocion = idPromocion;
		this.descripcion = descripcion;
	}
	
	
	
}
