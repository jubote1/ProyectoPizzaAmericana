package utilidadesCC;

import capaModeloCC.PedidoMonitoreoPagoVirtual;

/**
 * Traduce a palabras que le paso al pago de un pedido.
 *
 * La pantalla de monitoreo se llama "monitoreo de pagos virtuales" y hasta
 * ahora la unica pista que daba era cuantos minutos llevaba el pedido
 * esperando. Un pedido de 30 minutos podia ser uno al que el cliente le
 * rechazaron la tarjeta dos veces, uno que nunca recibio el link porque no se
 * genero, o uno que ya pago y todavia no se despacha. Las tres cosas pedian
 * acciones distintas y se veian iguales.
 *
 * Aqui se juntan las tres fuentes que ya existian -el estado del pedido, la
 * fecha de pago y los eventos que devolvio Wompi- para decir en una palabra
 * cual de los tres casos es, y con que urgencia hay que atenderlo.
 *
 * El nivel no es un color: es la urgencia. La pantalla decide el color, pero
 * quien ordena por urgencia y quien filtra "muestrame solo lo que hay que
 * salvar" trabaja sobre el nivel.
 */
public final class EstadoPagoVirtual {

	/** Ya entro la plata. No hay nada que hacer. */
	public static final int NIVEL_PAGADO = 1;

	/** Va en tiempo, todavia no se le ha recordado. */
	public static final int NIVEL_EN_TIEMPO = 2;

	/** Ya paso el aviso: se le recordo y sigue sin pagar. */
	public static final int NIVEL_AVISADO = 3;

	/** Hay que llamarlo AHORA: esta por vencerse, o algo fallo en el pago. */
	public static final int NIVEL_URGENTE = 4;

	/** Se perdio: paso el tiempo o ya se cancelo. */
	public static final int NIVEL_PERDIDO = 5;

	/** Cuantos minutos antes de la cancelacion se considera urgente. */
	private static final int MINUTOS_ULTIMO_LLAMADO = 10;

	private EstadoPagoVirtual() {
		super();
	}

	/** Estado del pedido cuando esta esperando el pago virtual. */
	private static final int PEDIDO_ESPERANDO_PAGO = 2;

	/** Estado del pedido despues de cancelarlo por no pago. */
	private static final int PEDIDO_CANCELADO = 4;

	/**
	 * Le pone estado y nivel al pedido, sobre lo que ya trae de la base.
	 *
	 * El orden de las preguntas importa: primero si entro la plata, porque eso
	 * gana sobre cualquier otra cosa -incluso sobre un pedido ya cancelado, que es
	 * justo el caso que hay que ver-; despues si se perdio; y solo al final, para
	 * los que siguen vivos, con cuanta urgencia hay que atenderlos.
	 *
	 * Los dos tiempos entran por parametro y no se leen aqui a proposito: esto se
	 * llama una vez por fila del listado, y leerlos adentro serian dos consultas a
	 * la tabla de parametros por cada pedido de la pantalla.
	 */
	public static void resolver(final PedidoMonitoreoPagoVirtual pedido, final int minutosAviso,
			final int minutosCancela) {
		if (pedido == null) {
			return;
		}
		final boolean pago = tiene(pedido.getFechaPago());
		final boolean cancelado = (pedido.getIdEstadoPedido() == PEDIDO_CANCELADO);
		final String ultimo = pedido.getUltimoEstado() == null ? "" : pedido.getUltimoEstado().toUpperCase();

		if (pago && cancelado) {
			//El cliente pago despues de que se le cancelo el pedido. Es el caso mas
			//delicado de todos: hay plata recibida y no hay pedido que entregar, y
			//nadie se entera porque el pedido ya no sale en ninguna pantalla.
			marcar(pedido, "PAGO DESPUES DE CANCELAR", NIVEL_URGENTE);
			return;
		}
		if (pago) {
			marcar(pedido, "PAGADO", NIVEL_PAGADO);
			return;
		}
		if (cancelado) {
			marcar(pedido, "CANCELADO SIN PAGO", NIVEL_PERDIDO);
			return;
		}
		if (!tiene(pedido.getIdLink())) {
			//Sin link el cliente no tiene por donde pagar. Le corre el reloj igual y
			//se le va a cancelar sin haber tenido la oportunidad.
			marcar(pedido, "SIN LINK DE PAGO", NIVEL_URGENTE);
			return;
		}
		if ("DECLINED".equals(ultimo)) {
			final int intentos = pedido.getRechazos();
			marcar(pedido, intentos > 1 ? "RECHAZADO " + intentos + " VECES" : "PAGO RECHAZADO", NIVEL_URGENTE);
			return;
		}
		if ("VOIDED".equals(ultimo) || "ERROR".equals(ultimo)) {
			marcar(pedido, "FALLA EN LA PASARELA", NIVEL_URGENTE);
			return;
		}
		if ("PENDING".equals(ultimo)) {
			//El cliente si esta intentando pagar: no es lo mismo que no hacer nada.
			marcar(pedido, "PAGO EN PROCESO", NIVEL_AVISADO);
			return;
		}
		if (pedido.getIdEstadoPedido() != PEDIDO_ESPERANDO_PAGO) {
			//Ya no esta esperando el pago y no se pago ni se cancelo: es otro flujo.
			//Se muestra lo que diga el catalogo en vez de inventarle un estado.
			marcar(pedido, tiene(pedido.getEstadoPedido()) ? pedido.getEstadoPedido() : "OTRO ESTADO",
					NIVEL_EN_TIEMPO);
			return;
		}
		marcar(pedido, textoEsperando(pedido, minutosCancela),
				nivelEsperando(pedido, minutosAviso, minutosCancela));
	}

	/** Para los que siguen esperando: que tan cerca estan de perderse. */
	private static int nivelEsperando(final PedidoMonitoreoPagoVirtual pedido, final int aviso,
			final int cancela) {
		final int minutos = pedido.getMinutos();
		if (minutos >= cancela) {
			return (NIVEL_PERDIDO);
		}
		if (minutos >= cancela - MINUTOS_ULTIMO_LLAMADO) {
			return (NIVEL_URGENTE);
		}
		if (minutos >= aviso) {
			return (NIVEL_AVISADO);
		}
		return (NIVEL_EN_TIEMPO);
	}

	private static String textoEsperando(final PedidoMonitoreoPagoVirtual pedido, final int cancela) {
		final int minutos = pedido.getMinutos();
		if (minutos >= cancela) {
			//Paso el tiempo y el proceso todavia no ha pasado a cancelarlo. Puede ser
			//que corra en unos minutos, o que el pedido sea de los que el proceso no
			//cancela -los de APP y los programados-. En los dos casos esta ahi, sin
			//pagar y sin que nadie lo esté mirando.
			return ("VENCIDO SIN PAGO");
		}
		if (minutos >= cancela - MINUTOS_ULTIMO_LLAMADO) {
			return ("SE VENCE EN " + (cancela - minutos) + " MIN");
		}
		//Mas de un aviso quiere decir que ya se le mando el recordatorio: el primero
		//es el envio del link.
		if (pedido.getAvisos() > 1) {
			return ("RECORDADO, SIN PAGAR");
		}
		return ("ESPERANDO PAGO");
	}

	private static void marcar(final PedidoMonitoreoPagoVirtual pedido, final String estado, final int nivel) {
		pedido.setEstado(estado);
		pedido.setNivel(nivel);
	}

	private static boolean tiene(final String valor) {
		return (valor != null && valor.trim().length() > 0);
	}
}
