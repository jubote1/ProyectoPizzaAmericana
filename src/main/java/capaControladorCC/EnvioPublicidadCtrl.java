package capaControladorCC;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.JsonObject;

import capaDAOCC.CampanaDAO;
import capaDAOCC.SegmentacionPersonaDAO;
import utilidadesCC.ControladorEnvioCorreo;
import utilidadesCC.EnviadorPublicidadDirecta;
import capaModeloCC.Correo;
import capaModeloCC.CorreoElectronico;

/**
 * El envio de publicidad: arma el publico, crea la campana y la manda.
 *
 * LOS TRES CANALES NO SON INTERCAMBIABLES
 *
 *   Brevo correo     hasta 231.354 personas. Es el canal masivo.
 *   Brevo WhatsApp   hasta 254.703. Llega a mas gente y se lee mucho antes,
 *                    que para algo del mismo dia es lo que decide.
 *   Correo directo   50 por tanda, uno cada 30 segundos. Es la salida de
 *                    emergencia, no un canal masivo.
 *
 * El correo directo sale de la cuenta propia de la empresa. Mandar masivo por
 * ahi quema la reputacion del dominio y despues no llegan ni las facturas. Por
 * eso el tope no es una limitacion tecnica sino una proteccion, y por eso el
 * servidor lo respeta aunque la pantalla pida mas.
 *
 * TODO VA EN SEGUNDO PLANO
 *
 * Una campana de decenas de miles no cabe en el tiempo de una peticion HTTP: se
 * caeria por timeout a la mitad y nadie sabria donde quedo. La pantalla dispara,
 * el servidor trabaja, y la pantalla pregunta el avance cada tanto.
 */
public class EnvioPublicidadCtrl {

	/**
	 * Cuantos destinatarios van en cada llamada a Brevo.
	 *
	 * No es un numero magico: uno por uno serian 231 mil llamadas HTTP, y todos
	 * de una sola vez es un cuerpo enorme que el proveedor rechaza. En lotes el
	 * fallo tambien queda acotado a un lote.
	 */
	private static final int LOTE_BREVO = 100;

	// =======================================================================
	// El publico
	// =======================================================================

	/**
	 * Las tiendas, para el selector.
	 *
	 * Va aqui y no reusando ConsultarSegmentacionPersona porque aquella corre
	 * la consulta completa del segmento para devolver, de paso, la lista de
	 * tiendas. Pedirle eso solo para llenar un desplegable es disparar un
	 * conteo sobre 450 mil personas cada vez que se abre la pantalla.
	 */
	@SuppressWarnings("unchecked")
	public static String tiendas() {
		final JSONArray lista = new JSONArray();
		for (final SegmentacionPersonaDAO.Tienda t : SegmentacionPersonaDAO.tiendas()) {
			final JSONObject o = new JSONObject();
			o.put("idtienda", t.idTienda);
			o.put("nombre", t.nombre);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("tiendas", lista);
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String alcance(final SegmentacionPersonaDAO.Filtro filtro,
			final CampanaDAO.FiltroExtra extra) {
		final CampanaDAO.Alcance a = CampanaDAO.alcance(filtro, extra);
		final JSONObject o = new JSONObject();
		o.put("personas", a.personas);
		o.put("con_correo", a.conCorreo);
		o.put("con_celular", a.conCelular);
		o.put("sin_consentimiento", a.sinConsentimiento);
		o.put("tope_directo", EnviadorPublicidadDirecta.tope());
		o.put("segundos_directo", EnviadorPublicidadDirecta.segundosEntreCorreos());
		return (o.toJSONString());
	}

	// =======================================================================
	// Crear y mandar
	// =======================================================================

	@SuppressWarnings("unchecked")
	public static String crearYEnviar(final String nombre, final String canal, final int idPlantilla,
			final String asunto, final String cuerpo, final String filtrosTexto,
			final SegmentacionPersonaDAO.Filtro filtro, final CampanaDAO.FiltroExtra extra,
			final String usuario) {
		final JSONObject r = new JSONObject();

		if (nombre == null || nombre.trim().length() == 0) {
			r.put("error", "La campana necesita un nombre. Sin el no se puede medir despues.");
			return (r.toJSONString());
		}
		if (!CampanaDAO.CANAL_CORREO.equals(canal) && !CampanaDAO.CANAL_WHATSAPP.equals(canal)
				&& !CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			r.put("error", "Canal no valido.");
			return (r.toJSONString());
		}
		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			if (cuerpo == null || cuerpo.trim().length() == 0) {
				r.put("error", "El correo directo necesita el contenido del mensaje.");
				return (r.toJSONString());
			}
			final long enCurso = CampanaDAO.campanaDirectaEnCurso();
			if (enCurso != 0) {
				r.put("error", "Ya hay una campana de correo directo en curso (la " + enCurso
						+ "). Espere a que termine.");
				return (r.toJSONString());
			}
		} else if (idPlantilla <= 0) {
			r.put("error", "Escoja la plantilla de Brevo.");
			return (r.toJSONString());
		}

		final long idCampana = CampanaDAO.crear(nombre.trim(), canal, idPlantilla, asunto, cuerpo,
				filtrosTexto, usuario);
		if (idCampana == 0) {
			r.put("error", "No se pudo crear la campana.");
			return (r.toJSONString());
		}

		//El tope solo aplica al correo directo. Los canales de Brevo van
		//completos: ahi el limite lo pone el plan contratado, no el dominio.
		final int tope = CampanaDAO.CANAL_DIRECTO.equals(canal)
				? EnviadorPublicidadDirecta.tope() : 0;
		final int cuantos = CampanaDAO.cargarDestinatarios(idCampana, canal, filtro, extra, tope);

		if (cuantos == 0) {
			CampanaDAO.cambiarEstado(idCampana, "CANCELADA");
			r.put("error", "Con ese filtro no quedo nadie a quien escribirle por ese canal.");
			r.put("idcampana", idCampana);
			return (r.toJSONString());
		}

		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			final String problema = EnviadorPublicidadDirecta.arrancar(idCampana);
			if (problema.length() > 0) {
				r.put("error", problema);
				return (r.toJSONString());
			}
		} else {
			arrancarBrevo(idCampana, canal, idPlantilla, asunto, usuario);
		}

		r.put("idcampana", idCampana);
		r.put("publico", cuantos);
		r.put("canal", canal);
		r.put("mensaje", "Campana " + idCampana + " en camino a " + cuantos + " personas.");
		return (r.toJSONString());
	}

	/**
	 * Suelta la campana por Brevo en un hilo aparte.
	 *
	 * Se marca lote por lote y no al final: si el servidor se cae a mitad, lo
	 * enviado queda marcado y al reanudar nadie recibe dos veces.
	 */
	private static void arrancarBrevo(final long idCampana, final String canal,
			final int idPlantilla, final String asunto, final String usuario) {
		CampanaDAO.cambiarEstado(idCampana, "ENVIANDO");
		final Thread hilo = new Thread(new Runnable() {
			public void run() {
				final Logger logger = Logger.getLogger("log_file");
				try {
					final SegmentacionClienteCtrl brevo = new SegmentacionClienteCtrl();
					final boolean porWhatsapp = CampanaDAO.CANAL_WHATSAPP.equals(canal);
					while (true) {
						final ArrayList<CampanaDAO.Destinatario> lote =
								CampanaDAO.pendientes(idCampana, LOTE_BREVO);
						if (lote.isEmpty()) {
							break;
						}
						final java.util.List<JsonObject> destinos = new java.util.ArrayList<JsonObject>();
						for (int i = 0; i < lote.size(); i++) {
							final CampanaDAO.Destinatario d = lote.get(i);
							final JsonObject j = new JsonObject();
							if (porWhatsapp) {
								j.addProperty("telefono", d.destino);
							} else {
								j.addProperty("name", d.nombre == null || d.nombre.length() == 0
										? "Cliente" : d.nombre);
								j.addProperty("email", d.destino);
							}
							destinos.add(j);
						}

						JsonObject respuesta;
						if (porWhatsapp) {
							respuesta = brevo.envioWhatsappBrevo(destinos, asunto, idPlantilla,
									new java.util.ArrayList<JsonObject>());
						} else {
							respuesta = brevo.envioCorreoBrevo(destinos, asunto, idPlantilla,
									new java.util.ArrayList<JsonObject>());
						}

						//Brevo responde por lote, no por persona. Marcar cada
						//uno por separado seria inventarse un detalle que la
						//respuesta no trae; se marca el lote con lo que dijo.
						final boolean bien = respuesta != null && respuesta.has("success")
								&& respuesta.get("success").getAsBoolean();
						final String detalle = (respuesta != null && respuesta.has("message"))
								? respuesta.get("message").getAsString() : "";
						for (int i = 0; i < lote.size(); i++) {
							CampanaDAO.marcar(idCampana, lote.get(i).idPersona,
									bien ? "ENVIADO" : "FALLIDO", detalle);
						}
						if (!bien) {
							logger.error("EnvioPublicidadCtrl: lote fallido en campana "
									+ idCampana + ", " + detalle);
						}
					}
					logger.info("EnvioPublicidadCtrl: campana " + idCampana + " terminada.");
				} catch (final Throwable t) {
					logger.error("EnvioPublicidadCtrl: fallo la campana " + idCampana + ", "
							+ t.toString());
				}
			}
		});
		hilo.setDaemon(true);
		hilo.setName("publicidad-" + idCampana);
		hilo.start();
	}

	// =======================================================================
	// Mirar como va
	// =======================================================================

	@SuppressWarnings("unchecked")
	public static String avance(final long idCampana) {
		final JSONObject o = new JSONObject();
		final CampanaDAO.Campana c = CampanaDAO.obtener(idCampana);
		if (c == null) {
			o.put("error", "No existe esa campana.");
			return (o.toJSONString());
		}
		o.put("idcampana", c.idCampana);
		o.put("nombre", c.nombre);
		o.put("canal", c.canal);
		o.put("estado", c.estado);
		o.put("publico", c.publico);
		o.put("enviados", c.enviados);
		o.put("fallidos", c.fallidos);
		o.put("pendientes", c.pendientes);
		o.put("creada_en", c.creadaEn);
		o.put("terminada_en", c.terminadaEn);
		if (CampanaDAO.CANAL_DIRECTO.equals(c.canal) && c.pendientes > 0) {
			//Para el correo directo el usuario necesita saber cuanto falta: con
			//30 segundos entre uno y otro, 50 correos son 25 minutos y sin este
			//dato la pantalla parece colgada.
			o.put("minutos_restantes",
					Math.round(c.pendientes * EnviadorPublicidadDirecta.segundosEntreCorreos() / 60.0));
		}
		return (o.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String ultimas(final int cuantas) {
		final JSONArray lista = new JSONArray();
		for (final CampanaDAO.Campana c : CampanaDAO.ultimas(cuantas)) {
			final JSONObject o = new JSONObject();
			o.put("idcampana", c.idCampana);
			o.put("nombre", c.nombre);
			o.put("canal", c.canal);
			o.put("estado", c.estado);
			o.put("publico", c.publico);
			o.put("enviados", c.enviados);
			o.put("fallidos", c.fallidos);
			o.put("creada_en", c.creadaEn);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("campanas", lista);
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String resultado(final long idCampana, final int horas) {
		final CampanaDAO.Resultado res = CampanaDAO.resultado(idCampana, horas);
		final JSONObject o = new JSONObject();
		o.put("enviados", res.enviados);
		o.put("compraron", res.compraron);
		o.put("valor", res.valor);
		o.put("horas", res.horas);
		o.put("porcentaje", res.enviados > 0
				? Math.round(res.compraron * 1000.0 / res.enviados) / 10.0 : 0);
		//Se dice explicitamente para que nadie lo lea como causa: que alguien
		//compre despues de recibir no prueba que compro POR el mensaje.
		o.put("advertencia", "Mide quien compro despues de recibir, no quien compro por recibir."
				+ " Sirve para comparar campanas entre si.");
		return (o.toJSONString());
	}

	// =======================================================================
	// La prueba antes de disparar
	// =======================================================================

	/**
	 * Manda UNO a una direccion de prueba.
	 *
	 * Vale lo que cuesta: es lo que habria mostrado que la imagen del combo
	 * futbolero pesaba 1,86 MB y llegaba lentisima. Un boton barato que evita
	 * mandarle algo roto a doscientas mil personas.
	 */
	@SuppressWarnings("unchecked")
	public static String probar(final String canal, final String destino, final int idPlantilla,
			final String asunto, final String cuerpo) {
		final JSONObject r = new JSONObject();
		if (destino == null || destino.trim().length() == 0) {
			r.put("error", "Escriba a donde quiere la prueba.");
			return (r.toJSONString());
		}
		try {
			if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
				if (!ControladorEnvioCorreo.esDireccionValida(destino)) {
					r.put("error", "Esa direccion de correo no es valida.");
					return (r.toJSONString());
				}
				final CorreoElectronico cuenta = ControladorEnvioCorreo.recuperarCorreo(
						"CUENTACORREOREPORTES", "CLAVECORREOREPORTE");
				final Correo correo = new Correo();
				correo.setAsunto("[PRUEBA] " + (asunto == null ? "" : asunto));
				correo.setUsuarioCorreo(cuenta.getCuentaCorreo());
				correo.setContrasena(cuenta.getClaveCorreo());
				correo.setMensaje(cuerpo == null ? "" : cuerpo.replace("{{nombre}}", "Prueba"));
				final ArrayList<String> destinos = new ArrayList<String>();
				destinos.add(destino.trim());
				final ControladorEnvioCorreo envio = new ControladorEnvioCorreo(correo, destinos);
				final ControladorEnvioCorreo.ResultadoEnvio res = envio.enviarCorreoClasificado();
				r.put("exito", res == ControladorEnvioCorreo.ResultadoEnvio.ENVIADO);
				r.put("detalle", String.valueOf(res));
			} else {
				final SegmentacionClienteCtrl brevo = new SegmentacionClienteCtrl();
				final java.util.List<JsonObject> uno = new java.util.ArrayList<JsonObject>();
				final JsonObject j = new JsonObject();
				JsonObject respuesta;
				if (CampanaDAO.CANAL_WHATSAPP.equals(canal)) {
					j.addProperty("telefono", destino.trim());
					uno.add(j);
					respuesta = brevo.envioWhatsappBrevo(uno, asunto, idPlantilla,
							new java.util.ArrayList<JsonObject>());
				} else {
					j.addProperty("name", "Prueba");
					j.addProperty("email", destino.trim());
					uno.add(j);
					respuesta = brevo.envioCorreoBrevo(uno, asunto, idPlantilla,
							new java.util.ArrayList<JsonObject>());
				}
				final boolean bien = respuesta != null && respuesta.has("success")
						&& respuesta.get("success").getAsBoolean();
				r.put("exito", bien);
				r.put("detalle", (respuesta != null && respuesta.has("message"))
						? respuesta.get("message").getAsString() : "");
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnvioPublicidadCtrl.probar: " + e.toString());
			r.put("error", "No se pudo enviar la prueba: " + e.toString());
		}
		return (r.toJSONString());
	}

	public static String detener() {
		EnviadorPublicidadDirecta.detener();
		final JSONObject r = new JSONObject();
		r.put("detenida", true);
		return (r.toJSONString());
	}
}
