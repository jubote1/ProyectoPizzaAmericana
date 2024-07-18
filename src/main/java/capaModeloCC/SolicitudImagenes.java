package capaModeloCC;

public class SolicitudImagenes {
	
	private int idImagen;
	private int idSolicitud;
	private String rutaImagen;
	public int getIdImagen() {
		return idImagen;
	}
	public void setIdImagen(int idImagen) {
		this.idImagen = idImagen;
	}
	public int getIdSolicitud() {
		return idSolicitud;
	}
	public void setIdSolicitud(int idSolicitud) {
		this.idSolicitud = idSolicitud;
	}
	public String getRutaImagen() {
		return rutaImagen;
	}
	public void setRutaImagen(String rutaImagen) {
		this.rutaImagen = rutaImagen;
	}
	public SolicitudImagenes(int idImagen, int idSolicitud, String rutaImagen) {
		super();
		this.idImagen = idImagen;
		this.idSolicitud = idSolicitud;
		this.rutaImagen = rutaImagen;
	}
	
	

}
