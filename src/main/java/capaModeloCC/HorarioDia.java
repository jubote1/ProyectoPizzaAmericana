package capaModeloCC;

public class HorarioDia {
	
	private int idDia;
	private String nombre;
	private String horaApertura;
	private String horaCierre;
	public int getIdDia() {
		return idDia;
	}
	public void setIdDia(int idDia) {
		this.idDia = idDia;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getHoraApertura() {
		return horaApertura;
	}
	public void setHoraApertura(String horaApertura) {
		this.horaApertura = horaApertura;
	}
	public String getHoraCierre() {
		return horaCierre;
	}
	public void setHoraCierre(String horaCierre) {
		this.horaCierre = horaCierre;
	}
	public HorarioDia(int idDia, String nombre, String horaApertura, String horaCierre) {
		super();
		this.idDia = idDia;
		this.nombre = nombre;
		this.horaApertura = horaApertura;
		this.horaCierre = horaCierre;
	}
	
	

}
