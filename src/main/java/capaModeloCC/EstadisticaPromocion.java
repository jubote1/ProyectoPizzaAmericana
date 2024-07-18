package capaModeloCC;

public class EstadisticaPromocion {
	
	private String fecha;
	private int idTienda;
	private int idPromocion;
	private int tiendaVirtual;
	private int contact;
	private int total;
	private String promocion;
	
	public String getPromocion() {
		return promocion;
	}
	public void setPromocion(String promocion) {
		this.promocion = promocion;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public int getIdTienda() {
		return idTienda;
	}
	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}
	public int getIdPromocion() {
		return idPromocion;
	}
	public void setIdPromocion(int idPromocion) {
		this.idPromocion = idPromocion;
	}
	public int getTiendaVirtual() {
		return tiendaVirtual;
	}
	public void setTiendaVirtual(int tiendaVirtual) {
		this.tiendaVirtual = tiendaVirtual;
	}
	public int getContact() {
		return contact;
	}
	public void setContact(int contact) {
		this.contact = contact;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public EstadisticaPromocion(String fecha, int idTienda, int idPromocion, int contact, int tiendaVirtual,
			int total, String promocion) {
		super();
		this.fecha = fecha;
		this.idTienda = idTienda;
		this.idPromocion = idPromocion;
		this.tiendaVirtual = tiendaVirtual;
		this.contact = contact;
		this.total = total;
		this.promocion = promocion;
	}
	
}
