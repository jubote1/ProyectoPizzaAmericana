package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Las marcas de cliente: el texto de alerta que se muestra al elegir al cliente
 * (cliente_alerta) y los "habilitadores" que se le prenden (cliente_marca, con su
 * catalogo parametrico cliente_marca_tipo). Ver sql/2026_10_25_01_cliente_marcas.sql.
 *
 * TODO VA POR TELEFONO, SOLO DIGITOS
 *
 * Un "cliente" del sistema es telefono mas direccion y puede tener varias filas; la
 * persona es la misma. cliente_alerta ya estaba por telefono y las marcas siguen ese
 * criterio. Todos los metodos normalizan el telefono a solo digitos antes de usarlo,
 * asi "311 234-5678" y "3112345678" son el mismo cliente.
 *
 * DOS MANERAS DE FALLAR, A PROPOSITO DISTINTAS
 *
 *  - Lo que lee el que toma el pedido (obtenerFichaPublica) y la consulta de bloqueo de
 *    Rappi Cargo NUNCA lanzan excepcion: si las tablas nuevas todavia no existen (el
 *    WAR se desplego antes de correr el SQL) el cliente se atiende igual con su texto de
 *    siempre. Tomar un pedido no puede depender de esta funcionalidad.
 *  - Lo que usa la pantalla de administracion SI lanza SQLException, para que la pantalla
 *    pueda decir "falta correr el script" en vez de mostrar una lista vacia que parece
 *    verdadera.
 *
 * Todas las consultas van con parametros: la lectura anterior de cliente_alerta armaba el
 * SQL concatenando el telefono.
 */
public class ClienteMarcaDAO {

	/** Un habilitador. Cuando viene de cliente_marca trae ademas quien y cuando lo marco. */
	public static class Tipo {
		public String idtipo = "";
		public String nombre = "";
		public String descripcion = "";
		/** N negativa, I informativa, P positiva. */
		public String severidad = "I";
		/** S: el sistema no deja enviar el pedido por Rappi Cargo. */
		public String bloqueaCargo = "N";
		public String activo = "S";
		public int orden = 10;
		public String fechaMarca = "";
		public String usuario = "";
	}

	/** Lo que hay guardado de un telefono. */
	public static class Ficha {
		public String telefono = "";
		public String tipoAlerta = "";
		public String mensaje = "";
		public String usuarioModifica = "";
		public String fechaModifica = "";
		public boolean existe = false;
		public ArrayList<Tipo> marcas = new ArrayList<Tipo>();
	}

	/** Una fila de la lista de clientes marcados. */
	public static class Fila {
		public String telefono = "";
		public String tipoAlerta = "";
		public String mensaje = "";
		public String idsMarcas = "";
		public String usuario = "";
		public String fecha = "";
	}

	/** Quien es el cliente detras de un telefono, para que quien marca sepa a quien marca. */
	public static class Persona {
		public String nombre = "";
		public String direccion = "";
	}

	public static class Evento {
		public String fecha = "";
		public String usuario = "";
		public String accion = "";
		public String detalle = "";
	}

	/** Lo que bloquea Rappi Cargo para un cliente: los habilitadores y el texto que dejo quien lo marco. */
	public static class Bloqueo {
		public ArrayList<String> nombres = new ArrayList<String>();
		public String texto = "";
	}

	/** Longitud maxima de cliente_alerta.mensaje. */
	public static final int MAXIMO_MENSAJE = 2000;

	// -----------------------------------------------------------------------
	// Utilidades
	// -----------------------------------------------------------------------

	/** Solo los digitos del telefono; vacio si no queda nada. */
	public static String normalizarTelefono(final String telefono) {
		if (telefono == null) {
			return ("");
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < telefono.length(); i++) {
			final char c = telefono.charAt(i);
			if (c >= '0' && c <= '9') {
				sb.append(c);
			}
		}
		return (sb.toString());
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ClienteMarcaDAO: cerrando conexion " + e.toString());
		}
	}

	private static Tipo leerTipo(final ResultSet rs) throws SQLException {
		final Tipo t = new Tipo();
		t.idtipo = rs.getString("idtipo");
		t.nombre = rs.getString("nombre");
		t.descripcion = rs.getString("descripcion") == null ? "" : rs.getString("descripcion");
		t.severidad = rs.getString("severidad");
		t.bloqueaCargo = rs.getString("bloquea_cargo");
		t.activo = rs.getString("activo");
		t.orden = rs.getInt("orden");
		return (t);
	}

	private static final String COLUMNAS_TIPO = "t.idtipo, t.nombre, t.descripcion, t.severidad, t.bloquea_cargo, t.activo, t.orden";

	/** Texto sin tildes y con guiones bajos, para armar el identificador de un habilitador nuevo. */
	private static String identificador(final String nombre) {
		final String sinTildes = java.text.Normalizer.normalize(nombre == null ? "" : nombre, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		String id = sinTildes.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (id.length() > 24) {
			id = id.substring(0, 24).replaceAll("_+$", "");
		}
		return (id.length() == 0 ? "MARCA" : id);
	}

	// -----------------------------------------------------------------------
	// Lectura
	// -----------------------------------------------------------------------

	/** Los habilitadores. Lanza SQLException (lo usa la pantalla de administracion). */
	public static ArrayList<Tipo> listarTipos(final boolean soloActivos) throws SQLException {
		final ArrayList<Tipo> tipos = new ArrayList<Tipo>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement("SELECT " + COLUMNAS_TIPO + " FROM cliente_marca_tipo t"
					+ (soloActivos ? " WHERE t.activo = 'S'" : "") + " ORDER BY t.orden, t.nombre");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				tipos.add(leerTipo(rs));
			}
			rs.close();
			ps.close();
		} finally {
			cerrar(cn);
		}
		return (tipos);
	}

	/**
	 * Lo que se le muestra a quien toma el pedido: el texto y los habilitadores ACTIVOS.
	 * Nunca lanza excepcion (ver la nota de la clase).
	 */
	public static Ficha obtenerFichaPublica(final String telefono) {
		final Ficha ficha = new Ficha();
		ficha.telefono = normalizarTelefono(telefono);
		if (ficha.telefono.length() == 0) {
			return (ficha);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement("SELECT tipo_alerta, mensaje FROM cliente_alerta WHERE telefono = ?");
			ps.setString(1, ficha.telefono);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ficha.existe = true;
				ficha.tipoAlerta = rs.getString(1) == null ? "" : rs.getString(1);
				ficha.mensaje = rs.getString(2) == null ? "" : rs.getString(2);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ClienteMarcaDAO.obtenerFichaPublica texto: " + e.toString());
		}
		try {
			if (cn != null) {
				ficha.marcas = marcasDe(cn, ficha.telefono, true);
			}
		} catch (final Exception e) {
			//Las tablas nuevas pueden no existir todavia: se sigue con el texto.
			Logger.getLogger("log_file").error("ClienteMarcaDAO.obtenerFichaPublica marcas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (ficha);
	}

	/** Lo mismo que la ficha publica, pero con todos los habilitadores y con quien toco el texto. Lanza SQLException. */
	public static Ficha obtenerFichaAdministracion(final String telefono) throws SQLException {
		final Ficha ficha = new Ficha();
		ficha.telefono = normalizarTelefono(telefono);
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT tipo_alerta, mensaje, IFNULL(usuario_modifica,''), IFNULL(DATE_FORMAT(fecha_modifica,'%Y-%m-%d %H:%i'),'')"
							+ " FROM cliente_alerta WHERE telefono = ?");
			ps.setString(1, ficha.telefono);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ficha.existe = true;
				ficha.tipoAlerta = rs.getString(1) == null ? "" : rs.getString(1);
				ficha.mensaje = rs.getString(2) == null ? "" : rs.getString(2);
				ficha.usuarioModifica = rs.getString(3);
				ficha.fechaModifica = rs.getString(4);
			}
			rs.close();
			ps.close();
			ficha.marcas = marcasDe(cn, ficha.telefono, false);
		} finally {
			cerrar(cn);
		}
		return (ficha);
	}

	private static ArrayList<Tipo> marcasDe(final Connection cn, final String telefono, final boolean soloActivas) throws SQLException {
		final ArrayList<Tipo> marcas = new ArrayList<Tipo>();
		final PreparedStatement ps = cn.prepareStatement("SELECT " + COLUMNAS_TIPO
				+ ", IFNULL(DATE_FORMAT(m.fecha_marca,'%Y-%m-%d %H:%i'),'') AS fecha_marca, m.usuario"
				+ " FROM cliente_marca m INNER JOIN cliente_marca_tipo t ON t.idtipo = m.idtipo WHERE m.telefono = ?"
				+ (soloActivas ? " AND t.activo = 'S'" : "") + " ORDER BY t.orden, t.nombre");
		ps.setString(1, telefono);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final Tipo t = leerTipo(rs);
			t.fechaMarca = rs.getString("fecha_marca");
			t.usuario = rs.getString("usuario");
			marcas.add(t);
		}
		rs.close();
		ps.close();
		return (marcas);
	}

	/** Los clientes que tienen texto o marcas. Lanza SQLException. */
	public static ArrayList<Fila> listar(final String filtro, final String idtipo, final int limite) throws SQLException {
		final ArrayList<Fila> filas = new ArrayList<Fila>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final String f = filtro == null ? "" : filtro.trim();
			final String t = idtipo == null ? "" : idtipo.trim();
			final StringBuilder sql = new StringBuilder(
					"SELECT a.telefono, IFNULL(a.tipo_alerta,''), IFNULL(a.mensaje,''), IFNULL(a.usuario_modifica,''),"
							+ " IFNULL(DATE_FORMAT(a.fecha_modifica,'%Y-%m-%d %H:%i'),''),"
							+ " (SELECT GROUP_CONCAT(m.idtipo ORDER BY tt.orden SEPARATOR ',') FROM cliente_marca m"
							+ "   INNER JOIN cliente_marca_tipo tt ON tt.idtipo = m.idtipo WHERE m.telefono = a.telefono) AS ids"
							+ " FROM cliente_alerta a WHERE 1 = 1");
			if (f.length() > 0) {
				sql.append(" AND (a.telefono LIKE ? OR a.mensaje LIKE ?)");
			}
			if (t.length() > 0) {
				sql.append(" AND EXISTS (SELECT 1 FROM cliente_marca m2 WHERE m2.telefono = a.telefono AND m2.idtipo = ?)");
			}
			sql.append(" ORDER BY IFNULL(a.fecha_modifica, '1970-01-01') DESC, a.telefono LIMIT ?");
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			int i = 1;
			if (f.length() > 0) {
				ps.setString(i++, "%" + f + "%");
				ps.setString(i++, "%" + f + "%");
			}
			if (t.length() > 0) {
				ps.setString(i++, t);
			}
			ps.setInt(i, limite <= 0 ? 200 : Math.min(limite, 500));
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Fila fila = new Fila();
				fila.telefono = rs.getString(1);
				fila.tipoAlerta = rs.getString(2);
				fila.mensaje = rs.getString(3);
				fila.usuario = rs.getString(4);
				fila.fecha = rs.getString(5);
				fila.idsMarcas = rs.getString(6) == null ? "" : rs.getString(6);
				filas.add(fila);
			}
			rs.close();
			ps.close();
		} finally {
			cerrar(cn);
		}
		return (filas);
	}

	/**
	 * Quien es el cliente detras del telefono (hasta cinco, los mas recientes). Solo para que
	 * quien marca confirme que es la persona correcta; si la consulta falla devuelve vacio.
	 */
	public static ArrayList<Persona> personasDeTelefono(final String telefono) {
		final ArrayList<Persona> personas = new ArrayList<Persona>();
		final String tel = normalizarTelefono(telefono);
		if (tel.length() == 0) {
			return (personas);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT CONCAT(IFNULL(nombre,''), ' ', IFNULL(apellido,'')) AS nombre, IFNULL(direccion,'') AS direccion"
							+ " FROM cliente WHERE telefono = ? OR telefono_celular = ? ORDER BY idcliente DESC LIMIT 5");
			ps.setString(1, tel);
			ps.setString(2, tel);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Persona p = new Persona();
				p.nombre = rs.getString("nombre").trim();
				p.direccion = rs.getString("direccion");
				personas.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ClienteMarcaDAO.personasDeTelefono: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (personas);
	}

	/** El historial de un telefono, lo mas reciente primero. Lanza SQLException. */
	public static ArrayList<Evento> historial(final String telefono, final int limite) throws SQLException {
		final ArrayList<Evento> eventos = new ArrayList<Evento>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT DATE_FORMAT(fecha,'%Y-%m-%d %H:%i'), usuario, accion, detalle FROM cliente_marca_log"
							+ " WHERE telefono = ? ORDER BY id DESC LIMIT ?");
			ps.setString(1, normalizarTelefono(telefono));
			ps.setInt(2, limite <= 0 ? 50 : Math.min(limite, 200));
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Evento e = new Evento();
				e.fecha = rs.getString(1);
				e.usuario = rs.getString(2);
				e.accion = rs.getString(3);
				e.detalle = rs.getString(4);
				eventos.add(e);
			}
			rs.close();
			ps.close();
		} finally {
			cerrar(cn);
		}
		return (eventos);
	}

	// -----------------------------------------------------------------------
	// Escritura (todo en una transaccion; lanza SQLException)
	// -----------------------------------------------------------------------

	/**
	 * Deja el telefono con exactamente estos habilitadores y este texto. Si no queda ni
	 * texto ni habilitadores, se borra el registro (equivale a quitar). Devuelve
	 * "GUARDADO" o "QUITADO".
	 */
	public static String guardar(final String telefono, final String mensaje, final ArrayList<String> idsTipos, final String usuario)
			throws SQLException {
		final String tel = normalizarTelefono(telefono);
		final String texto = mensaje == null ? "" : mensaje.trim();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);

			//Que habilitadores se piden: solo los que existen, sin repetir.
			final ArrayList<Tipo> pedidos = new ArrayList<Tipo>();
			final java.util.HashSet<String> vistos = new java.util.HashSet<String>();
			for (final String id : idsTipos) {
				final String limpio = id == null ? "" : id.trim();
				if (limpio.length() == 0 || !vistos.add(limpio)) {
					continue;
				}
				final PreparedStatement pt = cn.prepareStatement("SELECT " + COLUMNAS_TIPO + " FROM cliente_marca_tipo t WHERE t.idtipo = ?");
				pt.setString(1, limpio);
				final ResultSet rt = pt.executeQuery();
				if (rt.next()) {
					pedidos.add(leerTipo(rt));
				}
				rt.close();
				pt.close();
			}

			final String antes = descripcion(cn, tel);

			if (pedidos.isEmpty() && texto.length() == 0) {
				borrar(cn, tel);
				registrar(cn, usuario, tel, "QUITAR", "antes: " + antes);
				cn.commit();
				return ("QUITADO");
			}

			//El tipo de alerta es lo que pinta el icono en el web: negativa si alguno es grave.
			String existente = "";
			final PreparedStatement pe = cn.prepareStatement("SELECT IFNULL(tipo_alerta,'') FROM cliente_alerta WHERE telefono = ?");
			pe.setString(1, tel);
			final ResultSet re = pe.executeQuery();
			if (re.next()) {
				existente = re.getString(1);
			}
			re.close();
			pe.close();
			boolean negativa = false;
			boolean positiva = false;
			for (final Tipo t : pedidos) {
				negativa = negativa || "N".equals(t.severidad);
				positiva = positiva || "P".equals(t.severidad);
			}
			String tipoAlerta;
			if (negativa) {
				tipoAlerta = "ALERTA_NEGATIVA";
			} else if (positiva) {
				tipoAlerta = "ALERTA_POSITIVA";
			} else if (pedidos.isEmpty() && existente.length() > 0) {
				tipoAlerta = existente;
			} else {
				tipoAlerta = "ALERTA_INFORMATIVA";
			}

			final PreparedStatement pa = cn.prepareStatement(
					"INSERT INTO cliente_alerta (telefono, tipo_alerta, mensaje, usuario_modifica, fecha_modifica) VALUES (?,?,?,?,NOW())"
							+ " ON DUPLICATE KEY UPDATE tipo_alerta = VALUES(tipo_alerta), mensaje = VALUES(mensaje),"
							+ " usuario_modifica = VALUES(usuario_modifica), fecha_modifica = VALUES(fecha_modifica)");
			pa.setString(1, tel);
			pa.setString(2, tipoAlerta);
			pa.setString(3, texto);
			pa.setString(4, usuario);
			pa.executeUpdate();
			pa.close();

			//Habilitadores: se quitan los que ya no estan y se agregan los nuevos (los que siguen conservan quien y cuando).
			final PreparedStatement pq = cn.prepareStatement("SELECT idtipo FROM cliente_marca WHERE telefono = ?");
			pq.setString(1, tel);
			final ResultSet rq = pq.executeQuery();
			final java.util.HashSet<String> actuales = new java.util.HashSet<String>();
			while (rq.next()) {
				actuales.add(rq.getString(1));
			}
			rq.close();
			pq.close();
			for (final String actual : actuales) {
				if (!vistos.contains(actual)) {
					final PreparedStatement pd = cn.prepareStatement("DELETE FROM cliente_marca WHERE telefono = ? AND idtipo = ?");
					pd.setString(1, tel);
					pd.setString(2, actual);
					pd.executeUpdate();
					pd.close();
				}
			}
			for (final Tipo t : pedidos) {
				if (!actuales.contains(t.idtipo)) {
					final PreparedStatement pi = cn.prepareStatement(
							"INSERT INTO cliente_marca (telefono, idtipo, fecha_marca, usuario) VALUES (?,?,NOW(),?)");
					pi.setString(1, tel);
					pi.setString(2, t.idtipo);
					pi.setString(3, usuario);
					pi.executeUpdate();
					pi.close();
				}
			}
			registrar(cn, usuario, tel, "GUARDAR", "antes: " + antes + " || despues: " + descripcion(cn, tel));
			cn.commit();
			return ("GUARDADO");
		} catch (final SQLException e) {
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception e2) {
				//nada que hacer
			}
			throw e;
		} finally {
			cerrar(cn);
		}
	}

	/** Quita el texto y todos los habilitadores del telefono. Devuelve true si habia algo. */
	public static boolean quitar(final String telefono, final String usuario) throws SQLException {
		final String tel = normalizarTelefono(telefono);
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);
			final String antes = descripcion(cn, tel);
			final boolean habia = borrar(cn, tel);
			if (habia) {
				registrar(cn, usuario, tel, "QUITAR", "antes: " + antes);
			}
			cn.commit();
			return (habia);
		} catch (final SQLException e) {
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception e2) {
				//nada que hacer
			}
			throw e;
		} finally {
			cerrar(cn);
		}
	}

	private static boolean borrar(final Connection cn, final String tel) throws SQLException {
		final PreparedStatement p1 = cn.prepareStatement("DELETE FROM cliente_marca WHERE telefono = ?");
		p1.setString(1, tel);
		final int a = p1.executeUpdate();
		p1.close();
		final PreparedStatement p2 = cn.prepareStatement("DELETE FROM cliente_alerta WHERE telefono = ?");
		p2.setString(1, tel);
		final int b = p2.executeUpdate();
		p2.close();
		return (a + b > 0);
	}

	/** Una linea con lo que tiene el telefono, para el historial. */
	private static String descripcion(final Connection cn, final String tel) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		final PreparedStatement ps = cn.prepareStatement(
				"SELECT GROUP_CONCAT(idtipo ORDER BY idtipo SEPARATOR ',') FROM cliente_marca WHERE telefono = ?");
		ps.setString(1, tel);
		ResultSet rs = ps.executeQuery();
		String marcas = "";
		if (rs.next() && rs.getString(1) != null) {
			marcas = rs.getString(1);
		}
		rs.close();
		ps.close();
		final PreparedStatement pt = cn.prepareStatement("SELECT IFNULL(mensaje,'') FROM cliente_alerta WHERE telefono = ?");
		pt.setString(1, tel);
		rs = pt.executeQuery();
		String texto = "";
		if (rs.next()) {
			texto = rs.getString(1);
		}
		rs.close();
		pt.close();
		sb.append("marcas=[").append(marcas).append("] texto=\"").append(texto.length() > 400 ? texto.substring(0, 400) + "..." : texto).append("\"");
		return (sb.toString());
	}

	private static void registrar(final Connection cn, final String usuario, final String tel, final String accion, final String detalle)
			throws SQLException {
		final PreparedStatement ps = cn.prepareStatement(
				"INSERT INTO cliente_marca_log (fecha, usuario, telefono, accion, detalle) VALUES (NOW(),?,?,?,?)");
		ps.setString(1, usuario == null ? "" : usuario);
		ps.setString(2, tel);
		ps.setString(3, accion);
		ps.setString(4, detalle.length() > 1900 ? detalle.substring(0, 1900) : detalle);
		ps.executeUpdate();
		ps.close();
	}

	// -----------------------------------------------------------------------
	// Habilitadores (el catalogo)
	// -----------------------------------------------------------------------

	/**
	 * Crea o actualiza un habilitador. Si idtipo viene vacio se crea uno nuevo con un
	 * identificador sacado del nombre (que no se puede cambiar despues, porque las marcas
	 * ya puestas dependen de el). Devuelve el identificador.
	 */
	public static String guardarTipo(final String idtipo, final String nombre, final String descripcion, final String severidad,
			final String bloqueaCargo, final String activo, final int orden, final String usuario) throws SQLException {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);
			final String sev = "N".equals(severidad) || "P".equals(severidad) ? severidad : "I";
			final String bloquea = "S".equals(bloqueaCargo) ? "S" : "N";
			final String act = "N".equals(activo) ? "N" : "S";
			String id = idtipo == null ? "" : idtipo.trim();
			final String nom = nombre.trim();
			final String desc = descripcion == null ? "" : descripcion.trim();
			if (id.length() == 0) {
				final String base = identificador(nom);
				id = base;
				int n = 2;
				while (existeTipo(cn, id)) {
					final String sufijo = "_" + n++;
					id = (base.length() + sufijo.length() > 30 ? base.substring(0, 30 - sufijo.length()) : base) + sufijo;
				}
				final PreparedStatement pi = cn.prepareStatement(
						"INSERT INTO cliente_marca_tipo (idtipo, nombre, descripcion, severidad, bloquea_cargo, activo, orden) VALUES (?,?,?,?,?,?,?)");
				pi.setString(1, id);
				pi.setString(2, nom);
				pi.setString(3, desc);
				pi.setString(4, sev);
				pi.setString(5, bloquea);
				pi.setString(6, act);
				pi.setInt(7, orden);
				pi.executeUpdate();
				pi.close();
				registrar(cn, usuario, "#TIPO:" + id, "TIPO_NUEVO", nom + " | bloquea_cargo=" + bloquea + " | severidad=" + sev);
			} else {
				final PreparedStatement pu = cn.prepareStatement(
						"UPDATE cliente_marca_tipo SET nombre = ?, descripcion = ?, severidad = ?, bloquea_cargo = ?, activo = ?, orden = ? WHERE idtipo = ?");
				pu.setString(1, nom);
				pu.setString(2, desc);
				pu.setString(3, sev);
				pu.setString(4, bloquea);
				pu.setString(5, act);
				pu.setInt(6, orden);
				pu.setString(7, id);
				final int n = pu.executeUpdate();
				pu.close();
				if (n == 0) {
					throw new SQLException("El habilitador " + id + " no existe");
				}
				registrar(cn, usuario, "#TIPO:" + id, "TIPO_EDITA", nom + " | bloquea_cargo=" + bloquea + " | severidad=" + sev + " | activo=" + act);
			}
			cn.commit();
			return (id);
		} catch (final SQLException e) {
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception e2) {
				//nada que hacer
			}
			throw e;
		} finally {
			cerrar(cn);
		}
	}

	private static boolean existeTipo(final Connection cn, final String id) throws SQLException {
		final PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM cliente_marca_tipo WHERE idtipo = ?");
		ps.setString(1, id);
		final ResultSet rs = ps.executeQuery();
		final boolean existe = rs.next();
		rs.close();
		ps.close();
		return (existe);
	}

	// -----------------------------------------------------------------------
	// Rappi Cargo
	// -----------------------------------------------------------------------

	/**
	 * Los habilitadores activos con bloquea_cargo = 'S' que tiene alguno de estos telefonos.
	 * NUNCA lanza excepcion y, si no se puede consultar, devuelve "sin bloqueo": las tablas
	 * pueden no existir todavia y el envio por Rappi Cargo no puede quedar trabado por eso.
	 */
	public static Bloqueo bloqueosRappiCargo(final ArrayList<String> telefonos) {
		final Bloqueo bloqueo = new Bloqueo();
		final ArrayList<String> tels = new ArrayList<String>();
		for (final String t : telefonos) {
			final String n = normalizarTelefono(t);
			if (n.length() > 0 && !tels.contains(n)) {
				tels.add(n);
			}
		}
		if (tels.isEmpty()) {
			return (bloqueo);
		}
		final StringBuilder marcadores = new StringBuilder();
		for (int i = 0; i < tels.size(); i++) {
			marcadores.append(i == 0 ? "?" : ",?");
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					//Sin DISTINCT: con ORDER BY por una columna que no esta en la lista MySQL rechaza la
					//consulta (error 3065), y como esta funcion nunca lanza, el bloqueo dejaba de
					//funcionar en silencio. Los repetidos se descartan abajo.
					"SELECT t.nombre, m.telefono FROM cliente_marca m INNER JOIN cliente_marca_tipo t ON t.idtipo = m.idtipo"
							+ " WHERE t.activo = 'S' AND t.bloquea_cargo = 'S' AND m.telefono IN (" + marcadores + ") ORDER BY t.orden");
			for (int i = 0; i < tels.size(); i++) {
				ps.setString(i + 1, tels.get(i));
			}
			final ResultSet rs = ps.executeQuery();
			String telefonoMarcado = "";
			while (rs.next()) {
				if (!bloqueo.nombres.contains(rs.getString(1))) {
					bloqueo.nombres.add(rs.getString(1));
				}
				telefonoMarcado = rs.getString(2);
			}
			rs.close();
			ps.close();
			if (telefonoMarcado.length() > 0) {
				final PreparedStatement pt = cn.prepareStatement("SELECT IFNULL(mensaje,'') FROM cliente_alerta WHERE telefono = ?");
				pt.setString(1, telefonoMarcado);
				final ResultSet rt = pt.executeQuery();
				if (rt.next()) {
					bloqueo.texto = rt.getString(1);
				}
				rt.close();
				pt.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ClienteMarcaDAO.bloqueosRappiCargo: " + e.toString());
			bloqueo.nombres.clear();
			bloqueo.texto = "";
		} finally {
			cerrar(cn);
		}
		return (bloqueo);
	}

	/** Los telefonos (fijo y celular) de un cliente del sistema. Vacio si no se puede consultar. */
	public static ArrayList<String> telefonosDeCliente(final int idcliente) {
		final ArrayList<String> tels = new ArrayList<String>();
		if (idcliente <= 0) {
			return (tels);
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement("SELECT telefono, telefono_celular FROM cliente WHERE idcliente = ?");
			ps.setInt(1, idcliente);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				tels.add(rs.getString(1));
				tels.add(rs.getString(2));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("ClienteMarcaDAO.telefonosDeCliente: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (tels);
	}
}
