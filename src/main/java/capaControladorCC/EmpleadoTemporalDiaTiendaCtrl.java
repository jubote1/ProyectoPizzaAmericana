package capaControladorCC;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import capaDAOCC.EmpleadoTemporalDiaTiendaDAO;
import capaDAOCC.EmpleadoTemporalDiaTiendaDAO.Registro;

/**
 * Recibe de una tienda el ingreso o la salida de un domiciliario temporal y lo deja en el central
 * (general.empleado_temporal_dia_tienda), para que el mapa de domiciliarios sepa si esta dentro.
 *
 * Valida y limpia lo que llega -el servicio lo llaman los POS, pero esta en internet- y deja la
 * decision de que estado manda en la version que trae cada envio (ver EmpleadoTemporalDiaTiendaDAO).
 *
 * Respuestas: OK (aplicado), VIEJO (llego un estado mas viejo que el que ya habia: se descarta, y para
 * la tienda cuenta como enviado), INVALIDO (faltan datos: reintentarlo igual no sirve) y ERROR (el
 * central no pudo guardarlo: la tienda debe reintentar).
 */
public class EmpleadoTemporalDiaTiendaCtrl {

	@SuppressWarnings("unchecked")
	public static String registrar(final String cuerpo) {
		final JSONObject respuesta = new JSONObject();
		final Registro r = new Registro();
		try {
			final Object parseado = new JSONParser().parse(cuerpo == null ? "" : cuerpo);
			final JSONObject j = (JSONObject) parseado;
			r.idTienda = entero(j.get("idtienda"));
			r.idInterno = entero(j.get("idinterno"));
			r.id = entero(j.get("id"));
			r.identificacion = texto(j.get("identificacion"), 20);
			r.nombre = texto(j.get("nombre"), 100);
			r.telefono = texto(j.get("telefono"), 20);
			r.empresa = texto(j.get("empresa"), 50);
			r.idEmpresa = j.get("idempresa") == null ? null : Integer.valueOf(entero(j.get("idempresa")));
			r.fechaSistema = texto(j.get("fechasistema"), 10);
			r.horaIngreso = texto(j.get("horaingreso"), 10);
			r.horaSalida = texto(j.get("horasalida"), 10);
			r.observacion = texto(j.get("observacion"), 200);
			r.anulado = "S".equalsIgnoreCase(texto(j.get("anulado"), 1));
			r.version = largo(j.get("version"));
		} catch (final Exception e) {
			respuesta.put("respuesta", "INVALIDO");
			respuesta.put("mensaje", "El cuerpo no es un JSON valido.");
			return respuesta.toJSONString();
		}
		if (r.idTienda <= 0 || r.idInterno <= 0 || r.identificacion.length() == 0
				|| !r.fechaSistema.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$") || r.version <= 0) {
			respuesta.put("respuesta", "INVALIDO");
			respuesta.put("mensaje", "Faltan datos: idtienda, idinterno, identificacion, fechasistema (aaaa-mm-dd) y version.");
			return respuesta.toJSONString();
		}
		final int resultado = EmpleadoTemporalDiaTiendaDAO.registrar(r);
		if (resultado == EmpleadoTemporalDiaTiendaDAO.APLICADO) {
			respuesta.put("respuesta", "OK");
		} else if (resultado == EmpleadoTemporalDiaTiendaDAO.DESCARTADO_POR_VIEJO) {
			respuesta.put("respuesta", "VIEJO");
		} else {
			respuesta.put("respuesta", "ERROR");
			respuesta.put("mensaje", "El central no pudo guardar el registro.");
		}
		return respuesta.toJSONString();
	}

	private static String texto(final Object valor, final int maximo) {
		if (valor == null) {
			return "";
		}
		final String t = valor.toString().trim();
		return t.length() > maximo ? t.substring(0, maximo) : t;
	}

	private static int entero(final Object valor) {
		try {
			return valor == null ? 0 : Integer.parseInt(valor.toString().trim());
		} catch (final Exception e) {
			return 0;
		}
	}

	private static long largo(final Object valor) {
		try {
			return valor == null ? 0 : Long.parseLong(valor.toString().trim());
		} catch (final Exception e) {
			return 0;
		}
	}
}
