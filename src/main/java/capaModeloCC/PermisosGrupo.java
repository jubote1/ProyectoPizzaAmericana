package capaModeloCC;

public class PermisosGrupo {
	
	
	private int idPermiso;
	private int idGrupo;
	private String nombrePermiso;
	public int getIdPermiso() {
		return idPermiso;
	}
	public void setIdPermiso(int idPermiso) {
		this.idPermiso = idPermiso;
	}
	public int getIdGrupo() {
		return idGrupo;
	}
	public void setIdGrupo(int idGrupo) {
		this.idGrupo = idGrupo;
	}
	public String getNombrePermiso() {
		return nombrePermiso;
	}
	public void setNombrePermiso(String nombrePermiso) {
		this.nombrePermiso = nombrePermiso;
	}
	public PermisosGrupo(int idPermiso, int idGrupo, String nombrePermiso) {
		super();
		this.idPermiso = idPermiso;
		this.idGrupo = idGrupo;
		this.nombrePermiso = nombrePermiso;
	}
	
	

}
