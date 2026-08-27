package capaModeloCC;

/**
 * Modelo que representa una respuesta de encuesta de mercadeo tomada en el POS.
 * Corresponde a la tabla encuesta_mercadeo de la base de datos pizzaamericana.
 *
 * Se guarda una fila por respuesta, siguiendo el mismo esquema plano que ya usan
 * encuesta_servicio y encuesta_pqrs.
 *
 * OJO: idPedidoTienda es el consecutivo del pedido en la TIENDA
 * (tiendaamericana.pedido.idpedidotienda), NO el de pizzaamericana.pedido.idpedido,
 * que es una secuencia distinta. El par (idPedidoTienda, idTienda) identifica el
 * pedido, igual que numero_pedido + idtienda en log_encuesta_servicio.
 * idCliente tambien es el de la tienda.
 */
public class EncuestaMercadeo {

	private int idEncuesta;
	private int idPregunta;
	private String respuesta;
	private int idOpcion;
	private int idPedidoTienda;
	private int idTienda;
	private int idCliente;
	private int idUsuario;
	private String fechaCreacion;

	public EncuestaMercadeo() {
		super();
	}

	public EncuestaMercadeo(final int idPregunta, final String respuesta, final int idOpcion,
			final int idPedidoTienda, final int idTienda, final int idCliente, final int idUsuario) {
		super();
		this.idPregunta = idPregunta;
		this.respuesta = respuesta;
		this.idOpcion = idOpcion;
		this.idPedidoTienda = idPedidoTienda;
		this.idTienda = idTienda;
		this.idCliente = idCliente;
		this.idUsuario = idUsuario;
	}

	public int getIdEncuesta() {
		return this.idEncuesta;
	}

	public void setIdEncuesta(final int idEncuesta) {
		this.idEncuesta = idEncuesta;
	}

	public int getIdPregunta() {
		return this.idPregunta;
	}

	public void setIdPregunta(final int idPregunta) {
		this.idPregunta = idPregunta;
	}

	public String getRespuesta() {
		return this.respuesta;
	}

	public void setRespuesta(final String respuesta) {
		this.respuesta = respuesta;
	}

	public int getIdOpcion() {
		return this.idOpcion;
	}

	public void setIdOpcion(final int idOpcion) {
		this.idOpcion = idOpcion;
	}

	public int getIdPedidoTienda() {
		return this.idPedidoTienda;
	}

	public void setIdPedidoTienda(final int idPedidoTienda) {
		this.idPedidoTienda = idPedidoTienda;
	}

	public int getIdTienda() {
		return this.idTienda;
	}

	public void setIdTienda(final int idTienda) {
		this.idTienda = idTienda;
	}

	public int getIdCliente() {
		return this.idCliente;
	}

	public void setIdCliente(final int idCliente) {
		this.idCliente = idCliente;
	}

	public int getIdUsuario() {
		return this.idUsuario;
	}

	public void setIdUsuario(final int idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getFechaCreacion() {
		return this.fechaCreacion;
	}

	public void setFechaCreacion(final String fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}
}
