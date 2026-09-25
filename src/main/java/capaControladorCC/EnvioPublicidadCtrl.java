package capaControladorCC;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.JsonObject;

import capaDAOCC.CampanaDAO;
import capaDAOCC.DefinicionSegmentoDAO;
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

	/**
	 * Los segmentos, leidos de crm.segmento_definicion.
	 *
	 * Estaban escritos a mano en el javascript de la pantalla y la lista quedo
	 * vieja: le faltaban ORO -7.971 personas, el mejor publico que hay- y CASI
	 * PERDIDO, y ofrecia UNICA COMPRA, que no existe. Un filtro que no ofrece un
	 * segmento no falla ni avisa: simplemente no se puede escoger, y nadie se
	 * entera de que le esta faltando.
	 *
	 * La definicion la mantiene la pantalla de Definicion de Segmentos, asi que
	 * aqui se lee y no se repite.
	 */
	@SuppressWarnings("unchecked")
	public static String segmentos() {
		final JSONArray lista = new JSONArray();
		for (final String nombre : DefinicionSegmentoDAO.nombresValidos()) {
			lista.add(nombre);
		}
		final JSONObject r = new JSONObject();
		r.put("segmentos", lista);
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

	/**
	 * Abre una tanda de una campana y la manda.
	 *
	 * La campana puede venir escogida del desplegable -idCampana- o ser nueva,
	 * en cuyo caso se crea con el nombre que se escribio. Si ya existe una con
	 * ese nombre se reusa esa en vez de fallar: quien escribe el mismo nombre
	 * dos veces quiere la misma campana, no un error.
	 *
	 * @param tope cuantos mandar en ESTA tanda; 0 es todo el publico
	 */
	@SuppressWarnings("unchecked")
	public static String crearYEnviar(final long idCampanaEscogida, final String nombre,
			final String canal, final int idPlantilla, final String asunto, final String cuerpo,
			final int tope, final String filtrosTexto,
			final SegmentacionPersonaDAO.Filtro filtro, final CampanaDAO.FiltroExtra extra,
			final String usuario) {
		final JSONObject r = new JSONObject();

		if (!CampanaDAO.CANAL_CORREO.equals(canal) && !CampanaDAO.CANAL_WHATSAPP.equals(canal)
				&& !CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			r.put("error", "Canal no valido.");
			return (r.toJSONString());
		}
		if (idCampanaEscogida <= 0 && (nombre == null || nombre.trim().length() == 0)) {
			r.put("error", "Escoja una campana o pongale nombre a la nueva."
					+ " Sin campana no se puede medir despues si sirvio.");
			return (r.toJSONString());
		}
		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			if (cuerpo == null || cuerpo.trim().length() == 0) {
				r.put("error", "El correo directo necesita el contenido del mensaje.");
				return (r.toJSONString());
			}
			final long enCurso = CampanaDAO.envioDirectoEnCurso();
			if (enCurso != 0) {
				r.put("error", "Ya hay un envio de correo directo en curso (el " + enCurso
						+ "). Espere a que termine.");
				return (r.toJSONString());
			}
		} else if (idPlantilla <= 0) {
			r.put("error", "Escoja la plantilla de Brevo.");
			return (r.toJSONString());
		}

		//El tope del correo directo lo pone el servidor aunque la pantalla pida
		//mas: es proteccion de la reputacion del dominio, no una preferencia.
		int topeReal = tope < 0 ? 0 : tope;
		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			final int techo = EnviadorPublicidadDirecta.tope();
			if (topeReal == 0 || topeReal > techo) {
				topeReal = techo;
			}
		}

		long idCampana = idCampanaEscogida;
		if (idCampana <= 0) {
			idCampana = CampanaDAO.buscarPorNombre(nombre.trim());
			if (idCampana == 0) {
				idCampana = CampanaDAO.crear(nombre.trim(), canal, idPlantilla, asunto, cuerpo,
						filtrosTexto, extra.diasSinPublicidad, topeReal, usuario);
			}
		}
		if (idCampana == 0) {
			r.put("error", "No se pudo crear la campana.");
			return (r.toJSONString());
		}

		//El maestro se queda con lo ultimo que se uso, para que la proxima vez
		//que se escoja en el desplegable vuelva todo puesto.
		CampanaDAO.actualizarMaestro(idCampana, canal, idPlantilla, asunto, cuerpo, filtrosTexto,
				extra.diasSinPublicidad, topeReal);

		final long idEnvio = CampanaDAO.crearEnvio(idCampana, canal, idPlantilla, asunto, cuerpo,
				filtrosTexto, topeReal, extra.diasSinPublicidad, usuario);
		if (idEnvio == 0) {
			r.put("error", "No se pudo abrir la tanda de envio.");
			return (r.toJSONString());
		}

		final int cuantos = CampanaDAO.cargarDestinatarios(idEnvio, idCampana, canal, filtro,
				extra, topeReal);

		if (cuantos == 0) {
			CampanaDAO.cambiarEstado(idEnvio, "CANCELADA");
			//Se nombra el descanso explicitamente. Con 30 dias por defecto, la
			//causa mas probable de "no quedo nadie" no es el filtro sino que a
			//esa gente ya se le escribio, y sin decirlo la pantalla parece rota.
			String porque = "Con ese filtro no quedo nadie a quien escribirle por ese canal.";
			if (extra.diasSinPublicidad > 0) {
				porque += " Ojo que esta pidiendo " + extra.diasSinPublicidad
						+ " dias de descanso: puede que ya les haya escrito y esten descansando."
						+ " Baje ese numero para verlos.";
			}
			r.put("error", porque);
			r.put("idenvio", idEnvio);
			return (r.toJSONString());
		}

		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			final String problema = EnviadorPublicidadDirecta.arrancar(idEnvio);
			if (problema.length() > 0) {
				r.put("error", problema);
				return (r.toJSONString());
			}
		} else {
			arrancarBrevo(idEnvio, canal, idPlantilla, asunto, usuario);
		}

		r.put("idenvio", idEnvio);
		r.put("idcampana", idCampana);
		r.put("publico", cuantos);
		r.put("canal", canal);
		r.put("mensaje", "En camino a " + cuantos + " personas.");
		return (r.toJSONString());
	}

	/**
	 * Suelta la campana por Brevo en un hilo aparte.
	 *
	 * Se marca lote por lote y no al final: si el servidor se cae a mitad, lo
	 * enviado queda marcado y al reanudar nadie recibe dos veces.
	 */
	private static void arrancarBrevo(final long idEnvio, final String canal,
			final int idPlantilla, final String asunto, final String usuario) {
		CampanaDAO.cambiarEstado(idEnvio, "ENVIANDO");
		final Thread hilo = new Thread(new Runnable() {
			public void run() {
				final Logger logger = Logger.getLogger("log_file");
				try {
					final SegmentacionClienteCtrl brevo = new SegmentacionClienteCtrl();
					final boolean porWhatsapp = CampanaDAO.CANAL_WHATSAPP.equals(canal);
					while (true) {
						final ArrayList<CampanaDAO.Destinatario> lote =
								CampanaDAO.pendientes(idEnvio, LOTE_BREVO);
						if (lote.isEmpty()) {
							break;
						}
						//Brevo rechaza el LOTE COMPLETO si una sola direccion
						//viene mal: catorce correos malos entre quinientos
						//tumbaron los quinientos. Se revisa cada uno antes de
						//meterlo, y el que no sirve se marca solo -con el motivo-
						//sin arrastrar a los otros noventa y nueve.
						final ArrayList<CampanaDAO.Destinatario> buenos =
								new ArrayList<CampanaDAO.Destinatario>();
						final java.util.List<JsonObject> destinos = new java.util.ArrayList<JsonObject>();
						for (int i = 0; i < lote.size(); i++) {
							final CampanaDAO.Destinatario d = lote.get(i);
							final String destino = d.destino == null ? "" : d.destino.trim();
							if (!sirve(destino, porWhatsapp)) {
								CampanaDAO.marcar(idEnvio, d.idPersona, "FALLIDO",
										(porWhatsapp ? "El celular no sirve: " : "La direccion no sirve: ")
										+ destino);
								continue;
							}
							final JsonObject j = new JsonObject();
							if (porWhatsapp) {
								j.addProperty("telefono", destino);
							} else {
								j.addProperty("name", d.nombre == null || d.nombre.length() == 0
										? "Cliente" : d.nombre);
								j.addProperty("email", destino);
							}
							buenos.add(d);
							destinos.add(j);
						}

						//Todo el lote era malo: ya quedaron marcados, y como
						//dejaron de estar pendientes la siguiente vuelta trae
						//los que siguen. No se repite.
						if (destinos.isEmpty()) {
							continue;
						}

						final JsonObject respuesta = llamar(brevo, porWhatsapp, destinos,
								asunto, idPlantilla);
						//Brevo responde por lote, no por persona. Marcar cada
						//uno por separado seria inventarse un detalle que la
						//respuesta no trae; se marca el lote con lo que dijo.
						final boolean bien = salioBien(respuesta);
						final String detalle = mensajeDe(respuesta);

						if (bien || buenos.size() == 1) {
							for (int i = 0; i < buenos.size(); i++) {
								CampanaDAO.marcar(idEnvio, buenos.get(i).idPersona,
										bien ? "ENVIADO" : "FALLIDO", detalle);
							}
						} else {
							//EL LOTE SE CAYO. Brevo lo rechaza COMPLETO por una
							//sola direccion mala, asi que dar por perdidas a las
							//cien seria regalar noventa y nueve clientes buenos
							//por culpa de uno. Se reintenta de a uno: cuesta cien
							//llamadas, pero solo cuando algo fallo, y deja dicho
							//exactamente cual fue la mala.
							logger.error("EnvioPublicidadCtrl: lote rechazado en el envio "
									+ idEnvio + " (" + detalle + "). Se reintenta de a uno.");
							for (int i = 0; i < buenos.size(); i++) {
								final java.util.List<JsonObject> uno =
										new java.util.ArrayList<JsonObject>();
								uno.add(destinos.get(i));
								final JsonObject r = llamar(brevo, porWhatsapp, uno, asunto,
										idPlantilla);
								CampanaDAO.marcar(idEnvio, buenos.get(i).idPersona,
										salioBien(r) ? "ENVIADO" : "FALLIDO", mensajeDe(r));
							}
						}
					}
					logger.info("EnvioPublicidadCtrl: campana " + idEnvio + " terminada.");
				} catch (final Throwable t) {
					logger.error("EnvioPublicidadCtrl: fallo la campana " + idEnvio + ", "
							+ t.toString());
				}
			}
		});
		hilo.setDaemon(true);
		hilo.setName("publicidad-" + idEnvio);
		hilo.start();
	}

	private static JsonObject llamar(final SegmentacionClienteCtrl brevo, final boolean porWhatsapp,
			final java.util.List<JsonObject> destinos, final String asunto, final int idPlantilla)
			throws java.io.IOException {
		if (porWhatsapp) {
			return (brevo.envioWhatsappBrevo(destinos, asunto, idPlantilla,
					new java.util.ArrayList<JsonObject>()));
		}
		return (brevo.envioCorreoBrevo(destinos, asunto, idPlantilla,
				new java.util.ArrayList<JsonObject>()));
	}

	private static boolean salioBien(final JsonObject respuesta) {
		return (respuesta != null && respuesta.has("success")
				&& respuesta.get("success").getAsBoolean());
	}

	private static String mensajeDe(final JsonObject respuesta) {
		return ((respuesta != null && respuesta.has("message"))
				? respuesta.get("message").getAsString() : "");
	}

	/**
	 * Si esta direccion la va a aceptar Brevo.
	 *
	 * La misma regla que usa CampanaDAO para cargar, repetida aqui a proposito:
	 * la de alla evita que entren, esta evita que salgan. Una campana cargada
	 * antes de este arreglo todavia tiene direcciones malas adentro, y sin esta
	 * segunda revision volveria a tumbar el lote entero.
	 *
	 * Por celular son diez digitos pelados: el +57 lo pone el codigo de Brevo.
	 */
	private static boolean sirve(final String destino, final boolean porWhatsapp) {
		if (destino == null || destino.length() == 0) {
			return (false);
		}
		if (porWhatsapp) {
			return (destino.matches(CampanaDAO.PATRON_CELULAR));
		}
		return (destino.matches(CampanaDAO.PATRON_CORREO));
	}

	// =======================================================================
	// Mirar como va
	// =======================================================================

	@SuppressWarnings("unchecked")
	public static String avance(final long idEnvio) {
		final JSONObject o = new JSONObject();
		final CampanaDAO.Envio c = CampanaDAO.obtener(idEnvio);
		if (c == null) {
			o.put("error", "No existe ese envio.");
			return (o.toJSONString());
		}
		o.put("idenvio", c.idEnvio);
		o.put("idcampana", c.idCampana);
		o.put("nombre", c.campana);
		o.put("consecutivo", c.consecutivo);
		o.put("canal", c.canal);
		o.put("estado", c.estado);
		o.put("publico", c.publico);
		o.put("enviados", c.enviados);
		o.put("fallidos", c.fallidos);
		o.put("pendientes", c.pendientes);
		o.put("creada_en", c.creadoEn);
		o.put("terminada_en", c.terminadoEn);
		if (CampanaDAO.CANAL_DIRECTO.equals(c.canal) && c.pendientes > 0) {
			//Para el correo directo el usuario necesita saber cuanto falta: con
			//30 segundos entre uno y otro, 50 correos son 25 minutos y sin este
			//dato la pantalla parece colgada.
			o.put("minutos_restantes",
					Math.round(c.pendientes * EnviadorPublicidadDirecta.segundosEntreCorreos() / 60.0));
		}
		return (o.toJSONString());
	}

	/** El historial: las ultimas tandas, con el nombre de su campana. */
	@SuppressWarnings("unchecked")
	public static String ultimas(final int cuantas) {
		final JSONArray lista = new JSONArray();
		for (final CampanaDAO.Envio e : CampanaDAO.ultimas(cuantas)) {
			final JSONObject o = new JSONObject();
			o.put("idenvio", e.idEnvio);
			o.put("idcampana", e.idCampana);
			o.put("nombre", e.campana);
			o.put("consecutivo", e.consecutivo);
			o.put("canal", e.canal);
			o.put("estado", e.estado);
			o.put("tope", e.tope);
			o.put("publico", e.publico);
			o.put("enviados", e.enviados);
			o.put("fallidos", e.fallidos);
			o.put("creada_en", e.creadoEn);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("campanas", lista);
		return (r.toJSONString());
	}

	/**
	 * El maestro, para el desplegable.
	 *
	 * Devuelve tambien lo que se uso la ultima vez -canal, plantilla, asunto,
	 * tope, dias- para que escoger una campana deje la pantalla lista y no haya
	 * que volver a armarla.
	 */
	@SuppressWarnings("unchecked")
	public static String campanas() {
		final JSONArray lista = new JSONArray();
		for (final CampanaDAO.Campana c : CampanaDAO.campanas(true)) {
			final JSONObject o = new JSONObject();
			o.put("idcampana", c.idCampana);
			o.put("nombre", c.nombre);
			o.put("canal", c.canal);
			o.put("idplantilla", c.idPlantilla);
			o.put("asunto", c.asunto);
			o.put("cuerpo", c.cuerpo);
			o.put("filtros", c.filtros);
			o.put("dias_sin_publicidad", c.diasSinPublicidad);
			o.put("tope", c.tope);
			o.put("envios", c.envios);
			o.put("publico", c.publico);
			o.put("enviados", c.enviados);
			o.put("ultimo_envio_en", c.ultimoEnvioEn);
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("campanas", lista);
		r.put("dias_defecto", CampanaDAO.diasMinimosPorDefecto());
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String resultado(final long id, final int horas, final boolean porCampana) {
		final CampanaDAO.Resultado res = porCampana
				? CampanaDAO.resultadoCampana(id, horas)
				: CampanaDAO.resultado(id, horas);
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
