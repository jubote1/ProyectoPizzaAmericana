package capaModeloCC;

/**
 * Modelo que representa una opcion de respuesta de una pregunta de mercadeo
 * de tipo O (opciones).
 * Corresponde a la tabla opcion_mercadeo de la base de datos pizzaamericana.
 */
public class OpcionMercadeo {

	private int idOpcion;
	private int idPregunta;
	private String descripcion;
	private int orden;
	private String activo;

	public OpcionMercadeo() {
		super();
	}

	public OpcionMercadeo(final int idOpcion, final int idPregunta, final String descripcion, final int orden,
			final String activo) {
		super();
		this.idOpcion = idOpcion;
		this.idPregunta = idPregunta;
		this.descripcion = descripcion;
		this.orden = orden;
		this.activo = activo;
	}

	public int getIdOpcion() {
		return this.idOpcion;
	}

	public void setIdOpcion(final int idOpcion) {
		this.idOpcion = idOpcion;
	}

	public int getIdPregunta() {
		return this.idPregunta;
	}

	public void setIdPregunta(final int idPregunta) {
		this.idPregunta = idPregunta;
	}

	public String getDescripcion() {
		return this.descripcion;
	}

	public void setDescripcion(final String descripcion) {
		this.descripcion = descripcion;
	}

	public int getOrden() {
		return this.orden;
	}

	public void setOrden(final int orden) {
		this.orden = orden;
	}

	public String getActivo() {
		return this.activo;
	}

	public void setActivo(final String activo) {
		this.activo = activo;
	}
}
