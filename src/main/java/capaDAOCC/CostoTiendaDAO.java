package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * El costo de una tienda, concepto por concepto y semana por semana.
 *
 * Esto reemplaza la pestana PORCENTAJE del libro de Excel, donde los trece conceptos
 * de cada tienda estan digitados a mano y ninguno tiene formula. Esa pestana no se
 * puede reproducir: al medir cada concepto contra los datos reales, cada tienda cuadra
 * con un corte distinto -San Antonio con el mes calendario, Manrique con otro-, porque
 * cada quien reporto cuando pudo. Lo que se gana aqui no es parecerse al Excel sino
 * que las once salgan del mismo periodo.
 *
 * LA SEMANA SE IDENTIFICA POR SU DOMINGO
 *
 * Es la convencion de datamart.venta_semanal_tienda y de
 * inventarioamericana.gasto_empleado_temporal, las dos fuentes semanales que ya
 * existian. La semana del 2026-10-18 va del lunes 12 al domingo 18. Cambiarla aqui
 * dejaria el costo de una semana enfrentado a la venta de otra.
 *
 * DE DONDE SALE CADA CONCEPTO
 *
 *   EGRESO    datamart.egreso_historico_tienda, sumado por los tipos de egreso que
 *             concepto_costo_tipo_egreso le asigne al concepto. El datamart tiene las
 *             once tiendas al dia, asi que no hay que ir a los equipos de las tiendas.
 *   TEMPORAL  inventarioamericana.gasto_empleado_temporal, que ya trae por tienda y
 *             por semana lo que cobra la empresa temporal, desde 2022.
 *   DATAFONO  la venta con tarjeta de datamart.cierre_datafono_pos por la tarifa del
 *             parametro COMISIONDATAFONO. Mientras la tarifa este en cero el concepto
 *             no sale, y eso es mejor que sacarlo en cero: cero se lee como "no hubo
 *             comision", y lo que pasa es que nadie la ha parametrizado.
 *   AREA      se reparte desde gasto_area por participacion en la venta. Eso es de la
 *             fase 4 y todavia no lo hace este DAO.
 *   DIGITADO  costo_tienda_digitado, por tienda y mes. Son los que el sistema no sabe.
 *   MIXTO     parte digitada y parte de egresos. Hoy solo la nomina: la fija se digita
 *             -general.empleado no dice en que tienda esta cada quien- y el incentivo
 *             del domiciliario temporal sale del egreso tipo 1.
 *
 * Ver sql/costo_tienda.sql.
 */
public class CostoTiendaDAO {

	private static final Logger LOG = Logger.getLogger("log_file");

	public static final String ORIGEN_EGRESO = "EGRESO";
	public static final String ORIGEN_TEMPORAL = "TEMPORAL";
	public static final String ORIGEN_DATAFONO = "DATAFONO";
	public static final String ORIGEN_AREA = "AREA";
	public static final String ORIGEN_DIGITADO = "DIGITADO";
	/** Parte digitada y parte de egresos: hoy solo la nomina. */
	public static final String ORIGEN_MIXTO = "MIXTO";

	/** Un concepto de la pestana, con de donde sale su valor. */
	public static class Concepto {
		public int idConcepto = 0;
		public String nombre = "";
		public int orden = 0;
		public String origen = "";
	}

	/** Lo que vale un concepto en una tienda y una semana. */
	public static class Valor {
		public int idTienda = 0;
		public int idConcepto = 0;
		/** El domingo que identifica la semana. */
		public String semana = "";
		public double valor = 0;
	}

	/** Los conceptos activos, en el orden de la pestana. */
	public static ArrayList<Concepto> listarConceptos() {
		final ArrayList<Concepto> lista = new ArrayList<Concepto>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idconcepto, nombre, orden, origen"
					+ "  FROM pizzaamericana.concepto_costo"
					+ " WHERE activo = 'S' ORDER BY orden");
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Concepto c = new Concepto();
				c.idConcepto = rs.getInt("idconcepto");
				c.nombre = rs.getString("nombre");
				c.orden = rs.getInt("orden");
				c.origen = rs.getString("origen");
				lista.add(c);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.listarConceptos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * Los conceptos que salen de egresos, semana por semana, para el rango pedido.
	 *
	 * La semana se calcula con el mismo criterio en todas las fuentes: el domingo que
	 * cierra la semana de esa fecha: MOD(8 - DAYOFWEEK, 7), que deja el domingo en si
	 * mismo y el lunes en el domingo siguiente. Un egreso de un lunes cuenta en el
	 * domingo que viene, no en el que paso.
	 *
	 * Un tipo de egreso que no este en concepto_costo_tipo_egreso no entra: el JOIN lo
	 * deja por fuera. Eso es a proposito -ver el SQL- pero hay que saberlo al leer un
	 * total: no es la suma de todo lo que gasto la tienda.
	 */
	public static ArrayList<Valor> egresosPorSemana(final String desde, final String hasta,
			final int idTienda) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT e.idtienda,")
					.append("       m.idconcepto,")
					.append("       DATE_ADD(e.fecha, INTERVAL MOD(8 - DAYOFWEEK(e.fecha), 7) DAY) AS semana,")
					.append("       SUM(e.valoregreso) AS valor")
					.append("  FROM datamart.egreso_historico_tienda e")
					.append("  JOIN pizzaamericana.concepto_costo_tipo_egreso m")
					.append("    ON m.idtipo_egreso = e.idtipo_egreso")
					.append(" WHERE e.fecha BETWEEN ? AND ?");
			if (idTienda > 0) {
				sql.append(" AND e.idtienda = ?");
			}
			sql.append(" GROUP BY e.idtienda, m.idconcepto, semana");
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, desde);
			ps.setString(2, hasta);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Valor v = new Valor();
				v.idTienda = rs.getInt("idtienda");
				v.idConcepto = rs.getInt("idconcepto");
				v.semana = rs.getString("semana");
				v.valor = rs.getDouble("valor");
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.egresosPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * La mensajeria externa, que ya viene calculada por tienda y por semana.
	 *
	 * Esta tabla la escribe el proceso semanal de Servicios con las horas de los
	 * domiciliarios de empresa temporal por la tarifa de cada empresa. No se recalcula
	 * aqui a proposito: si el costo de una semana cambiara segun quien lo consulte, dos
	 * reportes del mismo periodo darian distinto.
	 *
	 * La fecha de esa tabla ya es el domingo de la semana, igual que aqui.
	 */
	public static ArrayList<Valor> mensajeriaPorSemana(final String desde, final String hasta,
			final int idTienda, final int idConcepto) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT idtienda, fecha AS semana, valor")
					.append("  FROM inventarioamericana.gasto_empleado_temporal")
					.append(" WHERE fecha BETWEEN ? AND ?");
			if (idTienda > 0) {
				sql.append(" AND idtienda = ?");
			}
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, desde);
			ps.setString(2, hasta);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Valor v = new Valor();
				v.idTienda = rs.getInt("idtienda");
				v.idConcepto = idConcepto;
				v.semana = rs.getString("semana");
				v.valor = rs.getDouble("valor");
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.mensajeriaPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * La comision de datafono: la venta con tarjeta de la semana por la tarifa.
	 *
	 * Se usa el cierre del POS y no el de Redeban porque el de Redeban trae lo que el
	 * banco consigno, que en varias tiendas difiere del cierre de la tienda hasta en un
	 * 12% -eso es un descuadre, no una comision-. La base de la comision es lo que la
	 * tienda vendio con tarjeta.
	 *
	 * Si la tarifa esta en cero devuelve la lista vacia, para que la pantalla muestre
	 * "sin parametrizar" en vez de un cero que se lee como "no hubo comision".
	 */
	public static ArrayList<Valor> datafonoPorSemana(final String desde, final String hasta,
			final int idTienda, final int idConcepto, final double tarifaPorciento) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		if (tarifaPorciento <= 0) {
			return lista;
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT idtienda,")
					.append("       DATE_ADD(fecha, INTERVAL MOD(8 - DAYOFWEEK(fecha), 7) DAY) AS semana,")
					.append("       SUM(valor_cierre) AS base")
					.append("  FROM datamart.cierre_datafono_pos")
					.append(" WHERE fecha BETWEEN ? AND ?");
			if (idTienda > 0) {
				sql.append(" AND idtienda = ?");
			}
			sql.append(" GROUP BY idtienda, semana");
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, desde);
			ps.setString(2, hasta);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Valor v = new Valor();
				v.idTienda = rs.getInt("idtienda");
				v.idConcepto = idConcepto;
				v.semana = rs.getString("semana");
				v.valor = rs.getDouble("base") * tarifaPorciento / 100.0D;
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.datafonoPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * Lo digitado del mes, repartido entre las semanas del rango.
	 *
	 * Lo que se digita es mensual y lo que se muestra es semanal, asi que hay que
	 * repartirlo. Se reparte parejo entre las semanas que de verdad tenga el mes dentro
	 * del rango, no entre cuatro fijas: un mes de cinco semanas no puede quedar con la
	 * quinta en cero.
	 *
	 * Ojo al leer una semana sola: si el rango corta un mes por la mitad, lo digitado
	 * de ese mes se reparte solo entre las semanas que quedaron dentro, y cada una sale
	 * mas alta de lo que seria en el mes completo.
	 */
	public static ArrayList<Valor> digitadoPorSemana(final String desde, final String hasta,
			final int idTienda) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		final ArrayList<String> semanas = semanasConVenta(desde, hasta);
		if (semanas.isEmpty()) {
			return lista;
		}
		// Cuantas semanas del rango cayeron en cada mes: lo de un mes se reparte solo
		// entre sus propias semanas.
		final LinkedHashMap<String, Integer> semanasDelMes = new LinkedHashMap<String, Integer>();
		for (final String s : semanas) {
			final String mes = s.substring(0, 7);
			final Integer n = semanasDelMes.get(mes);
			semanasDelMes.put(mes, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
		}

		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT idtienda, idconcepto, anio, mes, valor")
					.append("  FROM pizzaamericana.costo_tienda_digitado")
					.append(" WHERE CONCAT(anio, '-', LPAD(mes, 2, '0')) BETWEEN ? AND ?");
			if (idTienda > 0) {
				sql.append(" AND idtienda = ?");
			}
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, desde.substring(0, 7));
			ps.setString(2, hasta.substring(0, 7));
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final String mes = String.format("%04d-%02d", Integer.valueOf(rs.getInt("anio")),
						Integer.valueOf(rs.getInt("mes")));
				final Integer cuantas = semanasDelMes.get(mes);
				if (cuantas == null || cuantas.intValue() == 0) {
					continue;
				}
				final int idTiendaFila = rs.getInt("idtienda");
				final int idConceptoFila = rs.getInt("idconcepto");
				final double porSemana = rs.getDouble("valor") / cuantas.intValue();
				for (final String s : semanas) {
					if (!s.startsWith(mes)) {
						continue;
					}
					final Valor v = new Valor();
					v.idTienda = idTiendaFila;
					v.idConcepto = idConceptoFila;
					v.semana = s;
					v.valor = porSemana;
					lista.add(v);
				}
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.digitadoPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * Las semanas del rango, tomadas de la venta semanal.
	 *
	 * Se sacan de ahi y no de un calendario para que el costo y la venta hablen de las
	 * mismas semanas. Si una semana no tiene venta registrada no existe para esta
	 * pantalla, porque no habria contra que medir el porcentaje.
	 */
	public static ArrayList<String> semanasConVenta(final String desde, final String hasta) {
		final ArrayList<String> lista = new ArrayList<String>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT DISTINCT fecha FROM datamart.venta_semanal_tienda"
					+ " WHERE fecha BETWEEN ? AND ? ORDER BY fecha");
			ps.setString(1, desde);
			ps.setString(2, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lista.add(rs.getString("fecha"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.semanasConVenta: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/** La venta de cada tienda en cada semana del rango: el denominador del porcentaje. */
	public static ArrayList<Valor> ventaPorSemana(final String desde, final String hasta,
			final int idTienda) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT idtienda, fecha AS semana, valor")
					.append("  FROM datamart.venta_semanal_tienda")
					.append(" WHERE fecha BETWEEN ? AND ?");
			if (idTienda > 0) {
				sql.append(" AND idtienda = ?");
			}
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			ps.setString(1, desde);
			ps.setString(2, hasta);
			if (idTienda > 0) {
				ps.setInt(3, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Valor v = new Valor();
				v.idTienda = rs.getInt("idtienda");
				v.semana = rs.getString("semana");
				v.valor = rs.getDouble("valor");
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.ventaPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * La tarifa del datafono, en porcentaje.
	 *
	 * No se lee con ParametrosDAO.obtenerParametro a proposito: ese metodo trae
	 * valornumericod con getInt, asi que una tarifa de 1,5 llegaria como 1 y la
	 * comision saldria un tercio por debajo sin que nada avise.
	 */
	public static double tarifaDatafono() {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT valornumericod FROM general.parametros"
					+ " WHERE valorparametro = 'COMISIONDATAFONO'");
			final ResultSet rs = ps.executeQuery();
			double tarifa = 0;
			if (rs.next()) {
				tarifa = rs.getDouble(1);
			}
			rs.close();
			ps.close();
			return tarifa;
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.tarifaDatafono: " + e.toString());
			return 0;
		} finally {
			cerrar(cn);
		}
	}

	/** Guarda uno de los valores que toca digitar. */
	public static boolean guardarDigitado(final int idTienda, final int idConcepto, final int anio,
			final int mes, final double valor, final String usuario) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO pizzaamericana.costo_tienda_digitado"
					+ " (idtienda, idconcepto, anio, mes, valor, usuario)"
					+ " VALUES (?, ?, ?, ?, ?, ?)"
					+ " ON DUPLICATE KEY UPDATE valor = ?, usuario = ?");
			ps.setInt(1, idTienda);
			ps.setInt(2, idConcepto);
			ps.setInt(3, anio);
			ps.setInt(4, mes);
			ps.setDouble(5, valor);
			ps.setString(6, usuario);
			ps.setDouble(7, valor);
			ps.setString(8, usuario);
			final int filas = ps.executeUpdate();
			ps.close();
			return filas > 0;
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.guardarDigitado: " + e.toString());
			return false;
		} finally {
			cerrar(cn);
		}
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.cerrar: " + e.toString());
		}
	}
}
