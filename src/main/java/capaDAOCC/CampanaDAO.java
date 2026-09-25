package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Las campanas de envio de publicidad.
 *
 * UNA PERSONA, UN ENVIO
 *
 * Es la razon de ser de esta clase. La pantalla anterior agrupaba por correo
 * -GROUP BY LOWER(TRIM(email))- y eso tenia dos consecuencias medidas: quien
 * tiene dos correos recibia el mensaje dos veces, y las 43.177 personas que
 * compran sin tener correo no existian para ninguna campana. Para WhatsApp era
 * peor todavia: el celular lo tienen 255.156 personas y el correo 231.354.
 *
 * Aqui la llave de destinatario es (idcampana, idpersona). Mandar dos veces a
 * la misma persona en la misma campana no es un error que haya que cuidar: es
 * imposible.
 *
 * LOS DOS CONSENTIMIENTOS
 *
 * No basta con politica_datos. En cliente hay ademas envio_publicidad, que es
 * el "no me manden promociones" especifico. Medido el 2026-09-24: 667 filas
 * dicen si a datos y no a publicidad. Son pocas y por eso es facil que se
 * olviden, y son justamente las que no se pueden tocar.
 *
 * La regla es la mas restrictiva a proposito: si CUALQUIERA de las filas de
 * cliente de esa persona dice que no a publicidad, la persona queda por fuera.
 * Ante la duda con un dato personal, no se manda.
 *
 * EL FILTRO NO SE REESCRIBE
 *
 * El publico se arma con el mismo armarWhere de SegmentacionPersonaDAO sobre
 * crm.persona_resumen. Copiarlo aqui habria dejado dos definiciones de "el
 * publico" que se separan con el primer cambio, y entonces la pantalla que
 * cuenta y la que manda dirian cosas distintas.
 */
public class CampanaDAO {

	/** C correo por Brevo, W WhatsApp por Brevo, D correo directo. */
	public static final String CANAL_CORREO = "C";
	public static final String CANAL_WHATSAPP = "W";
	public static final String CANAL_DIRECTO = "D";

	/**
	 * La condicion de consentimiento, la misma para contar y para cargar.
	 *
	 * Va como texto y no como metodo con parametros porque no lleva ninguno: es
	 * una regla fija, y tenerla en un solo sitio es lo que evita que la cuenta
	 * y el envio difieran.
	 */
	private static final String CONSENTIMIENTO =
			" AND politica_datos = 'S'"
			+ " AND NOT EXISTS (SELECT 1 FROM pizzaamericana.cliente c"
			+ "                  WHERE c.idpersona = crm.persona_resumen.idpersona"
			+ "                    AND IFNULL(c.envio_publicidad,'N') <> 'S')";

	// =======================================================================
	// Lo que se devuelve
	// =======================================================================

	/**
	 * El maestro: la idea de campana, que se reusa.
	 *
	 * COMBO FUTBOLERO es una campana; lo que se mando el 24 de septiembre es un
	 * Envio de esa campana. Tenerlos separados es lo que permite preguntar "como
	 * le va al COMBO FUTBOLERO" y no solo "como le fue el 24".
	 */
	public static class Campana {
		public long idCampana;
		public String nombre = "";
		public String canal = "";
		public int idPlantilla;
		public String asunto = "";
		/** Solo lo usa el correo directo: Brevo trae su plantilla. */
		public String cuerpo = "";
		public String filtros = "";
		/** Con cuantos dias de descanso se arma por defecto cada tanda. */
		public int diasSinPublicidad;
		/** Cuantos por tanda; 0 es sin tope. */
		public int tope;
		/** Acumulados de TODAS las tandas. */
		public int publico;
		public int enviados;
		public int fallidos;
		public int envios;
		public String estado = "";
		public String usuario = "";
		public String creadaEn = "";
		public String ultimoEnvioEn = "";
	}

	/** Una disparada concreta de una campana. */
	public static class Envio {
		public long idEnvio;
		public long idCampana;
		public String campana = "";
		public int consecutivo;
		public String canal = "";
		public int idPlantilla;
		public String asunto = "";
		public String cuerpo = "";
		public int tope;
		public int diasSinPublicidad;
		public int publico;
		public int enviados;
		public int fallidos;
		public int pendientes;
		public String estado = "";
		public String usuario = "";
		public String creadoEn = "";
		public String terminadoEn = "";
	}

	/** Una persona a la que hay que escribirle. */
	public static class Destinatario {
		public long idPersona;
		public String destino = "";
		public String nombre = "";
	}

	/** Cuanta gente alcanza cada canal con el filtro puesto. */
	public static class Alcance {
		public int personas;
		public int conCorreo;
		public int conCelular;
		public int sinConsentimiento;
	}

	// =======================================================================
	// Cuanta gente hay
	// =======================================================================

	/**
	 * El publico, antes de disparar.
	 *
	 * Devuelve las tres cifras juntas -total, con correo, con celular- porque la
	 * pantalla tiene que poder decir "por correo le llega a 16.800 y por
	 * WhatsApp a 18.432" sin volver a preguntar. Y devuelve cuantos quedaron
	 * por fuera por consentimiento, que es la cifra que nadie mira y la que
	 * mete en problemas.
	 */
	public static Alcance alcance(final SegmentacionPersonaDAO.Filtro filtro,
			final FiltroExtra extra) {
		final Alcance a = new Alcance();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			SegmentacionPersonaDAO.sanear(filtro);
			cn = con.obtenerConexionBDPrincipal();

			final ArrayList<Object> valores = new ArrayList<Object>();
			final String where = SegmentacionPersonaDAO.armarWhere(filtro, valores, true)
					+ armarExtra(extra, valores);

			//Dos consultas sobre el mismo filtro: con consentimiento y sin el.
			//La diferencia es la gente que el filtro encontraba pero a la que
			//no se le puede escribir, y esa cifra se muestra.
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT COUNT(*) AS personas,"
					+ " SUM(email IS NOT NULL AND TRIM(email) <> '') AS con_correo,"
					+ " SUM(celular IS NOT NULL AND TRIM(celular) <> '') AS con_celular"
					+ " FROM crm.persona_resumen" + where + CONSENTIMIENTO);
			SegmentacionPersonaDAO.ponerValores(ps, valores);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				a.personas = rs.getInt("personas");
				a.conCorreo = rs.getInt("con_correo");
				a.conCelular = rs.getInt("con_celular");
			}
			rs.close();
			ps.close();

			final ArrayList<Object> valores2 = new ArrayList<Object>();
			final String where2 = SegmentacionPersonaDAO.armarWhere(filtro, valores2, true)
					+ armarExtra(extra, valores2);
			final PreparedStatement ps2 = cn.prepareStatement(
					"SELECT COUNT(*) AS personas FROM crm.persona_resumen" + where2);
			SegmentacionPersonaDAO.ponerValores(ps2, valores2);
			final ResultSet rs2 = ps2.executeQuery();
			if (rs2.next()) {
				a.sinConsentimiento = rs2.getInt("personas") - a.personas;
			}
			rs2.close();
			ps2.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.alcance: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (a);
	}

	// =======================================================================
	// Crear la campana y cargarle la gente
	// =======================================================================

	/**
	 * Los dias que debe descansar una persona entre un envio y el siguiente.
	 *
	 * Es lo que evita que las tandas se pisen. La carga ordena por valor DESC,
	 * asi que sin este descanso los 200 de manana serian los mismos 200 de hoy
	 * -los de mas valor- y el resto del segmento no recibiria nunca.
	 *
	 * Vive en general.parametros porque es justo lo que mercadeo va a querer
	 * mover cuando vea como responde la gente.
	 */
	public static int diasMinimosPorDefecto() {
		int dias = 30;
		try {
			final int valor = ParametrosDAO.retornarValorNumerico("PUBLICIDADDIASMINIMOS");
			if (valor > 0) {
				dias = valor;
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.diasMinimos: " + e.toString());
		}
		return (dias);
	}

	/**
	 * Crea el maestro de campana, o devuelve el que ya existe con ese nombre.
	 *
	 * El nombre es unico a proposito: dos campanas llamadas igual son un error
	 * de dedo, y el dia que se comparen desempenos no se sabria cual es cual.
	 */
	public static long crear(final String nombre, final String canal, final int idPlantilla,
			final String asunto, final String cuerpo, final String filtros,
			final int diasSinPublicidad, final int tope, final String usuario) {
		long id = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO crm.campana (nombre, canal, idplantilla, asunto, cuerpo, filtros,"
					+ " dias_sin_publicidad, tope, estado, usuario, creada_en)"
					+ " VALUES (?,?,?,?,?,?,?,?, 'ACTIVA', ?, NOW())",
					Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, nombre);
			ps.setString(2, canal);
			ps.setInt(3, idPlantilla);
			ps.setString(4, asunto);
			ps.setString(5, cuerpo);
			ps.setString(6, recortar(filtros, 2000));
			ps.setInt(7, diasSinPublicidad);
			ps.setInt(8, tope);
			ps.setString(9, usuario);
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.crear: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	/** El maestro con ese nombre, o 0 si no existe. */
	public static long buscarPorNombre(final String nombre) {
		long id = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idcampana FROM crm.campana WHERE nombre = ?");
			ps.setString(1, nombre);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				id = rs.getLong("idcampana");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.buscarPorNombre: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	/**
	 * Deja en el maestro lo ultimo que se uso.
	 *
	 * Asi, al escoger la campana en el desplegable, vuelven el canal, la
	 * plantilla, el asunto y el tope con los que se mando la vez pasada. Es lo
	 * que hace que reusar una campana sea un clic y no volver a armarla.
	 */
	public static void actualizarMaestro(final long idCampana, final String canal,
			final int idPlantilla, final String asunto, final String cuerpo, final String filtros,
			final int diasSinPublicidad, final int tope) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana SET canal = ?, idplantilla = ?, asunto = ?, cuerpo = ?,"
					+ " filtros = ?, dias_sin_publicidad = ?, tope = ? WHERE idcampana = ?");
			ps.setString(1, canal);
			ps.setInt(2, idPlantilla);
			ps.setString(3, asunto);
			ps.setString(4, cuerpo);
			ps.setString(5, recortar(filtros, 2000));
			ps.setInt(6, diasSinPublicidad);
			ps.setInt(7, tope);
			ps.setLong(8, idCampana);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.actualizarMaestro: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Abre una tanda nueva de una campana.
	 *
	 * El consecutivo se calcula aqui dentro y no lo manda la pantalla: dos
	 * personas disparando la misma campana a la vez se pisarian el numero.
	 *
	 * Canal, plantilla y asunto se copian a la tanda y no se dejan solo en el
	 * maestro porque el maestro cambia: si el mes entrante se usa otra
	 * plantilla, el historial tiene que seguir diciendo con cual se mando en
	 * septiembre. Sin eso, comparar tandas compara cosas distintas creyendo que
	 * son la misma.
	 */
	public static long crearEnvio(final long idCampana, final String canal, final int idPlantilla,
			final String asunto, final String cuerpo, final String filtros, final int tope,
			final int diasSinPublicidad, final String usuario) {
		long id = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO crm.campana_envio (idcampana, consecutivo, canal, idplantilla,"
					+ " asunto, cuerpo, filtros, tope, dias_sin_publicidad, estado, usuario, creado_en)"
					+ " SELECT ?, IFNULL(MAX(e.consecutivo),0) + 1, ?,?,?,?,?,?,?, 'BORRADOR', ?, NOW()"
					+ "   FROM crm.campana_envio e WHERE e.idcampana = ?",
					Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, idCampana);
			ps.setString(2, canal);
			ps.setInt(3, idPlantilla);
			ps.setString(4, asunto);
			ps.setString(5, cuerpo);
			ps.setString(6, recortar(filtros, 2000));
			ps.setInt(7, tope);
			ps.setInt(8, diasSinPublicidad);
			ps.setString(9, usuario);
			ps.setLong(10, idCampana);
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.crearEnvio: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	/**
	 * Mete en la campana a todas las personas que cumplen el filtro Y tienen
	 * por donde recibir ese canal.
	 *
	 * Va como INSERT ... SELECT y no trayendo la lista al programa para pegarla
	 * de vuelta: con un publico de decenas de miles, traerla y devolverla es
	 * mover megas por la red para nada, y ademas deja una ventana en la que la
	 * lista ya no es la que se conto.
	 *
	 * EL ORDEN ES LO QUE HACE POSIBLE ENVIAR POR TANDAS
	 *
	 * Se ordena por valor DESC: si solo caben 500, que sean los 500 que mas
	 * valen. Y como el filtro de dias sin publicidad deja por fuera a quien ya
	 * recibio, la tanda de manana toma LOS SIGUIENTES, no los mismos. Sin ese
	 * filtro, "hoy 500 y manana 200" le escribiria dos veces a los mismos 200.
	 *
	 * @param tope 0 para sin tope
	 * @return cuanta gente quedo
	 */
	public static int cargarDestinatarios(final long idEnvio, final long idCampana,
			final String canal, final SegmentacionPersonaDAO.Filtro filtro,
			final FiltroExtra extra, final int tope) {
		int cuantos = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			SegmentacionPersonaDAO.sanear(filtro);
			cn = con.obtenerConexionBDPrincipal();

			//Por WhatsApp el destino es el celular; por correo, el correo. Y en
			//cada caso se exige que ese dato exista: una persona sin correo no
			//puede entrar a una campana de correo, aunque cumpla el filtro.
			final boolean porCelular = CANAL_WHATSAPP.equals(canal);
			final String columnaDestino = porCelular ? "celular" : "email";

			final ArrayList<Object> valores = new ArrayList<Object>();
			final String where = SegmentacionPersonaDAO.armarWhere(filtro, valores, true)
					+ armarExtra(extra, valores);

			final StringBuilder sql = new StringBuilder();
			sql.append("INSERT IGNORE INTO crm.campana_destinatario")
				.append(" (idenvio, idcampana, idpersona, destino, nombre, estado)")
				.append(" SELECT ?, ?, idpersona, ").append(columnaDestino).append(",")
				.append(" TRIM(CONCAT(IFNULL(nombre,''),' ',IFNULL(apellido,''))), 'PENDIENTE'")
				.append(" FROM crm.persona_resumen").append(where).append(CONSENTIMIENTO)
				.append(" AND ").append(columnaDestino).append(" IS NOT NULL")
				.append(" AND TRIM(").append(columnaDestino).append(") <> ''");
			//El orden importa: si solo caben 500, que sean los 500 que mas
			//valen, no los primeros que salgan.
			sql.append(" ORDER BY valor DESC");
			if (tope > 0) {
				sql.append(" LIMIT ").append(tope);
			}

			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setLong(1, idEnvio);
			ps.setLong(2, idCampana);
			final ArrayList<Object> todos = new ArrayList<Object>();
			todos.addAll(valores);
			ponerDesde(ps, 3, todos);
			cuantos = ps.executeUpdate();
			ps.close();

			final PreparedStatement psUp = cn.prepareStatement(
					"UPDATE crm.campana_envio SET publico = ? WHERE idenvio = ?");
			psUp.setInt(1, cuantos);
			psUp.setLong(2, idEnvio);
			psUp.executeUpdate();
			psUp.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.cargarDestinatarios: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (cuantos);
	}

	// =======================================================================
	// Mandar
	// =======================================================================

	/** Los que faltan por enviar, de a poquitos. */
	public static ArrayList<Destinatario> pendientes(final long idEnvio, final int limite) {
		final ArrayList<Destinatario> lista = new ArrayList<Destinatario>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idpersona, destino, IFNULL(nombre,'') AS nombre"
					+ " FROM crm.campana_destinatario"
					+ " WHERE idenvio = ? AND estado = 'PENDIENTE'"
					+ " ORDER BY idpersona LIMIT ?");
			ps.setLong(1, idEnvio);
			ps.setInt(2, limite);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Destinatario d = new Destinatario();
				d.idPersona = rs.getLong("idpersona");
				d.destino = rs.getString("destino");
				d.nombre = rs.getString("nombre");
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.pendientes: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Deja dicho como le fue a una persona.
	 *
	 * Se marca de a uno y no al final del lote a proposito: si el servidor se
	 * cae a mitad de camino, lo ya enviado queda marcado como enviado y al
	 * reanudar no se le vuelve a escribir a nadie. Repetirle un correo a un
	 * cliente es de las pocas cosas que de verdad molestan.
	 */
	public static void marcar(final long idEnvio, final long idPersona, final String estado,
			final String detalle) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana_destinatario SET estado = ?, detalle = ?, enviado_en = NOW()"
					+ " WHERE idenvio = ? AND idpersona = ?");
			ps.setString(1, estado);
			ps.setString(2, recortar(detalle, 300));
			ps.setLong(3, idEnvio);
			ps.setLong(4, idPersona);
			ps.executeUpdate();
			ps.close();

			if ("ENVIADO".equals(estado)) {
				sellarPublicidad(cn, idPersona);
			}
			recalcular(cn, idEnvio);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.marcar: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Deja constancia de que a esta persona ya se le escribio hoy.
	 *
	 * ESTO ES LO QUE HACE QUE EL TOPE POR TANDAS SIRVA. El filtro de "dias sin
	 * publicidad" se apoya en cliente.ultima_fecha_publicidad, y esa columna la
	 * escribia unicamente la pantalla vieja de segmentacion: esta leia la marca
	 * pero nunca la ponia. Resultado, el filtro estaba ciego a sus propios
	 * envios y la tanda de manana volvia a tomar a los mismos de hoy.
	 *
	 * Se sella por PERSONA, en todas sus filas de cliente, porque el filtro
	 * tambien pregunta por persona. Sellar solo la fila por la que salio el
	 * correo dejaria a las otras filas de la misma persona viendose como si no
	 * hubieran recibido nada.
	 *
	 * Solo se sella lo ENVIADO: a quien le fallo el envio no le llego nada y no
	 * tiene por que descansar.
	 */
	private static void sellarPublicidad(final Connection cn, final long idPersona)
			throws Exception {
		final PreparedStatement ps = cn.prepareStatement(
				"UPDATE pizzaamericana.cliente SET ultima_fecha_publicidad = CURRENT_DATE"
				+ " WHERE idpersona = ?");
		ps.setLong(1, idPersona);
		ps.executeUpdate();
		ps.close();
	}

	/**
	 * Pone la tanda en el estado que le corresponde y sube los totales al
	 * maestro.
	 *
	 * Los acumulados de la campana se recalculan desde las tandas y no se van
	 * sumando: sumar deja el total mal para siempre en cuanto una tanda se
	 * cancele o se reintente, y nadie vuelve a saber cual de los dos numeros
	 * creer.
	 */
	private static void recalcular(final Connection cn, final long idEnvio) throws Exception {
		final PreparedStatement ps = cn.prepareStatement(
				"UPDATE crm.campana_envio e SET"
				+ " e.enviados = (SELECT COUNT(*) FROM crm.campana_destinatario d"
				+ "                WHERE d.idenvio = e.idenvio AND d.estado = 'ENVIADO'),"
				+ " e.fallidos = (SELECT COUNT(*) FROM crm.campana_destinatario d"
				+ "                WHERE d.idenvio = e.idenvio AND d.estado = 'FALLIDO'),"
				+ " e.estado = IF((SELECT COUNT(*) FROM crm.campana_destinatario d"
				+ "                 WHERE d.idenvio = e.idenvio AND d.estado = 'PENDIENTE') = 0,"
				+ "               'TERMINADA', e.estado),"
				+ " e.terminado_en = IF((SELECT COUNT(*) FROM crm.campana_destinatario d"
				+ "                       WHERE d.idenvio = e.idenvio AND d.estado = 'PENDIENTE') = 0,"
				+ "                     NOW(), e.terminado_en)"
				+ " WHERE e.idenvio = ?");
		ps.setLong(1, idEnvio);
		ps.executeUpdate();
		ps.close();

		totalizar(cn, idEnvio);
	}

	/** Sube al maestro la suma de sus tandas. */
	private static void totalizar(final Connection cn, final long idEnvio) throws Exception {
		final PreparedStatement ps = cn.prepareStatement(
				"UPDATE crm.campana c"
				+ "  JOIN crm.campana_envio x ON x.idcampana = c.idcampana"
				+ "   SET c.publico = (SELECT IFNULL(SUM(e.publico),0) FROM crm.campana_envio e"
				+ "                     WHERE e.idcampana = c.idcampana),"
				+ "       c.enviados = (SELECT IFNULL(SUM(e.enviados),0) FROM crm.campana_envio e"
				+ "                      WHERE e.idcampana = c.idcampana),"
				+ "       c.fallidos = (SELECT IFNULL(SUM(e.fallidos),0) FROM crm.campana_envio e"
				+ "                      WHERE e.idcampana = c.idcampana),"
				+ "       c.envios = (SELECT COUNT(*) FROM crm.campana_envio e"
				+ "                    WHERE e.idcampana = c.idcampana),"
				+ "       c.ultimo_envio_en = (SELECT MAX(e.creado_en) FROM crm.campana_envio e"
				+ "                             WHERE e.idcampana = c.idcampana)"
				+ " WHERE x.idenvio = ?");
		ps.setLong(1, idEnvio);
		ps.executeUpdate();
		ps.close();
	}

	/** Cambia el estado de una TANDA. */
	public static void cambiarEstado(final long idEnvio, final String estado) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana_envio SET estado = ? WHERE idenvio = ?");
			ps.setString(1, estado);
			ps.setLong(2, idEnvio);
			ps.executeUpdate();
			ps.close();
			totalizar(cn, idEnvio);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.cambiarEstado: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/** Jubila o revive una campana del maestro. */
	public static void cambiarEstadoCampana(final long idCampana, final String estado) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE crm.campana SET estado = ? WHERE idcampana = ?");
			ps.setString(1, estado);
			ps.setLong(2, idCampana);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.cambiarEstadoCampana: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	// =======================================================================
	// Consultar
	// =======================================================================

	/** Una tanda con lo suyo y el nombre de su campana. */
	public static Envio obtener(final long idEnvio) {
		Envio e = null;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT e.*, c.nombre AS campana,"
					+ "       (SELECT COUNT(*) FROM crm.campana_destinatario d"
					+ "         WHERE d.idenvio = e.idenvio AND d.estado = 'PENDIENTE') AS pendientes"
					+ "  FROM crm.campana_envio e"
					+ "  JOIN crm.campana c ON c.idcampana = e.idcampana"
					+ " WHERE e.idenvio = ?");
			ps.setLong(1, idEnvio);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				e = leerEnvio(rs);
			}
			rs.close();
			ps.close();
		} catch (final Exception ex) {
			Logger.getLogger("log_file").error("CampanaDAO.obtener: " + ex.toString());
		} finally {
			cerrar(cn);
		}
		return (e);
	}

	/** Las ultimas tandas, de todas las campanas: es el historial de la pantalla. */
	public static ArrayList<Envio> ultimas(final int cuantas) {
		final ArrayList<Envio> lista = new ArrayList<Envio>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT e.*, c.nombre AS campana,"
					+ "       (SELECT COUNT(*) FROM crm.campana_destinatario d"
					+ "         WHERE d.idenvio = e.idenvio AND d.estado = 'PENDIENTE') AS pendientes"
					+ "  FROM crm.campana_envio e"
					+ "  JOIN crm.campana c ON c.idcampana = e.idcampana"
					+ " ORDER BY e.idenvio DESC LIMIT ?");
			ps.setInt(1, cuantas);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(leerEnvio(rs));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.ultimas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * El maestro, para el desplegable.
	 *
	 * Van primero las que se usaron hace poco: quien entra a mandar algo casi
	 * siempre viene a repetir lo de la semana pasada, no a buscar lo del ano
	 * pasado.
	 */
	public static ArrayList<Campana> campanas(final boolean soloActivas) {
		final ArrayList<Campana> lista = new ArrayList<Campana>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT c.* FROM crm.campana c"
					+ (soloActivas ? " WHERE c.estado = 'ACTIVA'" : "")
					+ " ORDER BY IFNULL(c.ultimo_envio_en, c.creada_en) DESC, c.idcampana DESC");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(leer(rs));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.campanas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** La tanda de correo directo que este a medio camino, si hay alguna. */
	public static long envioDirectoEnCurso() {
		long id = 0;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final Statement stm = cn.createStatement();
			final ResultSet rs = stm.executeQuery(
					"SELECT idenvio FROM crm.campana_envio"
					+ " WHERE canal = 'D' AND estado = 'ENVIANDO'"
					+ " ORDER BY idenvio LIMIT 1");
			if (rs.next()) {
				id = rs.getLong("idenvio");
			}
			rs.close();
			stm.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.envioDirectoEnCurso: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (id);
	}

	// =======================================================================
	// Medir si sirvio
	// =======================================================================

	/** Cuanta gente de la campana compro despues de recibirla. */
	public static class Resultado {
		public int enviados;
		public int compraron;
		public double valor;
		public int horas;
	}

	/**
	 * Cruza a quien le llego contra quien compro despues.
	 *
	 * Esto es lo que separa un CRM de un enviador de correos. Y se mide contra
	 * la hora de ENVIO de cada persona, no contra la hora de la campana: en el
	 * correo directo el ultimo recibe media hora despues del primero, y
	 * medirlos con la misma vara le regalaria tiempo a unos y se lo quitaria a
	 * otros.
	 *
	 * OJO CON LEERLO COMO CAUSA. Que alguien compre despues de recibir no
	 * prueba que compro POR el mensaje; habria que comparar contra gente
	 * parecida que no lo recibio. Sirve para comparar campanas entre si, que ya
	 * es bastante.
	 */
	/** Como le fue a UNA tanda. */
	public static Resultado resultado(final long idEnvio, final int horas) {
		return (medir("d.idenvio = ?", idEnvio, horas));
	}

	/**
	 * Como le va a la campana completa, sumando todas sus tandas.
	 *
	 * Es la pregunta que solo se puede responder desde que campana y disparada
	 * estan separadas: "sirve el COMBO FUTBOLERO", no "sirvio el envio del 24".
	 */
	public static Resultado resultadoCampana(final long idCampana, final int horas) {
		return (medir("d.idcampana = ?", idCampana, horas));
	}

	private static Resultado medir(final String condicion, final long id, final int horas) {
		final Resultado r = new Resultado();
		r.horas = horas;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					//Con subconsultas correlacionadas y no con LATERAL: LATERAL
					//solo existe desde MySQL 8.0.14 y esto tiene que poder
					//correr sin averiguar primero en que version esta cada
					//servidor. El costo es el mismo: la subconsulta se evalua
					//una vez por destinatario en los dos casos.
					"SELECT COUNT(*) AS enviados,"
					+ " SUM((SELECT COUNT(*) FROM pizzaamericana.pedido pe"
					+ "       JOIN pizzaamericana.cliente cl ON cl.idcliente = pe.idcliente"
					+ "      WHERE cl.idpersona = d.idpersona"
					+ "        AND pe.fecha_cancelacion IS NULL"
					+ "        AND pe.fechainsercion > d.enviado_en"
					+ "        AND pe.fechainsercion <= DATE_ADD(d.enviado_en, INTERVAL ? HOUR)) > 0)"
					+ "     AS compraron,"
					+ " IFNULL(SUM((SELECT IFNULL(SUM(pe.total_neto),0) FROM pizzaamericana.pedido pe"
					+ "              JOIN pizzaamericana.cliente cl ON cl.idcliente = pe.idcliente"
					+ "             WHERE cl.idpersona = d.idpersona"
					+ "               AND pe.fecha_cancelacion IS NULL"
					+ "               AND pe.fechainsercion > d.enviado_en"
					+ "               AND pe.fechainsercion <= DATE_ADD(d.enviado_en, INTERVAL ? HOUR))),0)"
					+ "     AS valor"
					+ " FROM crm.campana_destinatario d"
					+ " WHERE " + condicion + " AND d.estado = 'ENVIADO'");
			ps.setInt(1, horas);
			ps.setInt(2, horas);
			ps.setLong(3, id);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				r.enviados = rs.getInt("enviados");
				r.compraron = rs.getInt("compraron");
				r.valor = rs.getDouble("valor");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO.resultado: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}


	// =======================================================================
	// Los filtros que venian de la pantalla anterior
	// =======================================================================

	/**
	 * Lo que la pantalla vieja sabia filtrar y crm.persona_resumen no tiene.
	 *
	 * Van aparte del Filtro de SegmentacionPersonaDAO a proposito: ese filtra
	 * sobre el resumen por persona, que es rapido; estos salen a buscar a
	 * cliente y a pedido. Tenerlos separados deja a la vista cuales son los
	 * caros.
	 */
	public static class FiltroExtra {
		/** No escribirle a quien recibio publicidad en los ultimos N dias. */
		public int diasSinPublicidad = 0;
		/** Deja por fuera los correos de plataforma, que no son del cliente. */
		public boolean excluirPlataformas = false;
		/** natural, juridica. */
		public ArrayList<String> tiposCliente = new ArrayList<String>();
		public ArrayList<Integer> productos = new ArrayList<Integer>();
		public ArrayList<Integer> especialidades = new ArrayList<Integer>();
		/** Las promociones, que en la tabla son excepciones. */
		public ArrayList<Integer> excepciones = new ArrayList<Integer>();
		public int pedidosMax = 0;
		public int puntosMin = 0;
		public boolean soloMiembrosClub = false;
		public String correoContiene = "";
		/** yyyy-MM-dd; compro entre esas dos fechas. */
		public String compraDesde = "";
		public String compraHasta = "";
	}

	/**
	 * Arma la parte del WHERE que sale del resumen.
	 *
	 * OJO CON PRODUCTO, ESPECIALIDAD Y PROMOCION
	 *
	 * Esos tres solo ven los pedidos que llegan al CENTRAL -domicilio y virtual-,
	 * porque el detalle de lo que se vendio solo existe alli:
	 * crm.stage_pedido_tienda trae totales por tienda, no lineas. O sea que
	 * filtrar por "pidio Hawaiana" deja por fuera a quien la pidio en el
	 * mostrador.
	 *
	 * No es algo que se rompio aqui: la pantalla anterior consultaba esa misma
	 * tabla y tenia exactamente el mismo hueco. Pero conviene saberlo antes de
	 * sacar conclusiones de un publico armado con esos filtros.
	 */
	private static String armarExtra(final FiltroExtra e, final ArrayList<Object> valores) {
		final StringBuilder w = new StringBuilder();
		if (e == null) {
			return ("");
		}

		if (e.diasSinPublicidad > 0) {
			// La regla mas restrictiva: si CUALQUIERA de sus filas de cliente
			// recibio publicidad hace poco, la persona descansa. Al reves
			// -exigir que todas hayan descansado- le escribiria igual a quien ya
			// recibio, que es justo lo que este filtro viene a evitar.
			w.append(" AND NOT EXISTS (SELECT 1 FROM pizzaamericana.cliente cp")
				.append("                  WHERE cp.idpersona = crm.persona_resumen.idpersona")
				.append("                    AND cp.ultima_fecha_publicidad IS NOT NULL")
				.append("                    AND cp.ultima_fecha_publicidad > CURRENT_DATE - INTERVAL ? DAY)");
			valores.add(Integer.valueOf(e.diasSinPublicidad));
		}

		if (e.excluirPlataformas) {
			// Son los que encabezan cualquier conteo -"privacy protection",
			// "CLIENTE RAPPI"- y su correo no es del cliente sino del canal.
			w.append(" AND SUBSTRING_INDEX(LOWER(TRIM(IFNULL(email,''))), '@', -1) <> 'rappi.com'")
				.append(" AND LOWER(TRIM(IFNULL(email,''))) NOT IN ('notengo@gmail.com','notiene@gmail.com')");
		}

		if (e.correoContiene != null && e.correoContiene.trim().length() > 0) {
			w.append(" AND email LIKE ?");
			valores.add("%" + e.correoContiene.trim() + "%");
		}

		if (e.pedidosMax > 0) {
			w.append(" AND pedidos <= ?");
			valores.add(Integer.valueOf(e.pedidosMax));
		}

		if (!e.tiposCliente.isEmpty()) {
			// La pantalla habla de natural y juridica; la tabla guarda
			// idtipopersona, 2 y 1. La traduccion va en un solo sitio.
			final ArrayList<Integer> tipos = new ArrayList<Integer>();
			for (int i = 0; i < e.tiposCliente.size(); i++) {
				final String t = e.tiposCliente.get(i);
				if ("natural".equalsIgnoreCase(t)) {
					tipos.add(Integer.valueOf(2));
				} else if ("juridica".equalsIgnoreCase(t)) {
					tipos.add(Integer.valueOf(1));
				}
			}
			if (!tipos.isEmpty()) {
				w.append(" AND EXISTS (SELECT 1 FROM pizzaamericana.cliente ct")
					.append("             WHERE ct.idpersona = crm.persona_resumen.idpersona")
					.append("               AND ct.idtipopersona IN (").append(marcas(tipos.size()))
					.append("))");
				valores.addAll(tipos);
			}
		}

		if (e.soloMiembrosClub || e.puntosMin > 0) {
			// La fidelizacion se lleva por CORREO, no por idcliente. Quien no
			// tiene correo no puede estar en el club, y por eso este filtro
			// reduce el publico a los que si lo tienen.
			w.append(" AND EXISTS (SELECT 1 FROM pizzaamericana.cliente_fidelizacion cf")
				.append("             WHERE cf.correo = crm.persona_resumen.email");
			if (e.soloMiembrosClub) {
				w.append(" AND cf.activo = 'S'");
			}
			if (e.puntosMin > 0) {
				w.append(" AND cf.puntos_vigentes >= ?");
				valores.add(Integer.valueOf(e.puntosMin));
			}
			w.append(")");
		}

		// Los tres de detalle van todos contra el mismo EXISTS sobre el pedido
		// del central. Se arman por separado para que el que no se use no
		// cueste nada.
		if (!e.productos.isEmpty()) {
			w.append(existePedido(" AND d.idproducto IN (" + marcas(e.productos.size()) + ")"));
			valores.addAll(e.productos);
		}
		if (!e.especialidades.isEmpty()) {
			final String m = marcas(e.especialidades.size());
			// La especialidad puede venir en cualquiera de los dos lados de la
			// pizza, asi que hay que mirar las dos columnas.
			w.append(existePedido(" AND (d.idespecialidad1 IN (" + m + ")"
					+ " OR d.idespecialidad2 IN (" + m + "))"));
			valores.addAll(e.especialidades);
			valores.addAll(e.especialidades);
		}
		if (!e.excepciones.isEmpty()) {
			w.append(existePedido(" AND d.idexcepcion IN (" + marcas(e.excepciones.size()) + ")"));
			valores.addAll(e.excepciones);
		}

		if (e.compraDesde != null && e.compraDesde.length() > 0) {
			w.append(" AND EXISTS (SELECT 1 FROM pizzaamericana.cliente cf2")
				.append("             JOIN pizzaamericana.pedido pf ON pf.idcliente = cf2.idcliente")
				.append("            WHERE cf2.idpersona = crm.persona_resumen.idpersona")
				.append("              AND pf.fecha_cancelacion IS NULL")
				.append("              AND pf.fechapedido >= ?");
			valores.add(e.compraDesde);
			if (e.compraHasta != null && e.compraHasta.length() > 0) {
				w.append(" AND pf.fechapedido <= ?");
				valores.add(e.compraHasta);
			}
			w.append(")");
		}

		return (w.toString());
	}

	/**
	 * La busqueda sobre el detalle del pedido, que los tres filtros comparten.
	 *
	 * VA DESDE EL PRODUCTO HACIA LA PERSONA, NO AL REVES, Y ESO IMPORTA
	 *
	 * La forma natural seria un EXISTS correlacionado -"para esta persona,
	 * existe un pedido con este producto"-, pero eso obliga a evaluar la
	 * subconsulta una vez por cada una de las 450 mil personas. Medido el
	 * 2026-09-24: 24 segundos.
	 *
	 * Asi, en cambio, primero se resuelve quienes compraron ese producto -que
	 * son pocos- y despues se cruza. Con el indice idx_detalle_producto la
	 * misma consulta baja a 0,18 segundos. Ciento treinta y cinco veces.
	 *
	 * Si alguien quita esos indices, esto vuelve a tardar 14 segundos y la
	 * pantalla se siente rota.
	 */
	private static String existePedido(final String condicion) {
		return (" AND idpersona IN (SELECT cd.idpersona"
				+ "                    FROM pizzaamericana.detalle_pedido d"
				+ "                    JOIN pizzaamericana.pedido pd ON pd.idpedido = d.idpedido"
				+ "                    JOIN pizzaamericana.cliente cd ON cd.idcliente = pd.idcliente"
				+ "                   WHERE pd.fecha_cancelacion IS NULL"
				+ "                     AND cd.idpersona IS NOT NULL" + condicion + ")");
	}

	private static String marcas(final int cuantos) {
		final StringBuilder m = new StringBuilder();
		for (int i = 0; i < cuantos; i++) {
			m.append(i == 0 ? "?" : ",?");
		}
		return (m.toString());
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	private static Campana leer(final ResultSet rs) throws Exception {
		final Campana c = new Campana();
		c.idCampana = rs.getLong("idcampana");
		c.nombre = texto(rs.getString("nombre"));
		c.canal = texto(rs.getString("canal"));
		c.idPlantilla = rs.getInt("idplantilla");
		c.asunto = texto(rs.getString("asunto"));
		c.cuerpo = texto(rs.getString("cuerpo"));
		c.filtros = texto(rs.getString("filtros"));
		c.diasSinPublicidad = rs.getInt("dias_sin_publicidad");
		c.tope = rs.getInt("tope");
		c.publico = rs.getInt("publico");
		c.enviados = rs.getInt("enviados");
		c.fallidos = rs.getInt("fallidos");
		c.envios = rs.getInt("envios");
		c.estado = texto(rs.getString("estado"));
		c.usuario = texto(rs.getString("usuario"));
		c.creadaEn = texto(rs.getString("creada_en"));
		c.ultimoEnvioEn = texto(rs.getString("ultimo_envio_en"));
		return (c);
	}

	private static Envio leerEnvio(final ResultSet rs) throws Exception {
		final Envio e = new Envio();
		e.idEnvio = rs.getLong("idenvio");
		e.idCampana = rs.getLong("idcampana");
		e.campana = texto(rs.getString("campana"));
		e.consecutivo = rs.getInt("consecutivo");
		e.canal = texto(rs.getString("canal"));
		e.idPlantilla = rs.getInt("idplantilla");
		e.asunto = texto(rs.getString("asunto"));
		e.cuerpo = texto(rs.getString("cuerpo"));
		e.tope = rs.getInt("tope");
		e.diasSinPublicidad = rs.getInt("dias_sin_publicidad");
		e.publico = rs.getInt("publico");
		e.enviados = rs.getInt("enviados");
		e.fallidos = rs.getInt("fallidos");
		e.pendientes = rs.getInt("pendientes");
		e.estado = texto(rs.getString("estado"));
		e.usuario = texto(rs.getString("usuario"));
		e.creadoEn = texto(rs.getString("creado_en"));
		e.terminadoEn = texto(rs.getString("terminado_en"));
		return (e);
	}

	private static void ponerDesde(final PreparedStatement ps, final int desde,
			final ArrayList<Object> valores) throws Exception {
		for (int i = 0; i < valores.size(); i++) {
			final Object v = valores.get(i);
			if (v instanceof Integer) {
				ps.setInt(desde + i, ((Integer) v).intValue());
			} else if (v instanceof Double) {
				ps.setDouble(desde + i, ((Double) v).doubleValue());
			} else {
				ps.setString(desde + i, String.valueOf(v));
			}
		}
	}

	private static String recortar(final String valor, final int largo) {
		if (valor == null) {
			return (null);
		}
		return (valor.length() > largo ? valor.substring(0, largo) : valor);
	}

	private static String texto(final String valor) {
		return (valor == null ? "" : valor);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("CampanaDAO: no cerro la conexion, " + e.toString());
		}
	}
}
