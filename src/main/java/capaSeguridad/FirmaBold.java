package capaSeguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Firma de los webhooks de Bold (encabezado x-bold-signature).
 *
 * Segun la documentacion de Bold: se codifica el cuerpo en Base64, se le aplica
 * HMAC-SHA256 con la llave secreta y el resultado, en hexadecimal, es la firma.
 * Pero la documentacion no aclara SOBRE QUE cuerpo se calcula (el que llega tal
 * cual o el JSON re-serializado), y con eventos reales el primer intento no
 * coincidio. Por eso se prueban las variantes razonables y se reporta cual
 * coincidio, para saber con eventos reales cual es la correcta y dejar una sola.
 *
 * Todas las variantes que se aceptan exigen la llave secreta: ninguna se puede
 * falsificar sin ella. Las que solo sirven para diagnosticar (firmado con la
 * llave vacia o con la llave de identidad) NO se aceptan como validas: dicen por
 * que falla, no dejan pasar nada.
 */
public class FirmaBold {

	/** La variante de la documentacion: HMAC de Base64(cuerpo tal cual llego). */
	public static final String OK = "OK";
	public static final String OK_CUERPO_CRUDO = "OK_CUERPO_CRUDO";
	public static final String OK_JSON_COMPACTO_B64 = "OK_JSON_COMPACTO_B64";
	public static final String OK_JSON_COMPACTO = "OK_JSON_COMPACTO";
	public static final String NO_COINCIDE = "NO_COINCIDE";
	public static final String SIN_FIRMA = "SIN_FIRMA";
	/** No hay llave secreta configurada: no se puede validar nada. */
	public static final String SIN_LLAVE = "SIN_LLAVE";
	/** Diagnostico: la firma coincide con una llave VACIA (Bold firma asi en su ambiente de pruebas). No es valida. */
	public static final String DIAG_LLAVE_VACIA = "DIAG_LLAVE_VACIA";
	/** Diagnostico: la firma coincide con la llave de IDENTIDAD, o sea que se guardo la llave equivocada. No es valida. */
	public static final String DIAG_LLAVE_IDENTIDAD = "DIAG_LLAVE_IDENTIDAD";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** HMAC-SHA256 de {@code texto} con {@code llave}, en bytes. Una llave vacia se trata como una de un byte cero (equivalente en HMAC). */
	static byte[] hmac(final byte[] texto, final byte[] llave) throws Exception {
		final Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(llave.length == 0 ? new byte[] { 0 } : llave, "HmacSHA256"));
		return mac.doFinal(texto);
	}

	static String hex(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		for (final byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/** Los mensajes candidatos a estar firmados, con el nombre de la variante que corresponde a cada uno. */
	private static List<Object[]> candidatos(final byte[] cuerpo) {
		final List<Object[]> lista = new ArrayList<Object[]>();
		lista.add(new Object[] { OK, Base64.getEncoder().encodeToString(cuerpo).getBytes(StandardCharsets.UTF_8) });
		lista.add(new Object[] { OK_CUERPO_CRUDO, cuerpo });
		try {
			final byte[] compacto = MAPPER.writeValueAsBytes(MAPPER.readTree(cuerpo));
			lista.add(new Object[] { OK_JSON_COMPACTO_B64,
					Base64.getEncoder().encodeToString(compacto).getBytes(StandardCharsets.UTF_8) });
			lista.add(new Object[] { OK_JSON_COMPACTO, compacto });
		} catch (final Exception noEsJson) {
			// Sin JSON legible solo quedan las variantes sobre el cuerpo tal cual.
		}
		return lista;
	}

	/** Verdadero si {@code firma} (hex o Base64, con o sin el prefijo sha256=) es la de esos bytes. */
	private static boolean coincide(final String firma, final byte[] digest) {
		String recibida = firma.trim();
		if (recibida.toLowerCase().startsWith("sha256=")) {
			recibida = recibida.substring(7);
		}
		final byte[] enHex = hex(digest).getBytes(StandardCharsets.UTF_8);
		if (MessageDigest.isEqual(recibida.toLowerCase().getBytes(StandardCharsets.UTF_8), enHex)) {
			return true;
		}
		final byte[] enBase64 = Base64.getEncoder().encodeToString(digest).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(recibida.getBytes(StandardCharsets.UTF_8), enBase64);
	}

	/** Compatibilidad: sin la llave de identidad no hay diagnostico de llave equivocada. */
	public static String validar(final byte[] cuerpo, final String firma, final String llaveSecreta) {
		return validar(cuerpo, firma, llaveSecreta, null);
	}

	/**
	 * @param cuerpo        el cuerpo exacto que llego, en bytes
	 * @param firma         el valor del encabezado x-bold-signature
	 * @param llaveSecreta  la llave secreta de Bold
	 * @param llaveIdentidad la llave de identidad (solo para diagnosticar si se guardo la equivocada); puede ser null
	 * @return una de las constantes de esta clase
	 */
	public static String validar(final byte[] cuerpo, final String firma, final String llaveSecreta,
			final String llaveIdentidad) {
		if (llaveSecreta == null || llaveSecreta.trim().isEmpty()) {
			return SIN_LLAVE;
		}
		if (firma == null || firma.trim().isEmpty()) {
			return SIN_FIRMA;
		}
		try {
			final byte[] secreta = llaveSecreta.trim().getBytes(StandardCharsets.UTF_8);
			final List<Object[]> candidatos = candidatos(cuerpo);
			for (final Object[] c : candidatos) {
				if (coincide(firma, hmac((byte[]) c[1], secreta))) {
					return (String) c[0];
				}
			}
			// Nada coincidio con la llave guardada: se mira si es por una llave equivocada.
			for (final Object[] c : candidatos) {
				if (coincide(firma, hmac((byte[]) c[1], new byte[0]))) {
					return DIAG_LLAVE_VACIA;
				}
			}
			if (llaveIdentidad != null && !llaveIdentidad.trim().isEmpty()) {
				final byte[] identidad = llaveIdentidad.trim().getBytes(StandardCharsets.UTF_8);
				for (final Object[] c : candidatos) {
					if (coincide(firma, hmac((byte[]) c[1], identidad))) {
						return DIAG_LLAVE_IDENTIDAD;
					}
				}
			}
			return NO_COINCIDE;
		} catch (final Exception e) {
			return NO_COINCIDE;
		}
	}

	/** Solo las variantes que exigieron la llave secreta son validas. */
	public static boolean esValida(final String resultado) {
		return OK.equals(resultado) || OK_CUERPO_CRUDO.equals(resultado) || OK_JSON_COMPACTO_B64.equals(resultado)
				|| OK_JSON_COMPACTO.equals(resultado);
	}

	/** SHA-256 hexadecimal del cuerpo: identifica de forma unica un evento, para no guardar dos veces un reintento. */
	public static String sha256Hex(final byte[] cuerpo) {
		try {
			return hex(MessageDigest.getInstance("SHA-256").digest(cuerpo));
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
