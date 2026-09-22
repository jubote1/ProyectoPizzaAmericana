package capaDAOCC;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Administra las definiciones de segmento: crear, editar, borrar y clasificar.
 *
 * Hasta el 2026-09-22 los segmentos vivian en un CASE dentro del procedimiento
 * de recalculo y crear uno nuevo era un cambio de codigo con despliegue. Con
 * crm.segmento_definicion y crm.segmento_regla un segmento es una fila con sus
 * reglas; esta clase es lo que deja editarlas desde una pantalla.
 *
 * TODO LO QUE ENTRA SE VALIDA AQUI, NO SOLO EN LA PANTALLA
 *
 * La condicion del recalculo se arma CONCATENANDO el nombre del campo y el
 * valor. Si esos dos llegaran libres seria una inyeccion de SQL corriendo como
 * root. Por eso:
 *
 *   - el campo tiene que estar en crm.segmento_campo -la lista blanca-,
 *   - el operador tiene que ser uno de los seis,
 *   - un campo numerico solo acepta un numero,
 *   - un campo de texto solo acepta letras, numeros y unos pocos signos.
 *
 * Lo que no pase NO se guarda. Validar al escribir y no al ejecutar es lo que
 * permite que despues el procedimiento concatene sin volver a escapar nada, y
 * que la prueba de la pantalla y el recalculo den exactamente lo mismo.
 *
 * LA PRUEBA DEVUELVE DOS NUMEROS, Y EL QUE IMPORTA ES EL SEGUNDO
 *
 * "Cumplen" es cuanta gente satisface las reglas. "Quedarian aqui" es cuanta
 * gente TERMINA en este segmento, descontando la que se lleva una definicion
 * de mayor prioridad. Un segmento nuevo puesto detras de uno mas amplio cumple
 * con miles y se queda con cero, y sin ese segundo numero uno lo guarda
 * convencido de que funciona.
 */
public class DefinicionSegmentoDAO {

	/** Los unicos operadores. Cualquier otro se rechaza al guardar. */
	private static final String[] OPERADORES = { "=", "<>", ">", "<", ">=", "<=" };

	/** Lo que le queda a quien no cumple ninguna definicion. Nombre reservado. */
	public static final String SIN_CLASIFICAR = "SIN CLASIFICAR";

	/**
	 * Si la tabla no se puede leer, estos son los segmentos que existen. No es
	 * adorno: SegmentacionPersonaDAO valida contra esta lista y quedarse sin
	 * ella dejaria la pantalla de consulta sin filtro de segmento.
	 */
	private static final String[] RESPALDO = {
		"NUEVO", "ACTIVO", "EN RIESGO", "DORMIDO", "SIN PEDIDOS" };

	// =======================================================================
	// Lo que se mueve
	// =======================================================================

	public static class Campo {
		public String campo = "";
		public String etiqueta = "";
		public String tipo = "NUMERO";
		public String ayuda = "";
	}

	public static class Regla {
		public int idRegla;
		public String campo = "";
		public String operador = "";
		public String valor = "";
	}

	public static class Definicion {
		public int idSegmento;
		public String nombre = "";
		public String descripcion = "";
		public int orden = 100;
		public String color = "";
		public String activo = "S";
		public ArrayList<Regla> reglas = new ArrayList<Regla>();
		/** Cuanta gente tiene HOY ese segmento en persona_resumen. */
		public int personas;
		public double valor;
	}

	// =======================================================================
	// Leer
	// =======================================================================

	public static ArrayList<Campo> campos() {
		final ArrayList<Campo> lista = new ArrayList<Campo>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT campo, etiqueta, tipo, IFNULL(ayuda,'') ayuda"
					+ " FROM crm.segmento_campo ORDER BY orden, campo");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Campo c = new Campo();
				c.campo = rs.getString("campo");
				c.etiqueta = rs.getString("etiqueta");
				c.tipo = rs.getString("tipo");
				c.ayuda = rs.getString("ayuda");
				lista.add(c);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.campos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Las definiciones con sus reglas y con cuanta gente tienen hoy. */
	public static ArrayList<Definicion> listar() {
		final ArrayList<Definicion> lista = new ArrayList<Definicion>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final LinkedHashMap<Integer, Definicion> porId = new LinkedHashMap<Integer, Definicion>();

			final PreparedStatement psD = cn.prepareStatement(
					"SELECT idsegmento, nombre, IFNULL(descripcion,'') descripcion,"
					+ " orden, IFNULL(color,'') color, activo"
					+ " FROM crm.segmento_definicion ORDER BY orden, idsegmento");
			final ResultSet rsD = psD.executeQuery();
			while (rsD.next()) {
				final Definicion d = new Definicion();
				d.idSegmento = rsD.getInt("idsegmento");
				d.nombre = rsD.getString("nombre");
				d.descripcion = rsD.getString("descripcion");
				d.orden = rsD.getInt("orden");
				d.color = rsD.getString("color");
				d.activo = rsD.getString("activo");
				porId.put(Integer.valueOf(d.idSegmento), d);
				lista.add(d);
			}
			rsD.close();
			psD.close();

			final PreparedStatement psR = cn.prepareStatement(
					"SELECT idregla, idsegmento, campo, operador, valor"
					+ " FROM crm.segmento_regla ORDER BY idsegmento, idregla");
			final ResultSet rsR = psR.executeQuery();
			while (rsR.next()) {
				final Definicion d = porId.get(Integer.valueOf(rsR.getInt("idsegmento")));
				if (d == null) {
					continue;
				}
				final Regla r = new Regla();
				r.idRegla = rsR.getInt("idregla");
				r.campo = rsR.getString("campo");
				r.operador = rsR.getString("operador");
				r.valor = rsR.getString("valor");
				d.reglas.add(r);
			}
			rsR.close();
			psR.close();

			//El conteo sale de persona_resumen y no de las reglas: es lo que hay
			//HOY en la tabla, o sea el resultado de la ultima clasificacion. Si
			//alguien edito una definicion y todavia no ha clasificado, el numero
			//va a estar viejo, y por eso la pantalla avisa.
			final HashMap<String, Integer> conteos = new HashMap<String, Integer>();
			final HashMap<String, Double> valores = new HashMap<String, Double>();
			final PreparedStatement psC = cn.prepareStatement(
					"SELECT segmento, COUNT(*) personas, IFNULL(SUM(valor),0) valor"
					+ " FROM crm.persona_resumen GROUP BY segmento");
			final ResultSet rsC = psC.executeQuery();
			while (rsC.next()) {
				conteos.put(rsC.getString("segmento"), Integer.valueOf(rsC.getInt("personas")));
				valores.put(rsC.getString("segmento"), Double.valueOf(rsC.getDouble("valor")));
			}
			rsC.close();
			psC.close();

			for (int i = 0; i < lista.size(); i++) {
				final Definicion d = lista.get(i);
				final Integer n = conteos.get(d.nombre);
				final Double v = valores.get(d.nombre);
				d.personas = (n == null ? 0 : n.intValue());
				d.valor = (v == null ? 0 : v.doubleValue());
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.listar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/** Cuanta gente quedo SIN CLASIFICAR: son los casos que a las reglas les faltan. */
	public static int sinClasificar() {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		int n = 0;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT COUNT(*) FROM crm.persona_resumen WHERE segmento = ?");
			ps.setString(1, SIN_CLASIFICAR);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				n = rs.getInt(1);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.sinClasificar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (n);
	}

	/**
	 * Los nombres de segmento que existen. Lo usa la pantalla de consulta para
	 * armar sus filtros, en vez de tenerlos escritos.
	 */
	public static ArrayList<String> nombresValidos() {
		final ArrayList<String> nombres = new ArrayList<String>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT nombre FROM crm.segmento_definicion ORDER BY orden, idsegmento");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				nombres.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.nombresValidos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		if (nombres.isEmpty()) {
			//Sin lista blanca no se abre el filtro: se cae al respaldo.
			for (int i = 0; i < RESPALDO.length; i++) {
				nombres.add(RESPALDO[i]);
			}
		}
		if (!nombres.contains(SIN_CLASIFICAR)) {
			nombres.add(SIN_CLASIFICAR);
		}
		return (nombres);
	}

	// =======================================================================
	// Validar
	// =======================================================================

	/** Un numero, con o sin decimales y con signo. */
	private static boolean esNumero(final String s) {
		return (s != null && s.matches("^-?[0-9]+([.][0-9]+)?$"));
	}

	/**
	 * Un texto que se puede concatenar sin escapar.
	 *
	 * Se validan los valores AL GUARDAR y no al ejecutar, a proposito: asi lo
	 * que hay en la tabla siempre es seguro de concatenar, y el procedimiento
	 * -que corre de noche, sin nadie mirando- no tiene que confiar en nadie.
	 */
	private static boolean esTexto(final String s) {
		return (s != null && s.length() > 0 && s.length() <= 40
				&& s.matches("^[A-Za-z0-9 ._-]+$"));
	}

	private static boolean operadorValido(final String op) {
		for (int i = 0; i < OPERADORES.length; i++) {
			if (OPERADORES[i].equals(op)) {
				return (true);
			}
		}
		return (false);
	}

	/** El tipo de cada campo de la lista blanca, para no consultarla de a una. */
	private static HashMap<String, String> tiposDeCampo() {
		final HashMap<String, String> tipos = new HashMap<String, String>();
		final ArrayList<Campo> lista = campos();
		for (int i = 0; i < lista.size(); i++) {
			tipos.put(lista.get(i).campo, lista.get(i).tipo);
		}
		return (tipos);
	}

	/**
	 * Arma la condicion SQL de un conjunto de reglas, igual que el
	 * procedimiento: las reglas de un segmento se suman con Y.
	 *
	 * @return null si NO queda ninguna regla valida. Quien llame tiene que
	 *         tratar ese null como "no hacer nada": una condicion vacia en un
	 *         UPDATE se llevaria a las 450 mil personas.
	 */
	public static String condicionDe(final ArrayList<Regla> reglas,
			final HashMap<String, String> tipos) {
		if (reglas == null || reglas.isEmpty()) {
			return (null);
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < reglas.size(); i++) {
			final Regla r = reglas.get(i);
			final String tipo = tipos.get(r.campo);
			if (tipo == null || !operadorValido(r.operador)) {
				continue;
			}
			final boolean texto = "TEXTO".equalsIgnoreCase(tipo);
			if (texto ? !esTexto(r.valor) : !esNumero(r.valor)) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(" AND ");
			}
			sb.append("`").append(r.campo).append("` ").append(r.operador).append(" ");
			sb.append(texto ? ("'" + r.valor + "'") : r.valor);
		}
		return (sb.length() == 0 ? null : sb.toString());
	}

	// =======================================================================
	// Probar antes de guardar
	// =======================================================================

	public static class Prueba {
		public String error = "";
		public String condicion = "";
		/** Cuantos cumplen las reglas, sin mirar prioridades. */
		public int cumplen;
		/** Cuantos terminarian de verdad en este segmento. */
		public int quedarian;
		public int contactables;
		public double valor;
	}

	/**
	 * @param idSegmento el que se esta editando, 0 si es nuevo. Se excluye de
	 *                   los de mayor prioridad para no restarse a si mismo.
	 * @param orden      la prioridad con la que se va a guardar
	 */
	public static Prueba probar(final ArrayList<Regla> reglas, final int idSegmento,
			final int orden) {
		final Prueba p = new Prueba();
		final HashMap<String, String> tipos = tiposDeCampo();
		final String cond = condicionDe(reglas, tipos);
		if (cond == null) {
			p.error = "Ninguna de las reglas sirve. Revise el campo, el operador y el valor.";
			return (p);
		}
		p.condicion = cond;

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();

			//1. Cuantos cumplen, a secas.
			final Statement st = cn.createStatement();
			final ResultSet rs = st.executeQuery(
					"SELECT COUNT(*) n, IFNULL(SUM(valor),0) v,"
					+ " IFNULL(SUM(politica_datos='S'),0) c"
					+ " FROM crm.persona_resumen WHERE " + cond);
			if (rs.next()) {
				p.cumplen = rs.getInt("n");
				p.valor = rs.getDouble("v");
				p.contactables = rs.getInt("c");
			}
			rs.close();
			st.close();

			//2. Cuantos quedarian, descontando los que se lleva una definicion
			//   de MAYOR prioridad. Este es el numero honesto.
			final StringBuilder resta = new StringBuilder(cond);
			final ArrayList<Definicion> otras = listar();
			//Un segmento nuevo todavia no tiene id y va a recibir el siguiente,
			//o sea el mas alto de todos. Tratarlo como 0 lo haria ganar todos
			//los empates de orden, y la prueba mostraria mas gente de la que
			//despues le va a quedar.
			final int idPropio = (idSegmento > 0 ? idSegmento : Integer.MAX_VALUE);
			for (int i = 0; i < otras.size(); i++) {
				final Definicion d = otras.get(i);
				if (d.idSegmento == idSegmento || !"S".equalsIgnoreCase(d.activo)) {
					continue;
				}
				/*
				 * Menor numero de orden es mayor prioridad.
				 *
				 * Con el mismo orden gana el de idsegmento MENOR, y eso hay que
				 * leerlo del procedimiento y no suponerlo: su cursor va
				 * "ORDER BY orden DESC, idsegmento DESC" y aplica de la menos
				 * prioritaria a la mas prioritaria, asi que dentro del mismo
				 * orden el id mas bajo escribe de ultimo y se queda con la
				 * gente. Al reves, la prueba mentiria justo en el caso en que
				 * uno duplica un segmento para variarlo.
				 */
				final boolean mandaMas = (d.orden < orden)
						|| (d.orden == orden && d.idSegmento < idPropio);
				if (!mandaMas) {
					continue;
				}
				final String c = condicionDe(d.reglas, tipos);
				if (c != null) {
					resta.append(" AND NOT (").append(c).append(")");
				}
			}
			final Statement st2 = cn.createStatement();
			final ResultSet rs2 = st2.executeQuery(
					"SELECT COUNT(*) FROM crm.persona_resumen WHERE " + resta.toString());
			if (rs2.next()) {
				p.quedarian = rs2.getInt(1);
			}
			rs2.close();
			st2.close();
		} catch (final Exception e) {
			p.error = "No se pudo probar: " + e.getMessage();
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.probar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (p);
	}

	// =======================================================================
	// Escribir
	// =======================================================================

	/** @return "" si guardo bien, o el motivo por el que no. */
	public static String guardar(final Definicion d, final String usuario) {
		if (d.nombre == null || d.nombre.trim().length() == 0) {
			return ("El segmento necesita un nombre.");
		}
		d.nombre = d.nombre.trim().toUpperCase();
		if (d.nombre.length() > 20) {
			return ("El nombre no puede pasar de 20 letras.");
		}
		if (!d.nombre.matches("^[A-Z0-9 ]+$")) {
			return ("El nombre solo admite letras sin tilde, numeros y espacios.");
		}
		if (SIN_CLASIFICAR.equals(d.nombre)) {
			return ("Ese nombre esta reservado: es el que lleva quien no cumple ninguna definicion.");
		}
		if (d.color != null && d.color.length() > 0 && !d.color.matches("^#[0-9A-Fa-f]{6}$")) {
			return ("El color tiene que ser un codigo como #1B4A9C.");
		}
		final HashMap<String, String> tipos = tiposDeCampo();
		if (condicionDe(d.reglas, tipos) == null) {
			//Un segmento sin reglas validas NO clasifica a nadie -el
			//procedimiento lo salta-, asi que guardarlo solo serviria para
			//quedar uno convencido de que hizo algo.
			return ("El segmento necesita por lo menos una regla valida.");
		}

		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);

			if (d.idSegmento > 0) {
				final PreparedStatement ps = cn.prepareStatement(
						"UPDATE crm.segmento_definicion SET nombre=?, descripcion=?,"
						+ " orden=?, color=?, activo=? WHERE idsegmento=?");
				ps.setString(1, d.nombre);
				ps.setString(2, recortar(d.descripcion, 255));
				ps.setInt(3, d.orden);
				ps.setString(4, d.color == null || d.color.length() == 0 ? null : d.color);
				ps.setString(5, "N".equalsIgnoreCase(d.activo) ? "N" : "S");
				ps.setInt(6, d.idSegmento);
				ps.executeUpdate();
				ps.close();
			} else {
				final PreparedStatement ps = cn.prepareStatement(
						"INSERT INTO crm.segmento_definicion (nombre, descripcion, orden, color, activo)"
						+ " VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, d.nombre);
				ps.setString(2, recortar(d.descripcion, 255));
				ps.setInt(3, d.orden);
				ps.setString(4, d.color == null || d.color.length() == 0 ? null : d.color);
				ps.setString(5, "N".equalsIgnoreCase(d.activo) ? "N" : "S");
				ps.executeUpdate();
				final ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					d.idSegmento = rs.getInt(1);
				}
				rs.close();
				ps.close();
			}

			//Las reglas se reemplazan enteras y no se van comparando una por
			//una: son tres o cuatro filas, y asi no queda forma de dejar a
			//medias un segmento con la mitad de las reglas viejas.
			final PreparedStatement psB = cn.prepareStatement(
					"DELETE FROM crm.segmento_regla WHERE idsegmento = ?");
			psB.setInt(1, d.idSegmento);
			psB.executeUpdate();
			psB.close();

			final PreparedStatement psR = cn.prepareStatement(
					"INSERT INTO crm.segmento_regla (idsegmento, campo, operador, valor)"
					+ " VALUES (?, ?, ?, ?)");
			for (int i = 0; i < d.reglas.size(); i++) {
				final Regla r = d.reglas.get(i);
				final String tipo = tipos.get(r.campo);
				if (tipo == null || !operadorValido(r.operador)) {
					continue;
				}
				final boolean texto = "TEXTO".equalsIgnoreCase(tipo);
				if (texto ? !esTexto(r.valor) : !esNumero(r.valor)) {
					continue;
				}
				psR.setInt(1, d.idSegmento);
				psR.setString(2, r.campo);
				psR.setString(3, r.operador);
				psR.setString(4, r.valor);
				psR.addBatch();
			}
			psR.executeBatch();
			psR.close();

			cn.commit();
			Logger.getLogger("log_file").info("Segmento guardado: " + d.nombre
					+ " orden=" + d.orden + " reglas=" + d.reglas.size() + " por " + usuario);
		} catch (final Exception e) {
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception r) {
				Logger.getLogger("log_file").error("DefinicionSegmentoDAO: no deshizo, " + r.toString());
			}
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.guardar: " + e.toString());
			if (e.toString().indexOf("Duplicate") >= 0) {
				return ("Ya existe un segmento con ese nombre.");
			}
			return ("No se pudo guardar: " + e.getMessage());
		} finally {
			cerrar(cn);
		}
		return ("");
	}

	/** @return "" si borro bien, o el motivo por el que no. */
	public static String borrar(final int idSegmento, final String usuario) {
		if (idSegmento <= 0) {
			return ("No se indico cual segmento.");
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);
			final PreparedStatement psR = cn.prepareStatement(
					"DELETE FROM crm.segmento_regla WHERE idsegmento = ?");
			psR.setInt(1, idSegmento);
			psR.executeUpdate();
			psR.close();
			final PreparedStatement psD = cn.prepareStatement(
					"DELETE FROM crm.segmento_definicion WHERE idsegmento = ?");
			psD.setInt(1, idSegmento);
			psD.executeUpdate();
			psD.close();
			cn.commit();
			//La gente que tenia ese segmento NO se toca aqui: se acomoda sola en
			//la proxima clasificacion, que es la que sabe a donde mandarla.
			Logger.getLogger("log_file").info("Segmento borrado: id=" + idSegmento + " por " + usuario);
		} catch (final Exception e) {
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception r) {
				Logger.getLogger("log_file").error("DefinicionSegmentoDAO: no deshizo, " + r.toString());
			}
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.borrar: " + e.toString());
			return ("No se pudo borrar: " + e.getMessage());
		} finally {
			cerrar(cn);
		}
		return ("");
	}

	// =======================================================================
	// Clasificar
	// =======================================================================

	/**
	 * Vuelve a repartir a las 450 mil personas segun las definiciones de este
	 * momento. Tarda alrededor de un minuto.
	 *
	 * Corre sobre persona_resumen -la tabla viva, la que esta leyendo el CRM- y
	 * NO recalcula los pedidos: eso es otra cosa, tarda 75 segundos mas y lo
	 * hace el proceso de la noche. Aqui solo se reparte lo que ya esta contado.
	 *
	 * @return "" si clasifico bien, o el motivo por el que no.
	 */
	public static String clasificar(final String usuario) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		final long inicio = System.currentTimeMillis();
		try {
			cn = con.obtenerConexionBDPrincipal();
			final CallableStatement cs = cn.prepareCall("{CALL crm.pr_clasificar_segmentos(?)}");
			cs.setString(1, "persona_resumen");
			cs.execute();
			cs.close();
			Logger.getLogger("log_file").info("Segmentos clasificados por " + usuario
					+ " en " + ((System.currentTimeMillis() - inicio) / 1000) + " s");
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO.clasificar: " + e.toString());
			return ("No se pudo clasificar: " + e.getMessage());
		} finally {
			cerrar(cn);
		}
		return ("");
	}

	// =======================================================================

	private static String recortar(final String s, final int largo) {
		if (s == null) {
			return (null);
		}
		final String t = s.trim();
		return (t.length() <= largo ? t : t.substring(0, largo));
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("DefinicionSegmentoDAO: no cerro, " + e.toString());
		}
	}
}
