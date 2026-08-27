package capaModeloCC;

import java.util.ArrayList;

/**
 * Modelo que representa una pregunta configurada para las encuestas de mercadeo
 * que se aplican en el POS al finalizar un pedido.
 * Corresponde a la tabla pregunta_mercadeo de la base de datos pizzaamericana.
 */
public class PreguntaMercadeo {

	/** Tipo de pregunta booleana: la respuesta es Si o No. */
	public static final String TIPO_BOOLEANA = "B";
	/** Tipo de pregunta de opciones: la respuesta es una de las opciones configuradas. */
	public static final String TIPO_OPCIONES = "O";
	/** Tipo de pregunta abierta: la respuesta es texto libre. */
	public static final String TIPO_ABIERTA = "A";

	private int idPregunta;
	private String titulo;
	private String descripcion;
	private String tipo;
	private String fechaInicio;
	private String fechaFin;
	private int orden;
	private String obligatoria;
	private String aplicaPV;
	private String aplicaDomicilio;
	private String activo;
	private String usuarioCreacion;
	private ArrayList<OpcionMercadeo> opciones;

	public PreguntaMercadeo() {
		super();
		this.opciones = new ArrayList<OpcionMercadeo>();
	}

	public PreguntaMercadeo(final int idPregunta, final String titulo, final String descripcion, final String tipo,
			final String fechaInicio, final String fechaFin, final int orden, final String obligatoria,
			final String aplicaPV, final String aplicaDomicilio, final String activo, final String usuarioCreacion) {
		super();
		this.idPregunta = idPregunta;
		this.titulo = titulo;
		this.descripcion = descripcion;
		this.tipo = tipo;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.orden = orden;
		this.obligatoria = obligatoria;
		this.aplicaPV = aplicaPV;
		this.aplicaDomicilio = aplicaDomicilio;
		this.activo = activo;
		this.usuarioCreacion = usuarioCreacion;
		this.opciones = new ArrayList<OpcionMercadeo>();
	}

	public int getIdPregunta() {
		return this.idPregunta;
	}

	public void setIdPregunta(final int idPregunta) {
		this.idPregunta = idPregunta;
	}

	public String getTitulo() {
		return this.titulo;
	}

	public void setTitulo(final String titulo) {
		this.titulo = titulo;
	}

	public String getDescripcion() {
		return this.descripcion;
	}

	public void setDescripcion(final String descripcion) {
		this.descripcion = descripcion;
	}

	public String getTipo() {
		return this.tipo;
	}

	public void setTipo(final String tipo) {
		this.tipo = tipo;
	}

	public String getFechaInicio() {
		return this.fechaInicio;
	}

	public void setFechaInicio(final String fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public String getFechaFin() {
		return this.fechaFin;
	}

	public void setFechaFin(final String fechaFin) {
		this.fechaFin = fechaFin;
	}

	public int getOrden() {
		return this.orden;
	}

	public void setOrden(final int orden) {
		this.orden = orden;
	}

	public String getObligatoria() {
		return this.obligatoria;
	}

	public void setObligatoria(final String obligatoria) {
		this.obligatoria = obligatoria;
	}

	public String getAplicaPV() {
		return this.aplicaPV;
	}

	public void setAplicaPV(final String aplicaPV) {
		this.aplicaPV = aplicaPV;
	}

	public String getAplicaDomicilio() {
		return this.aplicaDomicilio;
	}

	public void setAplicaDomicilio(final String aplicaDomicilio) {
		this.aplicaDomicilio = aplicaDomicilio;
	}

	public String getActivo() {
		return this.activo;
	}

	public void setActivo(final String activo) {
		this.activo = activo;
	}

	public String getUsuarioCreacion() {
		return this.usuarioCreacion;
	}

	public void setUsuarioCreacion(final String usuarioCreacion) {
		this.usuarioCreacion = usuarioCreacion;
	}

	public ArrayList<OpcionMercadeo> getOpciones() {
		return this.opciones;
	}

	public void setOpciones(final ArrayList<OpcionMercadeo> opciones) {
		this.opciones = opciones;
	}
}
