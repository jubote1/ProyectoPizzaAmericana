package capaModeloCC;

import java.util.Date;

public class PeriodoNomina {
	
	private int idPeriodo;
	private Date fechaInferior;
	private Date fechaSuperior;
	public int getIdPeriodo() {
		return idPeriodo;
	}
	public void setIdPeriodo(int idPeriodo) {
		this.idPeriodo = idPeriodo;
	}
	public Date getFechaInferior() {
		return fechaInferior;
	}
	public void setFechaInferior(Date fechaInferior) {
		this.fechaInferior = fechaInferior;
	}
	public Date getFechaSuperior() {
		return fechaSuperior;
	}
	public void setFechaSuperior(Date fechaSuperior) {
		this.fechaSuperior = fechaSuperior;
	}
	public PeriodoNomina(int idPeriodo, Date fechaInferior, Date fechaSuperior) {
		super();
		this.idPeriodo = idPeriodo;
		this.fechaInferior = fechaInferior;
		this.fechaSuperior = fechaSuperior;
	}
	

}
