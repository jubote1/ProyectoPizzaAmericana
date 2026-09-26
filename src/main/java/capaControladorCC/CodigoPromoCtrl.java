package capaControladorCC;

import org.json.simple.JSONObject;

import capaDAOCC.CodigoPromoDAO;
import capaDAOCC.ParametrosDAO;

/**
 * Capa de negocio del servicio CodigoPromocional: arma las respuestas JSON de reservar, confirmar y
 * liberar un codigo (ver capaDAOCC.CodigoPromoDAO para la logica) y decide si quien llama esta
 * autorizado.
 *
 * AUTORIZACION
 *
 * Si el parametro general.parametros.CODIGOSTOKEN tiene valor, cada llamada tiene que traer ese mismo
 * valor en el encabezado X-Token-Tienda. Si esta vacio no se exige nada, para poder desplegar primero
 * el central y despues el POS sin que las tiendas se queden sin poder redimir. El mismo esquema del
 * token de los eventos de Bold: se prende cuando todas las tiendas ya lo tienen puesto.
 */
public class CodigoPromoCtrl {

	private static final long VIGENCIA_TOKEN_MS = 60L * 1000L;
	private static String tokenEnCache = "";
	private static long tokenLeidoEn = 0;

	/** El token que deben traer las tiendas; "" si no se exige. Se relee cada minuto. */
	private static synchronized String tokenExigido() {
		final long ahora = System.currentTimeMillis();
		if (ahora - tokenLeidoEn > VIGENCIA_TOKEN_MS) {
			final String v = ParametrosDAO.retornarValorAlfanumerico("CODIGOSTOKEN");
			tokenEnCache = v == null ? "" : v.trim();
			tokenLeidoEn = ahora;
		}
		return tokenEnCache;
	}

	/** true si la llamada puede seguir. */
	public static boolean autorizado(final String tokenRecibido) {
		final String exigido = tokenExigido();
		return exigido.length() == 0 || exigido.equals(tokenRecibido == null ? "" : tokenRecibido.trim());
	}

	@SuppressWarnings("unchecked")
	public static String reservar(final String codigo, final int idTienda, final String usuario, final double total,
			final boolean esDomicilio, final String tokenPrevio) {
		final CodigoPromoDAO.Reserva r = CodigoPromoDAO.reservar(codigo, idTienda, usuario, total, esDomicilio,
				tokenPrevio);
		final JSONObject j = new JSONObject();
		j.put("respuesta", r.estado);
		j.put("mensaje", r.mensaje);
		j.put("token", r.token);
		j.put("descuentopesos", Double.valueOf(r.descuento));
		j.put("saldoposterior", Double.valueOf(r.saldoPosterior));
		if (r.codigo != null) {
			j.put("codigo", r.codigo.codigo);
			j.put("idoferta", Integer.valueOf(r.codigo.idOferta));
			j.put("idofertacliente", Integer.valueOf(r.codigo.idOfertaCliente));
			j.put("nombreoferta", r.codigo.nombreOferta);
			j.put("tipooferta", r.codigo.abierto ? "A" : "C");
			j.put("redparcial", r.codigo.redParcial);
			j.put("fechacaducidad", r.codigo.fechaCaducidad);
		}
		return j.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String confirmar(final String token, final int idPedido, final int idTienda, final String usuario,
			final double descuentoAplicado, final String origen) {
		final CodigoPromoDAO.Confirmacion c = CodigoPromoDAO.confirmar(token, idPedido, idTienda, usuario,
				descuentoAplicado, origen == null || origen.length() == 0 ? "POS" : origen);
		final JSONObject j = new JSONObject();
		j.put("respuesta", c.estado);
		j.put("mensaje", c.mensaje);
		j.put("saldoposterior", Double.valueOf(c.saldoPosterior));
		return j.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String liberar(final String token) {
		final boolean ok = CodigoPromoDAO.liberar(token);
		final JSONObject j = new JSONObject();
		j.put("respuesta", ok ? "OK" : "ERROR");
		return j.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String sinAutorizacion() {
		final JSONObject j = new JSONObject();
		j.put("respuesta", "NOAUTH");
		j.put("mensaje", "Esta tienda no esta autorizada para redimir codigos.");
		return j.toJSONString();
	}

	@SuppressWarnings("unchecked")
	public static String bloqueado() {
		final JSONObject j = new JSONObject();
		j.put("respuesta", "BLOQUEADO");
		j.put("mensaje", "Demasiados intentos con codigos que no existen. Intente de nuevo en unos minutos.");
		return j.toJSONString();
	}
}
