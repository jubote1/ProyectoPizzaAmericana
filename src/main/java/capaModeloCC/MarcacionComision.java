package capaModeloCC;

public class MarcacionComision {
	
	private int idMarcacion;
	private int idTienda;
	private int comision;
	private int comisionfull;
	
	
	public int getIdMarcacion() {
		return idMarcacion;
	}


	public void setIdMarcacion(int idMarcacion) {
		this.idMarcacion = idMarcacion;
	}


	public int getIdTienda() {
		return idTienda;
	}


	public void setIdTienda(int idTienda) {
		this.idTienda = idTienda;
	}


	public int getComision() {
		return comision;
	}


	public void setComision(int comision) {
		this.comision = comision;
	}


	public int getComisionfull() {
		return comisionfull;
	}


	public void setComisionfull(int comisionfull) {
		this.comisionfull = comisionfull;
	}


	public MarcacionComision(int idMarcacion, int idTienda, int comision, int comisionfull) {
		super();
		this.idMarcacion = idMarcacion;
		this.idTienda = idTienda;
		this.comision = comision;
		this.comisionfull = comisionfull;
	}


	
	
	

}
