package capaModeloCC;

public class AreaEscalamientoPQRS {
	
	private int idArea;
	private String area;
	private String correo;
	public int getIdArea() {
		return idArea;
	}
	public void setIdArea(int idArea) {
		this.idArea = idArea;
	}
	public String getArea() {
		return area;
	}
	public void setArea(String area) {
		this.area = area;
	}
	public String getCorreo() {
		return correo;
	}
	public void setCorreo(String correo) {
		this.correo = correo;
	}
	public AreaEscalamientoPQRS(int idArea, String area, String correo) {
		super();
		this.idArea = idArea;
		this.area = area;
		this.correo = correo;
	}
	
	

}
