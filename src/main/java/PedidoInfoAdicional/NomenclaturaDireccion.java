package PedidoInfoAdicional;

public class NomenclaturaDireccion {
	
	private int idnomemclatura;
	private String nomenclatura;
	public int getIdnomemclatura() {
		return idnomemclatura;
	}
	public void setIdnomemclatura(int idnomemclatura) {
		this.idnomemclatura = idnomemclatura;
	}
	public String getNomenclatura() {
		return nomenclatura;
	}
	public void setNomenclatura(String nomenclatura) {
		this.nomenclatura = nomenclatura;
	}
	public NomenclaturaDireccion(int idnomemclatura, String nomenclatura) {
		super();
		this.idnomemclatura = idnomemclatura;
		this.nomenclatura = nomenclatura;
	}
	
	

}
