package utilidadesCC;

import capaDAOCC.ParametrosDAO;

/**
 * Los tiempos del pago virtual, en un solo lugar.
 *
 * Estaban quemados y repetidos, y peor: no coincidian entre si. Al cliente se
 * le decia por mensaje de texto "tendras 20 minutos mas", por correo "tendras
 * 10 minutos mas", y el pedido se cancelaba a los 50 -o sea 30 despues del
 * aviso-. Tres cifras distintas y las tres equivocadas, en el momento en que el
 * cliente esta decidiendo si paga o no.
 *
 * Ahora el proceso y los mensajes leen de aqui, asi que lo que se le dice al
 * cliente es lo que de verdad va a pasar. Y se puede mover sin recompilar, que
 * era imposible con los numeros metidos en medio de las frases.
 */
public final class TiemposPagoVirtual {

	/** A los cuantos minutos sin pagar se le manda el recordatorio. */
	public static final String PARAM_AVISO = "WOMPIMINUTOSAVISO";

	/** A los cuantos minutos sin pagar se cancela el pedido. */
	public static final String PARAM_CANCELA = "WOMPIMINUTOSCANCELA";

	private static final int AVISO_POR_DEFECTO = 20;

	private static final int CANCELA_POR_DEFECTO = 50;

	private TiemposPagoVirtual() {
		super();
	}

	/** Minutos sin pagar tras los cuales se le recuerda al cliente. */
	public static int minutosAviso() {
		return (leer(PARAM_AVISO, AVISO_POR_DEFECTO));
	}

	/** Minutos sin pagar tras los cuales se cancela el pedido. */
	public static int minutosCancela() {
		final int cancela = leer(PARAM_CANCELA, CANCELA_POR_DEFECTO);
		//Si alguien deja la cancelacion antes del aviso, el cliente recibiria el
		//recordatorio despues de que le cancelaron el pedido. Se corrige aqui en
		//vez de dejar que el proceso quede sin sentido.
		return (cancela > minutosAviso() ? cancela : minutosAviso() + 10);
	}

	/**
	 * Cuantos minutos le quedan al cliente desde que recibe el recordatorio.
	 *
	 * Es la cifra que hay que decirle: no "llevas 20 minutos" sino "te quedan
	 * 30". Lo primero es un reproche, lo segundo es lo que necesita saber.
	 */
	public static int minutosRestantes() {
		return (minutosCancela() - minutosAviso());
	}

	private static int leer(final String parametro, final int porDefecto) {
		try {
			final int valor = ParametrosDAO.retornarValorNumerico(parametro);
			return (valor > 0 ? valor : porDefecto);
		} catch (final Exception e) {
			//Sin parametro el proceso tiene que seguir andando con los tiempos que
			//venia usando, no quedarse en cero y cancelar todo de una.
			System.out.println("TiemposPagoVirtual: no se pudo leer " + parametro + ", " + e);
			return (porDefecto);
		}
	}
}
