package capaSeguridad;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * La firma del QR de la encuesta.
 *
 * EL PROBLEMA QUE RESUELVE. El QR del tiquete lleva el numero de pedido a la
 * vista: ...?idpedido=328808&idtienda=5&tipo=pv. Cambiando ese numero en el
 * navegador se contesta la encuesta de un pedido ajeno, y cada encuesta da un
 * giro de ruleta. Asi se colaron 315 encuestas entre mayo y noviembre de 2026,
 * 75 de ellas en una sola noche.
 *
 * COMO LO RESUELVE. Al imprimir, el POS agrega a la direccion un vencimiento y
 * una firma corta que sale de combinar tienda, pedido, tipo y vencimiento con
 * una CLAVE SECRETA que solo conocen el POS y el central. El central vuelve a
 * calcularla y compara. La firma de un pedido no sirve para otro: cambiar el
 * numero la invalida, y no hay forma de calcular la del pedido siguiente sin
 * tener la clave.
 *
 * POR QUE UNA FIRMA Y NO UN CODIGO GUARDADO EN LA BASE. Un codigo al azar
 * obligaria al POS a escribirle al central EN EL MOMENTO DE IMPRIMIR, y si la
 * tienda esta sin internet el tiquete saldria sin QR. La tienda tiene que poder
 * vender sin red. Esta firma se calcula sin conectarse a nada: es pura cuenta
 * sobre datos que el POS ya tiene.
 *
 * POR QUE ESTA CLASE VIVE EN EL CENTRAL Y VIAJA A LA TIENDA COMO LIBRERIA. Si se
 * escribiera dos veces, basta una diferencia de un caracter en el texto que se
 * firma para que el central rechace TODAS las encuestas de la tienda. Es la
 * misma razon del motor de pedidos: una sola copia de la regla, dos que la
 * llaman.
 *
 * HASTA DONDE LLEGA. Quien tenga acceso a la base de datos de la tienda puede
 * leer la clave y seguir firmando. Esto no lo hace imposible: lo vuelve caro y
 * deja rastro. Hoy el fraude cuesta cero esfuerzo.
 */
public class FirmaEncuestaQR {

	/** Version del formato. Si cambia lo que se firma, sube, y las dos partes se siguen entendiendo. */
	public static final String VERSION = "1";

	/** Largo de la firma en la direccion: 16 caracteres hexadecimales, 64 bits. */
	private static final int LARGO_FIRMA = 16;

	private static final String FORMATO_VENCIMIENTO = "yyyyMMddHHmm";

	/** Nombre del parametro donde vive la clave, igual en la tienda y en el central. */
	public static final String PARAMETRO_CLAVE = "QRENCUESTA_CLAVE";

	private FirmaEncuestaQR() {
	}

	/**
	 * El texto que se firma. Es lo UNICO que tiene que coincidir entre los dos
	 * lados, y por eso esta aqui y no repartido en quien llama.
	 */
	private static String textoAFirmar(final int idTienda, final int idPedido, final String tipo,
			final String vencimiento) {
		return "v" + VERSION + "|" + idTienda + "|" + idPedido + "|"
				+ (tipo == null ? "" : tipo.trim().toLowerCase()) + "|" + vencimiento;
	}

	/**
	 * Calcula la firma. Devuelve cadena vacia si no hay clave: quien llama debe
	 * entender eso como "no firmar" y seguir, nunca como un error que detenga la
	 * impresion de un tiquete.
	 */
	public static String firmar(final int idTienda, final int idPedido, final String tipo,
			final String vencimiento, final String clave) {
		if (clave == null || clave.trim().isEmpty() || vencimiento == null || vencimiento.trim().isEmpty()) {
			return "";
		}
		try {
			final Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(clave.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			final byte[] crudo = mac.doFinal(
					textoAFirmar(idTienda, idPedido, tipo, vencimiento).getBytes(StandardCharsets.UTF_8));
			final StringBuilder hex = new StringBuilder();
			for (final byte b : crudo) {
				hex.append(String.format("%02x", b));
				if (hex.length() >= LARGO_FIRMA) {
					break;
				}
			}
			return hex.substring(0, LARGO_FIRMA);
		} catch (final Exception e) {
			// Sin firma antes que sin tiquete.
			return "";
		}
	}

	/** El vencimiento, en el formato de la direccion, a tantas horas de ahora. */
	public static String vencimientoEn(final int horas) {
		final Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR_OF_DAY, horas <= 0 ? 72 : horas);
		return new SimpleDateFormat(FORMATO_VENCIMIENTO).format(c.getTime());
	}

	/** Resultado de revisar una direccion que llego. */
	public static final String OK = "OK";
	/** La direccion no trae firma: tiquete impreso antes del cambio, o alguien que la quito. */
	public static final String SIN_FIRMA = "SIN_FIRMA";
	/** La firma no corresponde: el numero de pedido se cambio a mano. */
	public static final String INVALIDA = "INVALIDA";
	/** La firma esta bien pero el QR ya vencio. */
	public static final String VENCIDA = "VENCIDA";
	/** No hay clave configurada en el central: no se puede opinar. */
	public static final String SIN_CLAVE = "SIN_CLAVE";

	/**
	 * Revisa una direccion que llego. NUNCA lanza: devuelve una de las constantes
	 * de arriba y es quien llama el que decide si rechaza o solo anota.
	 */
	public static String revisar(final int idTienda, final int idPedido, final String tipo,
			final String vencimiento, final String firmaRecibida, final String clave) {
		if (firmaRecibida == null || firmaRecibida.trim().isEmpty()
				|| vencimiento == null || vencimiento.trim().isEmpty()) {
			return SIN_FIRMA;
		}
		if (clave == null || clave.trim().isEmpty()) {
			return SIN_CLAVE;
		}
		final String esperada = firmar(idTienda, idPedido, tipo, vencimiento.trim(), clave);
		if (esperada.isEmpty() || !iguales(esperada, firmaRecibida.trim().toLowerCase())) {
			// Se revisa la firma ANTES que el vencimiento a proposito: si no cuadra,
			// decir "vencida" seria dar una pista de que el vencimiento si se podia
			// tocar, y ademas confundiria un tiquete viejo legitimo con uno forjado.
			return INVALIDA;
		}
		try {
			final Date vence = new SimpleDateFormat(FORMATO_VENCIMIENTO).parse(vencimiento.trim());
			if (vence.before(new Date())) {
				return VENCIDA;
			}
		} catch (final Exception e) {
			return INVALIDA;
		}
		return OK;
	}

	/**
	 * Compara sin delatar en cuantos caracteres se parecen. Una comparacion que
	 * sale al primer caracter distinto permite adivinar la firma de a poquitos
	 * midiendo cuanto tarda; esta siempre recorre todo.
	 */
	private static boolean iguales(final String a, final String b) {
		if (a == null || b == null || a.length() != b.length()) {
			return false;
		}
		int diferencia = 0;
		for (int i = 0; i < a.length(); i++) {
			diferencia |= a.charAt(i) ^ b.charAt(i);
		}
		return diferencia == 0;
	}
}
