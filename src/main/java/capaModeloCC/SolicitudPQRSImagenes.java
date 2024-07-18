package capaModeloCC;

public class SolicitudPQRSImagenes {
	
	private int idImagen;
	private int idSolicitudPQRS;
	private String rutaImagen;
	public int getIdImagen() {
		return idImagen;
	}
	public void setIdImagen(int idImagen) {
		this.idImagen = idImagen;
	}
	public int getIdSolicitudPQRS() {
		return idSolicitudPQRS;
	}
	public void setIdSolicitudPQRS(int idSolicitudPQRS) {
		this.idSolicitudPQRS = idSolicitudPQRS;
	}
	public String getRutaImagen() {
		return rutaImagen;
	}
	public void setRutaImagen(String rutaImagen) {
		this.rutaImagen = rutaImagen;
	}
	public SolicitudPQRSImagenes(int idImagen, int idSolicitudPQRS, String rutaImagen) {
		super();
		this.idImagen = idImagen;
		this.idSolicitudPQRS = idSolicitudPQRS;
		this.rutaImagen = rutaImagen;
	}
	
	

}
