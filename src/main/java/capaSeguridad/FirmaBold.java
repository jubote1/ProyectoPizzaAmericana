package capaSeguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Firma de los webhooks de Bold (encabezado x-bold-signature).
 *
 * Segun la documentacion de Bold: se codifica el cuerpo en Base64, se le aplica
 * HMAC-SHA256 con la llave secreta y el resultado, en hexadecimal, es la firma.
 * Como esa descripcion es lo unico que hay para validar, tambien se prueba la
 * variante sobre el cuerpo crudo (sin Base64) y se reporta cual coincidio, para
 * saber con eventos reales cual es la correcta. Ambas exigen la llave secreta:
 * ninguna se puede falsificar sin ella.
 */
public class FirmaBold {

	public static final String OK = "OK";
	/** Coincidio la variante HMAC sobre el cuerpo crudo, no la de Base64 que dice la documentacion. */
	public static final String OK_CUERPO_CRUDO = "OK_CUERPO_CRUDO";
	public static final String NO_COINCIDE = "NO_COINCIDE";
	public static final String SIN_FIRMA = "SIN_FIRMA";
	/** No hay llave secreta configurada: no se puede validar nada. Una llave vacia no autentica a nadie. */
	public static final String SIN_LLAVE = "SIN_LLAVE";

	/** HMAC-SHA256 de {@code texto} con {@code llave}, en hexadecimal minuscula. */
	static String hmacHex(final byte[] texto, final String llave) throws Exception {
		final Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(llave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		final byte[] resultado = mac.doFinal(texto);
		final StringBuilder sb = new StringBuilder();
		for (final byte b : resultado) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * @param cuerpo    el cuerpo exacto que llego, en bytes
	 * @param firma     el valor del encabezado x-bold-signature
	 * @param llaveSecreta la llave secreta de Bold
	 * @return uno de OK, OK_CUERPO_CRUDO, NO_COINCIDE, SIN_FIRMA o SIN_LLAVE
	 */
	public static String validar(final byte[] cuerpo, final String firma, final String llaveSecreta) {
		if (llaveSecreta == null || llaveSecreta.trim().isEmpty()) {
			return SIN_LLAVE;
		}
		if (firma == null || firma.trim().isEmpty()) {
			return SIN_FIRMA;
		}
		try {
			final byte[] recibida = firma.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
			final String llave = llaveSecreta.trim();
			final String base64 = Base64.getEncoder().encodeToString(cuerpo);
			final String segunDocumentacion = hmacHex(base64.getBytes(StandardCharsets.UTF_8), llave);
			if (MessageDigest.isEqual(recibida, segunDocumentacion.getBytes(StandardCharsets.UTF_8))) {
				return OK;
			}
			final String sobreCuerpoCrudo = hmacHex(cuerpo, llave);
			if (MessageDigest.isEqual(recibida, sobreCuerpoCrudo.getBytes(StandardCharsets.UTF_8))) {
				return OK_CUERPO_CRUDO;
			}
			return NO_COINCIDE;
		} catch (final Exception e) {
			return NO_COINCIDE;
		}
	}

	public static boolean esValida(final String resultado) {
		return OK.equals(resultado) || OK_CUERPO_CRUDO.equals(resultado);
	}

	/** SHA-256 hexadecimal del cuerpo: identifica de forma unica un evento, para no guardar dos veces un reintento. */
	public static String sha256Hex(final byte[] cuerpo) {
		try {
			final byte[] resumen = MessageDigest.getInstance("SHA-256").digest(cuerpo);
			final StringBuilder sb = new StringBuilder();
			for (final byte b : resumen) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
