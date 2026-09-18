package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * La vista 360 de una persona.
 *
 * QUE ES UNA PERSONA Y POR QUE NO ES UN CLIENTE
 *
 * pizzaamericana.cliente tiene una fila por cada vez que alguien tomo los datos.
 * La misma persona esta en 1,21 filas en promedio y hay quien esta en decenas:
 * Juan Santa, la persona 141999, aparece en 28 filas del central y 36 de
 * tienda. Consultando por idcliente se ve un pedacito de su historia.
 *
 * El maestro crm.persona ya resolvio quien es quien -450.002 personas reales al
 * 2026-09-18-. Todo lo de aqui se consulta por idpersona y junta lo de TODAS
 * sus filas de cliente.
 *
 * DE DONDE SALEN LAS CIFRAS
 *
 * El resumen sale de la vista crm.v_persona_360, que se creo en el paso 4 del
 * plan. Las listas -pedidos, ofertas, PQRS, las caras- se consultan aqui porque
 * una vista no puede devolver listas.
 *
 * LA TRAMPA DEL NUMERO DE PEDIDO
 *
 * encuesta_servicio.idpedido y resultado_ruleta.idpedido NO son
 * pizzaamericana.pedido.idpedido: son el numero del pedido EN LA TIENDA y solo
 * son unicos junto con idtienda. Cruzar por idpedido no falla, devuelve CERO,
 * que es peor. Se cruza por (idtienda, numposheader).
 */
public class Persona360DAO {

	/** Cuantos pedidos se traen al detalle. Mas que esto no se alcanza a mirar. */
	private static final int TOPE_PEDIDOS = 30;

	/** Tope de resultados de una busqueda, para que un apellido comun no traiga miles. */
	private static final int TOPE_BUSQUEDA = 50;

	// =======================================================================
	// Lo que se devuelve
	// =======================================================================

	/** Una persona en la lista de resultados de la busqueda. */
	public static class Resumen {
		public long idPersona;
		public String nombre = "";
		public String apellido = "";
		public String celular = "";
		public String email = "";
		public int pedidos;
		public String ultimoPedido = "";
	}

	/** El 360 completo. */
	public static class Detalle {
		public long idPersona;
		public String nombre = "";
		public String apellido = "";
		public String celular = "";
		public String email = "";
		public String politicaDatos = "";
		public String origen = "";
		public int filasCentral;
		public int filasTienda;
		public int tiendas;
		public int pedidos;
		public double valorComprado;
		public String primerPedido = "";
		public String ultimoPedido = "";
		public int pedidosCancelados;
		public int tiendaHabitual;
		public int ofertasAsignadas;
		public int ofertasUsadas;
		public int pqrs;
		public String ultimaPqrs = "";
		public int encuestas;
		public int girosRuleta;
		public double puntos;
	}

	/** Una fila de cliente: una de las "caras" de la persona. */
	public static class Cara {
		public int idCliente;
		public int idTienda;
		public String nombre = "";
		public String apellido = "";
		public String telefono = "";
		public String celular = "";
		public String email = "";
		public String direccion = "";
		public String origen = "";
	}

	public static class Pedido {
		public long idPedido;
		public int idTienda;
		public String fecha = "";
		public double valor;
		public String tipo = "";
		public boolean cancelado;
	}

	public static class Oferta {
		public String oferta = "";
		public String fechaIngreso = "";
		public String utilizada = "";
		public String fechaUso = "";
		public String codigo = "";
	}

	public static class Pqrs {
		public long idSolicitud;
		public String fecha = "";
		public int idTienda;
		public String tipo = "";
		public String comentario = "";
		public String estado = "";
	}

	// =======================================================================
	// Buscar
	// =======================================================================

	/**
	 * Busca personas.
	 *
	 * Decide sola por donde buscar segun lo que escribieron: si son puros
	 * digitos es un celular, si trae arroba es un correo, si no es un nombre.
	 * Preguntarle al usuario "por que campo quiere buscar" es pedirle que haga
	 * el trabajo del programa.
	 *
	 * El celular se normaliza igual que el maestro -se quitan los no digitos,
	 * el indicativo 57 y los ceros de marcacion- para que dar con la persona no
	 * dependa de como escribieron el numero.
	 *
	 * Por nombre la busqueda es POR EL COMIENZO. Buscar por el medio
	 * -LIKE '%x%'- no puede usar indice y recorreria las 457 mil filas en cada
	 * tecla.
	 */
	public static ArrayList<Resumen> buscar(final String texto) {
		final ArrayList<Resumen> lista = new ArrayList<Resumen>();
		if (texto == null || texto.trim().length() < 3) {
			return (lista);
		}
		final String limpio = texto.trim();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final String campo;
			String valor;
			if (limpio.indexOf('@') > 0) {
				campo = " p.email = ? ";
				valor = limpio.toLowerCase();
			} else if (soloDigitos(limpio)) {
				campo = " p.celular_norm = ? ";
				valor = normalizarCelular(limpio);
				if (valor == null) {
					return (lista);
				}
			} else {
				campo = " (p.nombre LIKE ? OR p.apellido LIKE ?) ";
				valor = limpio;
			}

			final String sql = "SELECT p.idpersona, IFNULL(p.nombre,'') AS nombre,"
					+ " IFNULL(p.apellido,'') AS apellido, IFNULL(p.celular_norm,'') AS celular,"
					+ " IFNULL(p.email,'') AS email,"
					+ " (SELECT COUNT(*) FROM pedido pe"
					+ "   WHERE pe.idcliente IN (SELECT c.idcliente FROM cliente c"
					+ "                           WHERE c.idpersona = p.idpersona)"
					+ "     AND pe.fecha_cancelacion IS NULL) AS pedidos,"
					+ " (SELECT MAX(pe.fechapedido) FROM pedido pe"
					+ "   WHERE pe.idcliente IN (SELECT c.idcliente FROM cliente c"
					+ "                           WHERE c.idpersona = p.idpersona)"
					+ "     AND pe.fecha_cancelacion IS NULL) AS ultimo"
					+ " FROM crm.persona p"
					+ " WHERE p.activa = 'S' AND p.idpersona_principal IS NULL AND" + campo
					+ " ORDER BY p.nombre, p.apellido LIMIT " + TOPE_BUSQUEDA;

			final PreparedStatement ps = cn.prepareStatement(sql);
			if (campo.indexOf("LIKE") >= 0) {
				ps.setString(1, valor + "%");
				ps.setString(2, valor + "%");
			} else {
				ps.setString(1, valor);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Resumen r = new Resumen();
				r.idPersona = rs.getLong("idpersona");
				r.nombre = texto(rs.getString("nombre"));
				r.apellido = texto(rs.getString("apellido"));
				r.celular = texto(rs.getString("celular"));
				r.email = texto(rs.getString("email"));
				r.pedidos = rs.getInt("pedidos");
				r.ultimoPedido = texto(rs.getString("ultimo"));
				lista.add(r);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.buscar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// =======================================================================
	// El detalle
	// =======================================================================

	public static Detalle obtener(final long idPersona) {
		Detalle d = null;
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			//La vista resuelve el alias: si se consulta por una persona que
			//resulto ser la misma que otra, responde lo de la principal.
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT * FROM crm.v_persona_360 WHERE idpersona ="
					+ " IFNULL((SELECT x.idpersona_principal FROM crm.persona x"
					+ "          WHERE x.idpersona = ?), ?)");
			ps.setLong(1, idPersona);
			ps.setLong(2, idPersona);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				d = new Detalle();
				d.idPersona = rs.getLong("idpersona");
				d.nombre = texto(rs.getString("nombre"));
				d.apellido = texto(rs.getString("apellido"));
				d.celular = texto(rs.getString("celular"));
				d.email = texto(rs.getString("email"));
				d.politicaDatos = texto(rs.getString("politica_datos"));
				d.origen = texto(rs.getString("origen"));
				d.filasCentral = rs.getInt("filas_cliente_central");
				d.filasTienda = rs.getInt("filas_cliente_tienda");
				d.tiendas = rs.getInt("tiendas_donde_esta");
				d.pedidos = rs.getInt("pedidos");
				d.valorComprado = rs.getDouble("valor_comprado");
				d.primerPedido = texto(rs.getString("primer_pedido"));
				d.ultimoPedido = texto(rs.getString("ultimo_pedido"));
				d.pedidosCancelados = rs.getInt("pedidos_cancelados");
				d.tiendaHabitual = rs.getInt("tienda_habitual");
				d.ofertasAsignadas = rs.getInt("ofertas_asignadas");
				d.ofertasUsadas = rs.getInt("ofertas_usadas");
				d.pqrs = rs.getInt("pqrs");
				d.ultimaPqrs = texto(rs.getString("ultima_pqrs"));
				d.encuestas = rs.getInt("encuestas_respondidas");
				d.girosRuleta = rs.getInt("giros_ruleta");
				d.puntos = rs.getDouble("puntos_vigentes");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.obtener: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (d);
	}

	/**
	 * Las filas de cliente de la persona, del central y de las tiendas.
	 *
	 * Es la parte que mas explica: ver que "una sola persona" son treinta filas
	 * con el nombre escrito de treinta formas es lo que hace entender por que
	 * hacia falta el maestro.
	 */
	public static ArrayList<Cara> caras(final long idPersona) {
		final ArrayList<Cara> lista = new ArrayList<Cara>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			PreparedStatement ps = cn.prepareStatement(
					"SELECT c.idcliente, c.idtienda, IFNULL(c.nombre,'') AS nombre,"
					+ " IFNULL(c.apellido,'') AS apellido, IFNULL(c.telefono,'') AS telefono,"
					+ " IFNULL(c.telefono_celular,'') AS celular, IFNULL(c.email,'') AS email,"
					+ " IFNULL(c.direccion,'') AS direccion"
					+ " FROM cliente c WHERE c.idpersona = ? ORDER BY c.idcliente DESC");
			ps.setLong(1, idPersona);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Cara c = new Cara();
				c.idCliente = rs.getInt("idcliente");
				c.idTienda = rs.getInt("idtienda");
				c.nombre = texto(rs.getString("nombre"));
				c.apellido = texto(rs.getString("apellido"));
				c.telefono = texto(rs.getString("telefono"));
				c.celular = texto(rs.getString("celular"));
				c.email = texto(rs.getString("email"));
				c.direccion = texto(rs.getString("direccion"));
				c.origen = "CENTRAL";
				lista.add(c);
			}
			rs.close();
			ps.close();

			ps = cn.prepareStatement(
					"SELECT s.idcliente, s.idtienda, IFNULL(s.nombre,'') AS nombre,"
					+ " IFNULL(s.apellido,'') AS apellido, IFNULL(s.celular_norm,'') AS celular,"
					+ " IFNULL(s.email,'') AS email, IFNULL(s.como_se_pego,'') AS como"
					+ " FROM crm.stage_cliente_tienda s WHERE s.idpersona = ?"
					+ " ORDER BY s.idtienda, s.idcliente DESC");
			ps.setLong(1, idPersona);
			rs = ps.executeQuery();
			while (rs.next()) {
				final Cara c = new Cara();
				c.idCliente = rs.getInt("idcliente");
				c.idTienda = rs.getInt("idtienda");
				c.nombre = texto(rs.getString("nombre"));
				c.apellido = texto(rs.getString("apellido"));
				c.celular = texto(rs.getString("celular"));
				c.email = texto(rs.getString("email"));
				c.origen = "TIENDA (" + texto(rs.getString("como")) + ")";
				lista.add(c);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.caras: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	public static ArrayList<Pedido> pedidos(final long idPersona) {
		final ArrayList<Pedido> lista = new ArrayList<Pedido>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT pe.idpedido, pe.idtienda, pe.fechapedido, pe.total_neto,"
					+ " IFNULL(tp.nombre,'') AS tipo, pe.fecha_cancelacion"
					+ " FROM pedido pe"
					+ " LEFT JOIN tipo_pedido tp ON tp.idtipopedido = pe.idtipopedido"
					+ " WHERE pe.idcliente IN (SELECT c.idcliente FROM cliente c WHERE c.idpersona = ?)"
					+ " ORDER BY pe.fechapedido DESC, pe.idpedido DESC LIMIT " + TOPE_PEDIDOS);
			ps.setLong(1, idPersona);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Pedido p = new Pedido();
				p.idPedido = rs.getLong("idpedido");
				p.idTienda = rs.getInt("idtienda");
				p.fecha = texto(rs.getString("fechapedido"));
				p.valor = rs.getDouble("total_neto");
				p.tipo = texto(rs.getString("tipo"));
				p.cancelado = (rs.getString("fecha_cancelacion") != null);
				lista.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.pedidos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	public static ArrayList<Oferta> ofertas(final long idPersona) {
		final ArrayList<Oferta> lista = new ArrayList<Oferta>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(o.nombre_oferta,'') AS oferta, oc.ingreso_oferta, oc.utilizada,"
					+ " oc.uso_oferta, IFNULL(oc.codigo_promocion,'') AS codigo"
					+ " FROM oferta_cliente oc"
					+ " LEFT JOIN oferta o ON o.idoferta = oc.idoferta"
					+ " WHERE oc.idcliente IN (SELECT c.idcliente FROM cliente c WHERE c.idpersona = ?)"
					+ " ORDER BY oc.ingreso_oferta DESC");
			ps.setLong(1, idPersona);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Oferta o = new Oferta();
				o.oferta = texto(rs.getString("oferta"));
				o.fechaIngreso = texto(rs.getString("ingreso_oferta"));
				o.utilizada = texto(rs.getString("utilizada"));
				o.fechaUso = texto(rs.getString("uso_oferta"));
				o.codigo = texto(rs.getString("codigo"));
				lista.add(o);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.ofertas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	public static ArrayList<Pqrs> pqrs(final long idPersona) {
		final ArrayList<Pqrs> lista = new ArrayList<Pqrs>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT q.idsolicitudPQRS, q.fechasolicitud, q.idtienda,"
					+ " IFNULL(q.tiposolicitud,'') AS tipo, IFNULL(q.comentario,'') AS comentario,"
					+ " IFNULL(ep.descripcion, CONCAT('Estado ', IFNULL(q.idestado,0))) AS estado"
					+ " FROM solicitudPQRS q"
					+ " LEFT JOIN estado_pqrs ep ON ep.idestado = q.idestado"
					+ " WHERE q.idcliente IN (SELECT c.idcliente FROM cliente c WHERE c.idpersona = ?)"
					+ " ORDER BY q.fechasolicitud DESC");
			ps.setLong(1, idPersona);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Pqrs q = new Pqrs();
				q.idSolicitud = rs.getLong("idsolicitudPQRS");
				q.fecha = texto(rs.getString("fechasolicitud"));
				q.idTienda = rs.getInt("idtienda");
				q.tipo = texto(rs.getString("tipo"));
				q.comentario = texto(rs.getString("comentario"));
				q.estado = texto(rs.getString("estado"));
				lista.add(q);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.pqrs: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	// =======================================================================
	// El estado del maestro
	// =======================================================================

	/** Las cifras de la pagina de inicio del CRM. */
	public static class Resumen360 {
		public int personas;
		public int alias;
		public int centralColgados;
		public int centralSueltos;
		public int tiendaColgados;
		public int tiendaSueltos;
		public String ultimaPersona = "";
	}

	/**
	 * El estado del maestro, para la pagina de inicio.
	 *
	 * La fecha de la ultima persona no es adorno: el maestro lo mantiene al dia
	 * un proceso nocturno, y si esa fecha no es de hoy o de ayer es que el
	 * proceso no esta corriendo y el CRM esta mirando una foto vieja. Entre el
	 * 15 y el 18 de septiembre de 2026 estuvo tres dias quieto y nadie se
	 * habria enterado si no se mira.
	 */
	public static Resumen360 resumen() {
		final Resumen360 r = new Resumen360();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT (SELECT COUNT(*) FROM crm.persona"
					+ "        WHERE activa='S' AND idpersona_principal IS NULL) AS personas,"
					+ " (SELECT COUNT(*) FROM crm.persona WHERE idpersona_principal IS NOT NULL) AS alias,"
					+ " (SELECT COUNT(*) FROM cliente WHERE idpersona IS NOT NULL) AS central_ok,"
					+ " (SELECT COUNT(*) FROM cliente WHERE idpersona IS NULL) AS central_suelto,"
					+ " (SELECT COUNT(*) FROM crm.stage_cliente_tienda WHERE idpersona IS NOT NULL) AS tienda_ok,"
					+ " (SELECT COUNT(*) FROM crm.stage_cliente_tienda WHERE idpersona IS NULL) AS tienda_suelto,"
					+ " (SELECT MAX(creado_en) FROM crm.persona) AS ultima");
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				r.personas = rs.getInt("personas");
				r.alias = rs.getInt("alias");
				r.centralColgados = rs.getInt("central_ok");
				r.centralSueltos = rs.getInt("central_suelto");
				r.tiendaColgados = rs.getInt("tienda_ok");
				r.tiendaSueltos = rs.getInt("tienda_suelto");
				r.ultimaPersona = texto(rs.getString("ultima"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("Persona360DAO.resumen: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	// =======================================================================
	// Plomeria
	// =======================================================================

	/**
	 * Misma regla del maestro: solo celular colombiano de diez digitos que
	 * empiece por 3. Lo que no cumpla devuelve null y no se adivina.
	 */
	public static String normalizarCelular(final String valor) {
		if (valor == null) {
			return (null);
		}
		final StringBuilder digitos = new StringBuilder();
		for (int i = 0; i < valor.length(); i++) {
			final char c = valor.charAt(i);
			if (c >= '0' && c <= '9') {
				digitos.append(c);
			}
		}
		String n = digitos.toString();
		//Ceros de marcacion e indicativo de pais.
		while (n.startsWith("0")) {
			n = n.substring(1);
		}
		if (n.length() == 12 && n.startsWith("57")) {
			n = n.substring(2);
		}
		if (n.length() == 10 && n.charAt(0) == '3') {
			return (n);
		}
		return (null);
	}

	private static boolean soloDigitos(final String valor) {
		int digitos = 0;
		for (int i = 0; i < valor.length(); i++) {
			final char c = valor.charAt(i);
			if (c >= '0' && c <= '9') {
				digitos++;
			} else if (c != ' ' && c != '-' && c != '+' && c != '(' && c != ')') {
				return (false);
			}
		}
		return (digitos >= 7);
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
			Logger.getLogger("log_file").error("Persona360DAO: no cerro la conexion, " + e.toString());
		}
	}
}
