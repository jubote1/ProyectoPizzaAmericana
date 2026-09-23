package utilidadesCC;

/**
 * Lee un valor en pesos escrito por una persona.
 *
 * POR QUE EXISTE ESTO EN UN SOLO LADO
 *
 * Porque cada vez que se resolvio aparte salio distinto, y las tres versiones
 * estaban mal de forma diferente:
 *
 *   InsertarSolicitudConciliacion  Double.parseDouble suelto, y el catch dejaba
 *                                  el valor en CERO. "150.000" guardaba 150
 *                                  -mil veces menos- y "150,000" guardaba cero.
 *                                  Ninguno avisaba. En solicitud_conciliacion
 *                                  quedaron seis filas asi.
 *
 *   ActualizarSolicitudConciliacion  Borraba TODOS los puntos, asi que "1377.5"
 *                                    quedaba en 13775, diez veces mas.
 *
 * Un cero se ve; una cifra diez o mil veces distinta pasa de largo. Por eso la
 * regla vive aqui y los dos servicios la llaman.
 *
 * LA REGLA
 *
 * La convencion de aca: punto para los miles, coma para los decimales. Asi
 * "150.000" son ciento cincuenta mil y "1377,5" son mil trescientos setenta y
 * siete con cinco.
 *
 * El punto solo es ambiguo, porque "150.000" son ciento cincuenta mil pero
 * "1377.5" son mil trescientos setenta y siete con cinco. Se decide por
 * cuantos digitos quedan detras del punto: tres son miles, uno o dos son
 * decimales. Es como se escribe de verdad.
 *
 * Cuando no se entiende devuelve null, y el que llama NO guarda. Es preferible
 * que la pantalla diga "revise el valor" a que el dato se pierda en silencio.
 *
 * El espejo de esta regla en el navegador es normalizarValor, en
 * js/conciliacionqrdatafono.js. Alli solo sirve para avisarle al usuario antes
 * de enviar; quien decide es este. Si una cambia, la otra tambien.
 */
public class ValorDigitado {

	/** Nadie construye esto: son reglas, no un objeto. */
	private ValorDigitado() {
	}

	/**
	 * @param valor        lo que escribio la persona
	 * @param permitirVacio true cuando dejarlo en blanco es valido y significa
	 *        cero -por ejemplo el valor final de una solicitud que todavia no
	 *        se ha resuelto-; false cuando el campo es obligatorio y borrarlo
	 *        sin querer no puede terminar en un cero guardado
	 * @return el valor, o null si no se entiende o es negativo
	 */
	public static Double leer(final String valor, final boolean permitirVacio) {
		if (valor == null || valor.trim().length() == 0) {
			return (permitirVacio ? Double.valueOf(0) : null);
		}
		//Un signo de pesos o un espacio pegado no deberian tumbar el guardado.
		String limpio = valor.trim().replace("$", "").replace(" ", "");

		if (limpio.indexOf(',') >= 0) {
			//Hay coma: la coma es el decimal y los puntos son miles.
			limpio = limpio.replace(".", "").replace(',', '.');
		} else {
			final int primerPunto = limpio.indexOf('.');
			final int ultimoPunto = limpio.lastIndexOf('.');
			final int digitosFinales = limpio.length() - ultimoPunto - 1;
			if (primerPunto >= 0 && primerPunto == ultimoPunto && digitosFinales > 0
					&& digitosFinales <= 2) {
				//Un solo punto con uno o dos digitos detras: es decimal, se deja.
			} else {
				//Varios puntos, o tres digitos detras: son separadores de miles.
				limpio = limpio.replace(".", "");
			}
		}
		try {
			final double leido = Double.parseDouble(limpio);
			//Un valor negativo no existe aqui y da vuelta al signo de la
			//diferencia: un faltante se vuelve sobrante.
			if (leido < 0) {
				return (null);
			}
			return (Double.valueOf(leido));
		} catch (final Exception e) {
			return (null);
		}
	}
}
