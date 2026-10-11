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

	/**
	 * Las especialidades favoritas, para el filtro de gusto.
	 *
	 * Van con el conteo porque la lista sola engana: hay familias con tres
	 * personas. El numero "claras" es el publico real al que se le puede decir
	 * "sabemos que te gusta".
	 */
	@SuppressWarnings("unchecked")
	public static String familias() {
		final JSONArray lista = new JSONArray();
		for (final SegmentacionPersonaDAO.Familia x : SegmentacionPersonaDAO.familias()) {
			final JSONObject j = new JSONObject();
			j.put("familia", x.nombre);
			j.put("personas", Integer.valueOf(x.personas));
			j.put("claras", Integer.valueOf(x.claras));
			lista.add(j);
		}
		final JSONObject r = new JSONObject();
		r.put("familias", lista);
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
			final int tope, final int esperados, final String filtrosTexto,
			final SegmentacionPersonaDAO.Filtro filtro, final CampanaDAO.FiltroExtra extra,
			final String usuario, final int idOferta) {
		final JSONObject r = new JSONObject();

		//Lo que se puede decir sin tocar nada: canal, campana, oferta, contenido. Es el mismo
		//chequeo que hace programar() al guardar un envio para mas tarde.
		final String problemaEstatico = problemaDeEnvio(canal, idCampanaEscogida, nombre, idPlantilla,
				cuerpo, idOferta);
		if (problemaEstatico.length() > 0) {
			r.put("error", problemaEstatico);
			return (r.toJSONString());
		}
		//Lo que depende del momento: el correo directo sale de a uno desde un solo reloj, y no
		//caben dos tandas a la vez.
		if (idOferta > 0 || CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			final long enCurso = CampanaDAO.envioDirectoEnCurso();
			if (enCurso != 0) {
				r.put("error", "Ya hay un envio de correo directo en curso (el " + enCurso
						+ "). Espere a que termine.");
				return (r.toJSONString());
			}
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
		if (idOferta <= 0) {
			CampanaDAO.actualizarMaestro(idCampana, canal, idPlantilla, asunto, cuerpo, filtrosTexto,
					extra.diasSinPublicidad, topeReal);
		}

		final long idEnvio = CampanaDAO.crearEnvio(idCampana, canal, idPlantilla,
				idOferta > 0 ? "Correo de la oferta " + idOferta : asunto, idOferta > 0 ? "" : cuerpo,
				filtrosTexto, topeReal, extra.diasSinPublicidad, usuario);
		if (idEnvio == 0) {
			r.put("error", "No se pudo abrir la tanda de envio.");
			return (r.toJSONString());
		}
		if (idOferta > 0) {
			CampanaDAO.asignarOferta(idEnvio, idOferta);
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

		//LA RED. La pantalla dice a cuantos conto; si al cargar salen muchos
		//mas, algo se perdio entre contar y enviar y NO se manda nada.
		//
		//No es hipotetico: el 2026-09-25 el filtro de "solo mostrador" se
		//perdia al enviar -el canal de envio le pisaba el nombre al de venta-,
		//la pantalla conto 53 personas y la campana salio a 3.135. Sin esta
		//comparacion, un filtro que se pierda no se nota hasta despues.
		//
		//Se tolera que crezca un poco: entre contar y enviar alguien pudo
		//comprar o cambiar de segmento. Lo que no se tolera es que se dispare.
		if (esperados > 0 && cuantos > esperados + Math.max(25, esperados / 10)) {
			CampanaDAO.cambiarEstado(idEnvio, "CANCELADA");
			r.put("error", "NO SE ENVIO NADA. La pantalla conto " + esperados
					+ " personas y al cargar la lista salieron " + cuantos
					+ ". Como no coinciden, algo se perdio entre contar y enviar."
					+ " Vuelva a contar y revise los filtros antes de insistir.");
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
		o.put("idoferta", c.idOferta);
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
			o.put("idoferta", e.idOferta);
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
		if (!porCampana) {
			//Una tanda con oferta si se puede medir con causa: el codigo es unico por persona.
			final CampanaDAO.Embudo b = CampanaDAO.embudo(id);
			if (b.disponible && b.emitidos > 0) {
				final JSONObject e = new JSONObject();
				e.put("enviados", b.enviados);
				e.put("codigos", b.emitidos);
				e.put("anulados", b.anulados);
				e.put("usados", b.usados);
				e.put("porcentaje_uso", b.enviados > 0 ? Math.round(b.usados * 1000.0 / b.enviados) / 10.0 : 0);
				e.put("descontado", b.descontado);
				e.put("ventas", b.ventas);
				o.put("embudo", e);
			}
		}
		return (o.toJSONString());
	}

	/** Las ofertas que se pueden mandar en una campana, para el selector de la pantalla. */
	@SuppressWarnings("unchecked")
	public static String ofertas() {
		final JSONArray lista = new JSONArray();
		for (final capaDAOCC.CodigoPromoDAO.OfertaEnviable e : capaDAOCC.CodigoPromoDAO.ofertasEnviables()) {
			final JSONObject o = new JSONObject();
			o.put("idoferta", e.idOferta);
			o.put("nombre", e.nombre);
			o.put("valor", e.valor);
			o.put("porcentaje", e.porcentaje);
			o.put("dias", e.dias);
			o.put("max_emision", e.maxEmision);
			o.put("emitidos", e.emitidos);
			o.put("saldo", "S".equals(e.redParcial));
			lista.add(o);
		}
		final JSONObject r = new JSONObject();
		r.put("ofertas", lista);
		return (r.toJSONString());
	}

	/** Anula los codigos sin usar de una tanda con oferta (se mando la oferta equivocada, o con un error). */
	@SuppressWarnings("unchecked")
	public static String anularCodigos(final long idEnvio, final String usuario) {
		final JSONObject o = new JSONObject();
		if (CampanaDAO.envioDirectoEnCurso() == idEnvio) {
			o.put("error", "El envio sigue en curso: detengalo primero.");
			return (o.toJSONString());
		}
		final int n = capaDAOCC.CodigoPromoDAO.anularEnvio(idEnvio, usuario);
		if (n < 0) {
			o.put("error", "No se pudieron anular los codigos.");
		} else {
			o.put("anulados", n);
			o.put("mensaje", n + " codigos sin usar quedaron anulados. Los que ya se usaron no se tocan.");
		}
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
				correo.setAsunto("[PRUEBA] " + (asunto == null ? "" : asunto.replace("{{nombre}}", "Prueba")));
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

	// =======================================================================
	// Validar, y armar filtros desde un mapa de parametros
	// =======================================================================

	/**
	 * Lo que se puede decir sobre un envio sin tocar nada: canal valido, campana o nombre, oferta
	 * vigente, contenido. "" si esta bien.
	 *
	 * Lo usan el envio inmediato y la programacion, para que un envio programado no pueda guardarse
	 * con algo que el inmediato habria rechazado.
	 */
	private static String problemaDeEnvio(final String canal, final long idCampanaEscogida,
			final String nombre, final int idPlantilla, final String cuerpo, final int idOferta) {
		if (!CampanaDAO.CANAL_CORREO.equals(canal) && !CampanaDAO.CANAL_WHATSAPP.equals(canal)
				&& !CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			return ("Canal no valido.");
		}
		if (idCampanaEscogida <= 0 && (nombre == null || nombre.trim().length() == 0)) {
			return ("Escoja una campana o pongale nombre a la nueva."
					+ " Sin campana no se puede medir despues si sirvio.");
		}
		if (idOferta > 0) {
			//Con oferta cada persona recibe su propio codigo dentro del correo de la oferta, y ese correo sale por el
			//correo directo: Brevo con una plantilla que lleve el codigo por destinatario queda para una segunda etapa.
			if (!CampanaDAO.CANAL_DIRECTO.equals(canal)) {
				return ("El envio con oferta sale por correo directo, con el correo de la oferta.");
			}
			return (capaDAOCC.CodigoPromoDAO.problemaParaEnviar(idOferta));
		}
		if (CampanaDAO.CANAL_DIRECTO.equals(canal)) {
			if (cuerpo == null || cuerpo.trim().length() == 0) {
				return ("El correo directo necesita el contenido del mensaje.");
			}
			return ("");
		}
		if (idPlantilla <= 0) {
			return ("Escoja la plantilla de Brevo.");
		}
		return ("");
	}

	/**
	 * Guarda como texto el filtro con el que se armo la campana.
	 *
	 * No es adorno: dentro de tres meses, viendo que una campana funciono, lo primero que alguien va
	 * a querer saber es a quien se le mando. Sin esto la respuesta se perdio.
	 */
	public static String resumenDeFiltros(final java.util.Map<String, String> p) {
		final StringBuilder t = new StringBuilder();
		final String[] campos = { "segmentos", "idtienda", "pedidosmin", "valormin",
				"diasmin", "diasmax", "canal", "concorreo", "autorizados", "universo",
				"diassinpublicidad", "tiposcliente", "productos",
				"familiafavorita", "fidelidadmin", "pizzasmin" };
		for (int i = 0; i < campos.length; i++) {
			final String valor = p.get(campos[i]);
			if (valor != null && valor.trim().length() > 0) {
				if (t.length() > 0) {
					t.append(" | ");
				}
				t.append(campos[i]).append("=").append(valor.trim());
			}
		}
		return (t.length() == 0 ? "todo el CRM" : t.toString());
	}

	/**
	 * Los filtros que venian de la pantalla anterior.
	 *
	 * Van aparte del filtro de segmento porque salen a buscar a cliente y a pedido, mientras el otro
	 * se resuelve dentro del resumen por persona. Tenerlos separados deja a la vista cuales son los
	 * caros.
	 */
	public static CampanaDAO.FiltroExtra extraDe(final java.util.Map<String, String> p) {
		final CampanaDAO.FiltroExtra e = new CampanaDAO.FiltroExtra();
		e.diasSinPublicidad = entero(p.get("diassinpublicidad"));
		e.excluirPlataformas = "S".equals(p.get("excluirplataformas"));
		e.pedidosMax = entero(p.get("pedidosmax"));
		e.puntosMin = entero(p.get("puntosmin"));
		e.soloMiembrosClub = "S".equals(p.get("soloclub"));
		e.correoContiene = texto(p.get("correo"));
		e.compraDesde = texto(p.get("comprodesde"));
		e.compraHasta = texto(p.get("comprohasta"));
		llenarTexto(e.tiposCliente, p.get("tiposcliente"));
		llenarEntero(e.productos, p.get("productos"));
		llenarEntero(e.especialidades, p.get("especialidades"));
		llenarEntero(e.excepciones, p.get("promociones"));
		return (e);
	}

	/** Las listas llegan separadas por coma, como en el resto de la pantalla. */
	private static void llenarEntero(final ArrayList<Integer> destino, final String valor) {
		if (valor == null || valor.trim().length() == 0) {
			return;
		}
		final String[] partes = valor.split(",");
		for (int i = 0; i < partes.length; i++) {
			final int n = entero(partes[i]);
			if (n > 0) {
				destino.add(Integer.valueOf(n));
			}
		}
	}

	private static void llenarTexto(final ArrayList<String> destino, final String valor) {
		if (valor == null || valor.trim().length() == 0) {
			return;
		}
		final String[] partes = valor.split(",");
		for (int i = 0; i < partes.length; i++) {
			if (partes[i].trim().length() > 0) {
				destino.add(partes[i].trim());
			}
		}
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor.trim());
	}

	private static int entero(final String valor) {
		try {
			return (Integer.parseInt(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	private static long largo(final String valor) {
		try {
			return (Long.parseLong(valor.trim()));
		} catch (final Exception e) {
			return (0);
		}
	}

	// =======================================================================
	// Programar para una fecha y hora futura
	//
	// Una programacion guarda los PARAMETROS de la pantalla, no la lista de gente. A la hora, el
	// reloj del servidor (utilidadesCC.ProgramadorPublicidad) vuelve a armar el publico con los
	// datos de ese momento y pasa por EXACTAMENTE el mismo camino que un envio inmediato:
	// crearYEnviar. Por eso las mismas redes aplican: la de "esperados", el tope del correo
	// directo, que no haya otro correo directo en curso, y que la oferta siga vigente.
	// =======================================================================

	/** Minutos de gracia por defecto si el parametro no esta. */
	private static final int GRACIA_POR_DEFECTO_MIN = 120;

	private static int graciaMinutos() {
		int minutos = GRACIA_POR_DEFECTO_MIN;
		try {
			final int valor = capaDAOCC.ParametrosDAO.retornarValorNumerico("PUBLICIDADPROGRAMADAGRACIAMIN");
			if (valor > 0) {
				minutos = valor;
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("EnvioPublicidadCtrl.graciaMinutos: " + e.toString());
		}
		return (minutos);
	}

	/**
	 * Lleva "2026-10-12T08:30" -o con espacio, o con segundos- a "yyyy-MM-dd HH:mm:ss".
	 * null si no es una fecha y hora real: se rechaza el 31 de febrero en vez de dejarlo correr.
	 */
	private static String fechaHora(final String cuando) {
		if (cuando == null) {
			return (null);
		}
		String t = cuando.trim().replace('T', ' ');
		if (!t.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?")) {
			return (null);
		}
		if (t.length() == 16) {
			t += ":00";
		}
		try {
			final java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			f.setLenient(false);
			f.parse(t);
		} catch (final java.text.ParseException e) {
			return (null);
		}
		return (t);
	}

	/**
	 * Deja un envio listo para dispararse solo a la hora escogida.
	 *
	 * @param p       los mismos parametros que llegarian a "enviar" (filtros, canalenvio, plantilla,
	 *                contenido, tope, esperados, idoferta...)
	 * @param cuando  fecha y hora futura, en la hora del servidor
	 */
	@SuppressWarnings("unchecked")
	public static String programar(final java.util.Map<String, String> p, final String cuando,
			final String usuario) {
		final JSONObject r = new JSONObject();

		final String ejecutarEn = fechaHora(cuando);
		if (ejecutarEn == null) {
			r.put("error", "Escoja la fecha y la hora en que debe salir el envio.");
			return (r.toJSONString());
		}
		final int minutos = capaDAOCC.CampanaProgramadaDAO.minutosHasta(ejecutarEn);
		if (minutos == Integer.MIN_VALUE) {
			r.put("error", "No se pudo validar la fecha escogida. Intente de nuevo.");
			return (r.toJSONString());
		}
		if (minutos < capaDAOCC.CampanaProgramadaDAO.MINIMO_MINUTOS) {
			r.put("error", "La hora tiene que ser futura: al menos " + capaDAOCC.CampanaProgramadaDAO.MINIMO_MINUTOS
					+ " minutos desde ahora. Si es para ya, use Enviar.");
			return (r.toJSONString());
		}
		if (minutos > capaDAOCC.CampanaProgramadaDAO.MAXIMO_DIAS * 24 * 60) {
			r.put("error", "No se puede programar a mas de " + capaDAOCC.CampanaProgramadaDAO.MAXIMO_DIAS
					+ " dias. Revise el ano.");
			return (r.toJSONString());
		}

		final String canal = texto(p.get("canalenvio"));
		final long idCampana = largo(p.get("idcampana"));
		final String nombre = texto(p.get("nombre"));
		final int idOferta = entero(p.get("idoferta"));
		final String problema = problemaDeEnvio(canal, idCampana, nombre, entero(p.get("idplantilla")),
				p.get("cuerpo"), idOferta);
		if (problema.length() > 0) {
			r.put("error", problema);
			return (r.toJSONString());
		}
		final int esperados = entero(p.get("esperados"));
		if (esperados <= 0) {
			//Es la red del envio: sin el numero que la persona vio y aprobo, a la hora no habria contra
			//que comparar, y un filtro que se pierda no se notaria hasta despues.
			r.put("error", "Cuente el publico antes de programar: el numero que vea es el que se aprueba.");
			return (r.toJSONString());
		}

		//Los parametros tal cual, menos lo que es de la accion y no del envio.
		final JSONObject guardado = new JSONObject();
		for (final java.util.Map.Entry<String, String> e : p.entrySet()) {
			if ("accion".equals(e.getKey()) || "programar_para".equals(e.getKey())) {
				continue;
			}
			guardado.put(e.getKey(), e.getValue());
		}

		final long id = capaDAOCC.CampanaProgramadaDAO.crear(idCampana, nombre, canal, idOferta, esperados,
				entero(p.get("tope")), resumenDeFiltros(p), guardado.toJSONString(), ejecutarEn, usuario);
		if (id == 0) {
			r.put("error", "No se pudo guardar la programacion.");
			return (r.toJSONString());
		}
		Logger.getLogger("log_file").info("[EnvioProgramado] " + usuario + " programo el envio " + id
				+ " para " + ejecutarEn + " (canal " + canal + ", " + esperados + " esperados)");
		r.put("idprogramacion", id);
		r.put("ejecutar_en", ejecutarEn.substring(0, 16));
		r.put("mensaje", "Quedo programado para el " + ejecutarEn.substring(0, 16)
				+ ". El publico se arma a esa hora con los datos de ese momento.");
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String programadas() {
		final JSONObject r = new JSONObject();
		final JSONArray lista = new JSONArray();
		for (final capaDAOCC.CampanaProgramadaDAO.Programacion p : capaDAOCC.CampanaProgramadaDAO.listar(14)) {
			final JSONObject o = new JSONObject();
			o.put("idprogramacion", p.idProgramacion);
			o.put("idcampana", p.idCampana);
			o.put("nombre", p.nombre);
			o.put("canal", p.canal);
			o.put("idoferta", p.idOferta);
			o.put("esperados", p.esperados);
			o.put("tope", p.tope);
			o.put("resumen", p.resumenFiltros);
			o.put("ejecutar_en", p.ejecutarEn);
			o.put("estado", p.estado);
			o.put("idenvio", p.idEnvio);
			o.put("resultado", p.resultado);
			o.put("usuario", p.usuario);
			o.put("creada_en", p.creadoEn);
			lista.add(o);
		}
		r.put("programadas", lista);
		return (r.toJSONString());
	}

	@SuppressWarnings("unchecked")
	public static String cancelarProgramada(final long idProgramacion, final String usuario) {
		final JSONObject r = new JSONObject();
		if (idProgramacion <= 0) {
			r.put("error", "No se indico cual programacion.");
			return (r.toJSONString());
		}
		if (capaDAOCC.CampanaProgramadaDAO.cancelar(idProgramacion, usuario)) {
			Logger.getLogger("log_file").info("[EnvioProgramado] " + usuario + " cancelo la programacion "
					+ idProgramacion);
			r.put("mensaje", "Programacion cancelada. No se mando nada.");
		} else {
			r.put("error", "Ya no se puede cancelar: o ya salio, o ya la cancelaron, o esta saliendo en este momento.");
		}
		return (r.toJSONString());
	}

	/**
	 * Dispara una programacion que ya llego a su hora. La llama el reloj del servidor.
	 *
	 * Pasa por crearYEnviar, el mismo camino del envio inmediato: no hay una segunda forma de
	 * mandar publicidad, y por eso no hay una segunda forma de equivocarse.
	 */
	public static void ejecutarProgramada(final long idProgramacion) {
		final Logger logger = Logger.getLogger("log_file");
		final capaDAOCC.CampanaProgramadaDAO.Programacion p = capaDAOCC.CampanaProgramadaDAO.obtener(idProgramacion);
		if (p == null || !capaDAOCC.CampanaProgramadaDAO.PROGRAMADA.equals(p.estado)) {
			return;
		}

		//Paso su plazo: no se manda tarde. Una oferta pensada para las 8 de la manana que llega a las
		//4 de la tarde es peor que no llegar, y quien la programo no esta mirando.
		final int gracia = graciaMinutos();
		if (p.minutosVencida > gracia) {
			capaDAOCC.CampanaProgramadaDAO.vencer(idProgramacion, "Paso su hora por " + p.minutosVencida
					+ " minutos (el plazo de gracia es de " + gracia + ") y no se mando. Reprogramela si todavia aplica.");
			logger.warn("[EnvioProgramado] " + idProgramacion + " vencida sin enviarse.");
			return;
		}

		//El correo directo sale de a uno desde un solo reloj. Si hay otra tanda saliendo, esta espera al
		//siguiente minuto -hasta que venza la gracia- en vez de fallar.
		final boolean usaCorreoDirecto = CampanaDAO.CANAL_DIRECTO.equals(p.canal) || p.idOferta > 0;
		if (usaCorreoDirecto && CampanaDAO.envioDirectoEnCurso() != 0) {
			return;
		}

		if (!capaDAOCC.CampanaProgramadaDAO.reclamar(idProgramacion)) {
			return; //Otra instancia la tomo primero.
		}

		try {
			final java.util.Map<String, String> params = new java.util.HashMap<String, String>();
			final org.json.simple.JSONObject guardado =
					(org.json.simple.JSONObject) new org.json.simple.parser.JSONParser().parse(p.parametros);
			for (final Object clave : guardado.keySet()) {
				final Object valor = guardado.get(clave);
				params.put(String.valueOf(clave), valor == null ? "" : String.valueOf(valor));
			}

			final String respuesta = crearYEnviar(
					largo(params.get("idcampana")),
					params.get("nombre"),
					params.get("canalenvio"),
					entero(params.get("idplantilla")),
					params.get("asunto"),
					params.get("cuerpo"),
					entero(params.get("tope")),
					entero(params.get("esperados")),
					p.resumenFiltros,
					SegmentacionPersonaCtrl.filtroDe(params),
					extraDe(params),
					p.usuario,
					entero(params.get("idoferta")));

			final org.json.simple.JSONObject r =
					(org.json.simple.JSONObject) new org.json.simple.parser.JSONParser().parse(respuesta);
			final Object idEnvio = r.get("idenvio");
			final Long envio = idEnvio instanceof Number ? Long.valueOf(((Number) idEnvio).longValue()) : null;
			if (r.get("error") != null) {
				capaDAOCC.CampanaProgramadaDAO.terminar(idProgramacion, capaDAOCC.CampanaProgramadaDAO.FALLIDA,
						envio, String.valueOf(r.get("error")));
				logger.error("[EnvioProgramado] " + idProgramacion + " no salio: " + r.get("error"));
			} else {
				capaDAOCC.CampanaProgramadaDAO.terminar(idProgramacion, capaDAOCC.CampanaProgramadaDAO.ENVIADA,
						envio, String.valueOf(r.get("mensaje")));
				logger.info("[EnvioProgramado] " + idProgramacion + " salio como envio " + envio + ": "
						+ r.get("mensaje"));
			}
		} catch (final Throwable t) {
			capaDAOCC.CampanaProgramadaDAO.terminar(idProgramacion, capaDAOCC.CampanaProgramadaDAO.FALLIDA, null,
					"Fallo al dispararla: " + t.toString());
			logger.error("[EnvioProgramado] " + idProgramacion + " fallo: " + t.toString());
		}
	}
}
