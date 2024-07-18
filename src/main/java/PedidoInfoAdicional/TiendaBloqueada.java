package PedidoInfoAdicional;

public class TiendaBloqueada {
	
	private int idtienda;
	private String nombreTienda;
	private String comentario;
	private String fechaAuditoria;
	
	
	public String getFechaAuditoria() {
		return fechaAuditoria;
	}
	public void setFechaAuditoria(String fechaAuditoria) {
		this.fechaAuditoria = fechaAuditoria;
	}
	public int getIdtienda() {
		return idtienda;
	}
	public void setIdtienda(int idtienda) {
		this.idtienda = idtienda;
	}
	public String getNombreTienda() {
		return nombreTienda;
	}
	public void setNombreTienda(String nombreTienda) {
		this.nombreTienda = nombreTienda;
	}
	public String getComentario() {
		return comentario;
	}
	public void setComentario(String comentario) {
		this.comentario = comentario;
	}
	public TiendaBloqueada(int idtienda, String nombreTienda, String comentario, String fechaAuditoria) {
		super();
		this.idtienda = idtienda;
		this.nombreTienda = nombreTienda;
		this.comentario = comentario;
		this.fechaAuditoria = fechaAuditoria;
	}
	
	
	

}
