package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Segmentacion de PERSONAS sobre crm.persona_resumen.
 *
 * EN QUE SE DIFERENCIA DE segmentacionCliente.html
 *
 * Aquella trabaja sobre pizzaamericana.cliente -una fila por cada vez que
 * alguien tomo los datos- y su salida es una lista de correos para una campana
 * por Brevo. Esta trabaja sobre PERSONAS: las 1,21 filas promedio de la misma
 * persona ya estan unidas, y cuenta tambien los pedidos de mostrador, que en
 * el central no existen.
 *
 * No la reemplaza. La de campanas sigue siendo la que envia; esta es para
 * mirar, contar y sacar la lista.
 *
 * POR QUE SE LEE UNA TABLA Y NO LA VISTA 360
 *
 * crm.v_persona_360 responde una persona en 0,4 s, pero esta hecha de
 * subconsultas correlacionadas: agrupar 450 mil personas la haria recorrer los
 * 797 mil pedidos una vez por cada una, sobre el mismo servidor donde estan
 * vendiendo las once tiendas. crm.persona_resumen se calcula una vez por noche
 * y aqui solo se filtra, con indices por segmento, ultimo_pedido, valor y
 * tienda habitual.
 *
 * NADA DE LO QUE ESCRIBE EL USUARIO ENTRA EN EL SQL
 *
 * Todo va por parametros, incluidos los segmentos -se generan tantos ? como
 * segmentos lleguen- y la lista blanca de columnas para ordenar. El login de
 * este mismo sistema era inyectable hasta hace dos dias; no se repite.
 */
public class SegmentacionPersonaDAO {

	/** Tope de una pagina. Mas que esto no se alcanza a mirar en pantalla. */
	private static final int TOPE_PAGINA = 200;

	/**
	 * Tope de la descarga. No es capricho: son datos personales, y un archivo
	 * de medio millon de filas se manda por correo sin pensarlo dos veces.
	 */
	public static final int TOPE_DESCARGA = 50000;

	/** Los unicos segmentos que existen. Lo que no este aqui no se consulta. */
	private static final String[] SEGMENTOS_VALIDOS = {
		"NUEVO", "ACTIVO", "EN RIESGO", "DORMIDO", "SIN PEDIDOS" };

	// =======================================================================
	// Lo que entra
	// =======================================================================

	public static class Filtro {
		public ArrayList<String> segmentos = new ArrayList<String>();
		/** 0 = todas las tiendas. */
		public int idTienda = 0;
		public int pedidosMin = 0;
		public double valorMin = 0;
		/** -1 = sin tope. */
		public int diasMin = -1;
		public int diasMax = -1;
		public boolean soloConCorreo = false;
		public boolean soloAutorizados = false;
		/** TODOS, MOSTRADOR, DOMICILIO, AMBOS. */
		public String canal = "TODOS";
		/** VALOR, RECIENTE, PEDIDOS. */
		public String orden = "VALOR";
		public int pagina = 1;
		public int porPagina = 50;
	}

	// =======================================================================
	// Lo que sale
	// =======================================================================

	public static class Fila {
		public long idPersona;
		public String nombre = "";
		public String apellido = "";
		public String celular = "";
		public String email = "";
		public String politicaDatos = "";
		public String segmento = "";
		public int pedidos;
		public int pedidosCentral;
		public int pedidosTienda;
		public double valor;
		public double ticket;
		public String ultimoPedido = "";
		public int diasSinComprar;
		public int diasEntrePedidos;
		public int tiendaHabitual;
		public int tiendasDistintas;
	}

	public static class Conteo {
		public String segmento = "";
		public int personas;
		public double valor;
	}

	public static class Resultado {
		public int personas;
		public long pedidos;
		public double valor;
		public int conCorreo;
		public int autorizados;
		public ArrayList<Fila> filas = new ArrayList<Fila>();
		public ArrayList<Conteo> porSegmento = new ArrayList<Conteo>();
		public String error = "";
	}

	public static class Tienda {
		public int idTienda;
		public String nombre = "";
	}

	// =======================================================================
	// El armado del WHERE
	// =======================================================================

	/**
	 * Arma la condicion y deja en la lista los valores, en el mismo orden.
	 *
	 * @param conSegmento false para el conteo por segmento, que necesita ver
	 *                    TODOS los segmentos aunque el usuario haya filtrado
	 *                    por uno: asi la pantalla puede mostrar donde esta el
	 *                    resto y no solo lo que ya escogio.
	 */
	private static String armarWhere(final Filtro f, final ArrayList<Object> valores,
			final boolean conSegmento) {
		final StringBuilder w = new StringBuilder(" WHERE 1 = 1");

		if (conSegmento && f.segmentos != null && !f.segmentos.isEmpty()) {
			w.append(" AND segmento IN (");
			for (int i = 0; i < f.segmentos.size(); i++) {
				w.append(i == 0 ? "?" : ",?");
				valores.add(f.segmentos.get(i));
			}
			w.append(")");
		}
		if (f.idTienda > 0) {
			w.append(" AND tienda_habitual = ?");
			valores.add(Integer.valueOf(f.idTienda));
		}
		if (f.pedidosMin > 0) {
			w.append(" AND pedidos >= ?");
			valores.add(Integer.valueOf(f.pedidosMin));
		}
		if (f.valorMin > 0) {
			w.append(" AND valor >= ?");
			valores.add(Double.valueOf(f.valorMin));
		}
		if (f.diasMin >= 0) {
			w.append(" AND dias_sin_comprar >= ?");
			valores.add(Integer.valueOf(f.diasMin));
		}
		if (f.diasMax >= 0) {
			w.append(" AND dias_sin_comprar <= ?");
			valores.add(Integer.valueOf(f.diasMax));
		}
		if (f.soloConCorreo) {
			//Un correo vacio no es NULL en esta tabla, viene de cliente.email y
			//puede llegar como cadena en blanco. Preguntar solo por NOT NULL
			//dejaria pasar filas sin correo.
			w.append(" AND email IS NOT NULL AND TRIM(email) <> ''");
		}
		if (f.soloAutorizados) {
			w.append(" AND politica_datos = 'S'");
		}
		if ("MOSTRADOR".equals(f.canal)) {
			w.append(" AND pedidos_tienda > 0 AND pedidos_central = 0");
		} else if ("DOMICILIO".equals(f.canal)) {
			w.append(" AND pedidos_central > 0 AND pedidos_tienda = 0");
		} else if ("AMBOS".equals(f.canal)) {
			w.append(" AND pedidos_central > 0 AND pedidos_tienda > 0");
		}
		return (w.toString());
	}

	/**
	 * La columna por la que se ordena sale de una lista blanca, nunca de lo que
	 * llegue por parametro: un ORDER BY no admite ?, asi que si se concatenara
	 * lo que manda la pantalla seria una puerta abierta.
	 *
	 * El desempate por idpersona no sobra: sin el, dos personas con el mismo
	 * valor pueden salir en distinto orden en cada pagina y una queda repetida
	 * mientras otra no aparece nunca.
	 */
	private static String armarOrden(final Filtro f) {
		if ("RECIENTE".equals(f.orden)) {
			return (" ORDER BY ultimo_pedido DESC, idpersona");
		}
		if ("PEDIDOS".equals(f.orden)) {
			return (" ORDER BY pedidos DESC, idpersona");
		}
		return (" ORDER BY valor DESC, idpersona");
	}

	private static void ponerValores(final PreparedStatement ps, final ArrayList<Object> valores)
			throws Exception {
		for (int i = 0; i < valores.size(); i++) {
			final Object v = valores.get(i);
			if (v instanceof Integer) {
				ps.setInt(i + 1, ((Integer) v).intValue());
			} else if (v instanceof Double) {
				ps.setDouble(i + 1, ((Double) v).doubleValue());
			} else {
				ps.setString(i + 1, String.valueOf(v));
			}
		}
	}

	/** Deja el filtro dentro de lo razonable antes de tocar la base. */
	private static void sanear(final Filtro f) {
		if (f.porPagina < 1) {
			f.porPagina = 50;
		}
		if (f.porPagina > TOPE_PAGINA) {
			f.porPagina = TOPE_PAGINA;
		}
		if (f.pagina < 1) {
			f.pagina = 1;
		}
		final ArrayList<String> limpios = new ArrayList<String>();
		if (f.segmentos != null) {
			for (int i = 0; i < f.segmentos.size(); i++) {
				final String s = f.segmentos.get(i);
				for (int j = 0; j < SEGMENTOS_VALIDOS.length; j++) {
					if (SEGMENTOS_VALIDOS[j].equals(s)) {
						limpios.add(s);
						break;
					}
				}
			}
		}
		f.segmentos = limpios;
	}

	// =======================================================================
	// La consulta
	// =======================================================================

	public static Resultado consultar(final Filtro f) {
		sanear(f);
		final Resultado r = new Resultado();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();

			//1. El total, que es lo que de verdad le importa a quien segmenta:
			//   cuanta gente hay ahi, no cuales son las primeras cincuenta.
			final ArrayList<Object> v1 = new ArrayList<Object>();
			final String where = armarWhere(f, v1, true);
			final PreparedStatement psTotal = cn.prepareStatement(
					"SELECT COUNT(*), IFNULL(SUM(pedidos),0), IFNULL(SUM(valor),0),"
					+ " SUM(email IS NOT NULL AND TRIM(email) <> ''),"
					+ " SUM(politica_datos = 'S')"
					+ " FROM crm.persona_resumen" + where);
			ponerValores(psTotal, v1);
			final ResultSet rsTotal = psTotal.executeQuery();
			if (rsTotal.next()) {
				r.personas = rsTotal.getInt(1);
				r.pedidos = rsTotal.getLong(2);
				r.valor = rsTotal.getDouble(3);
				r.conCorreo = rsTotal.getInt(4);
				r.autorizados = rsTotal.getInt(5);
			}
			rsTotal.close();
			psTotal.close();

			//2. La pagina.
			final ArrayList<Object> v2 = new ArrayList<Object>();
			final String where2 = armarWhere(f, v2, true);
			final PreparedStatement psLista = cn.prepareStatement(
					"SELECT idpersona, nombre, apellido, celular, email, politica_datos,"
					+ " segmento, pedidos, pedidos_central, pedidos_tienda, valor,"
					+ " ticket_promedio, ultimo_pedido, dias_sin_comprar,"
					+ " dias_entre_pedidos, tienda_habitual, tiendas_distintas"
					+ " FROM crm.persona_resumen" + where2 + armarOrden(f)
					+ " LIMIT ? OFFSET ?");
			v2.add(Integer.valueOf(f.porPagina));
			v2.add(Integer.valueOf((f.pagina - 1) * f.porPagina));
			ponerValores(psLista, v2);
			final ResultSet rs = psLista.executeQuery();
			while (rs.next()) {
				r.filas.add(leerFila(rs));
			}
			rs.close();
			psLista.close();

			//3. Como se reparte por segmento, SIN el filtro de segmento. Sirve
			//   para ver que hay al lado de lo que se escogio.
			final ArrayList<Object> v3 = new ArrayList<Object>();
			final String where3 = armarWhere(f, v3, false);
			final PreparedStatement psSeg = cn.prepareStatement(
					"SELECT segmento, COUNT(*), IFNULL(SUM(valor),0)"
					+ " FROM crm.persona_resumen" + where3
					+ " GROUP BY segmento ORDER BY COUNT(*) DESC");
			ponerValores(psSeg, v3);
			final ResultSet rsSeg = psSeg.executeQuery();
			while (rsSeg.next()) {
				final Conteo c = new Conteo();
				c.segmento = texto(rsSeg.getString(1));
				c.personas = rsSeg.getInt(2);
				c.valor = rsSeg.getDouble(3);
				r.porSegmento.add(c);
			}
			rsSeg.close();
			psSeg.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("SegmentacionPersonaDAO.consultar: " + e.toString());
			r.error = "No se pudo consultar la segmentacion.";
		} finally {
			cerrar(cn);
		}
		return (r);
	}

	/**
	 * Las filas que se descargan. Sin paginar, con tope.
	 *
	 * Va aparte de consultar() y no reutiliza la pagina porque son dos cosas
	 * distintas: aqui no interesa el conteo ni el reparto por segmento, y traer
	 * cincuenta mil filas para mostrar cincuenta seria absurdo.
	 */
	public static ArrayList<Fila> paraDescargar(final Filtro f) {
		sanear(f);
		final ArrayList<Fila> filas = new ArrayList<Fila>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final ArrayList<Object> v = new ArrayList<Object>();
			final String where = armarWhere(f, v, true);
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idpersona, nombre, apellido, celular, email, politica_datos,"
					+ " segmento, pedidos, pedidos_central, pedidos_tienda, valor,"
					+ " ticket_promedio, ultimo_pedido, dias_sin_comprar,"
					+ " dias_entre_pedidos, tienda_habitual, tiendas_distintas"
					+ " FROM crm.persona_resumen" + where + armarOrden(f) + " LIMIT ?");
			v.add(Integer.valueOf(TOPE_DESCARGA));
			ponerValores(ps, v);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				filas.add(leerFila(rs));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("SegmentacionPersonaDAO.paraDescargar: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (filas);
	}

	private static Fila leerFila(final ResultSet rs) throws Exception {
		final Fila x = new Fila();
		x.idPersona = rs.getLong(1);
		x.nombre = texto(rs.getString(2));
		x.apellido = texto(rs.getString(3));
		x.celular = texto(rs.getString(4));
		x.email = texto(rs.getString(5));
		x.politicaDatos = texto(rs.getString(6));
		x.segmento = texto(rs.getString(7));
		x.pedidos = rs.getInt(8);
		x.pedidosCentral = rs.getInt(9);
		x.pedidosTienda = rs.getInt(10);
		x.valor = rs.getDouble(11);
		x.ticket = rs.getDouble(12);
		x.ultimoPedido = texto(rs.getString(13));
		x.diasSinComprar = rs.getInt(14);
		if (rs.wasNull()) {
			x.diasSinComprar = -1;
		}
		x.diasEntrePedidos = rs.getInt(15);
		if (rs.wasNull()) {
			x.diasEntrePedidos = -1;
		}
		x.tiendaHabitual = rs.getInt(16);
		x.tiendasDistintas = rs.getInt(17);
		return (x);
	}

	// =======================================================================
	// Lo que necesita la pantalla para armarse
	// =======================================================================

	public static ArrayList<Tienda> tiendas() {
		final ArrayList<Tienda> lista = new ArrayList<Tienda>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idtienda, nombre FROM tienda WHERE funcional = 'S' ORDER BY nombre");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Tienda t = new Tienda();
				t.idTienda = rs.getInt(1);
				t.nombre = texto(rs.getString(2));
				lista.add(t);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("SegmentacionPersonaDAO.tiendas: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Los umbrales con los que se calcularon los segmentos, y cuando se
	 * calcularon.
	 *
	 * La pantalla los muestra porque "ACTIVO" no quiere decir nada por si solo:
	 * quien mira la cifra tiene que saber que son 60 dias, y que el dato es de
	 * anoche y no de este momento.
	 */
	public static String[] umbrales() {
		final String[] datos = { "", "", "" };
		final ConexionBaseDatos con = new ConexionBaseDatos();
		Connection cn = null;
		try {
			cn = con.obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT (SELECT valor FROM crm.parametro_segmento WHERE nombre='DIAS_ACTIVO'),"
					+ " (SELECT valor FROM crm.parametro_segmento WHERE nombre='DIAS_RIESGO'),"
					+ " (SELECT MAX(actualizado_en) FROM crm.persona_resumen)");
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				datos[0] = texto(rs.getString(1));
				datos[1] = texto(rs.getString(2));
				datos[2] = texto(rs.getString(3));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("SegmentacionPersonaDAO.umbrales: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (datos);
	}

	private static String texto(final String s) {
		return (s == null ? "" : s);
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("SegmentacionPersonaDAO: no cerro, " + e.toString());
		}
	}
}
