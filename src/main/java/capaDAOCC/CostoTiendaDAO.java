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
	public static final String ORIGEN_AREA = "AREA";
	public static final String ORIGEN_DIGITADO = "DIGITADO";
	/** Parte digitada y parte de egresos: hoy solo la nomina. */
	public static final String ORIGEN_MIXTO = "MIXTO";
	/** Sale de inventarioamericana.gasto_semanal: las comisiones y el datafono. */
	public static final String ORIGEN_SEMANAL = "SEMANAL";

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

	/**
	 * Las tiendas que tienen venta en el rango, con su nombre.
	 *
	 * Se sacan de la venta y no del catalogo de tiendas a proposito: la bodega y las
	 * tiendas cerradas estan en el catalogo y no tienen nada que hacer en un costo por
	 * punto de venta. Si una tienda abre, aparece sola.
	 */
	public static LinkedHashMap<Integer, String> tiendasConVenta(final String desde,
			final String hasta) {
		final LinkedHashMap<Integer, String> tiendas = new LinkedHashMap<Integer, String>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT DISTINCT v.idtienda, t.nombre"
					+ "  FROM datamart.venta_semanal_tienda v"
					+ "  JOIN pizzaamericana.tienda t ON t.idtienda = v.idtienda"
					+ " WHERE v.fecha BETWEEN ? AND ? AND v.valor > 0"
					+ " ORDER BY t.nombre");
			ps.setString(1, desde);
			ps.setString(2, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				tiendas.put(Integer.valueOf(rs.getInt("idtienda")), rs.getString("nombre"));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.tiendasConVenta: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return tiendas;
	}

	/**
	 * Cuantos domicilios hizo cada tienda en cada semana.
	 *
	 * Sirve para dividir: un DOMICILIOS de tres millones no dice nada hasta que se sabe
	 * si fueron mil domicilios o dos mil. Con esto la pantalla saca el costo por
	 * domicilio, que es con lo que se comparan dos tiendas de tamanos distintos.
	 *
	 * SE CUENTA AQUI Y NO SE LEE DE gasto_semanal, A PROPOSITO
	 *
	 * El concepto 1 de gasto_configuracion, "Total domicilios", tiene este mismo
	 * conteo. No se usa por dos razones. La primera es que ese catalogo tiene conceptos
	 * marcados "NO USAR" y una bandera activo que otros reportes interpretan a su
	 * manera; amarrar el costo por tienda a eso es quedar expuesto a que alguien apague
	 * una fila por otro motivo y aqui desaparezca un numero sin aviso. La segunda es
	 * que el conteo se calcula contra la base de cada tienda, y el datamart ya lo tiene
	 * centralizado. Medido contra septiembre de 2026, las once tiendas, las dos fuentes
	 * difieren a lo sumo en un pedido de cada mil setecientos.
	 *
	 * Por lo mismo, comisionesPorSemana NO filtra por activo: lo que decide que entra
	 * al costo es concepto_costo_gasto_semanal, que es nuestra, y nada mas.
	 */
	public static ArrayList<Valor> domiciliosPorSemana(final String desde, final String hasta,
			final int idTienda) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT idtienda,")
					.append("       DATE_ADD(fechapedido, INTERVAL MOD(8 - DAYOFWEEK(fechapedido), 7) DAY)")
					.append("         AS semana,")
					.append("       COUNT(*) AS cuantos")
					.append("  FROM datamart.pedido")
					.append(" WHERE idtipopedido = 1 AND fechapedido BETWEEN ? AND ?");
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
				v.semana = rs.getString("semana");
				v.valor = rs.getDouble("cuantos");
				lista.add(v);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.domiciliosPorSemana: " + e.toString());
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
	 * Las comisiones y la comision de datafonos, que ya vienen calculadas por tienda y
	 * por semana en inventarioamericana.gasto_semanal.
	 *
	 * LA SEMANA SE CRUZA CONTRA SU DOMINGO, Y ESO ES LO QUE EVITA DUPLICAR RAPPI
	 *
	 * El concepto 18 -comision de Rappi- lo escriben dos procesos con cortes distintos:
	 * ReporteSemanalRappi corta domingo, de lunes a domingo, y ReporteSemanalPagoRappi
	 * corta miercoles, de sabado a viernes. Los dos guardan en el mismo concepto, asi
	 * que en gasto_semanal hay una fila de domingo y una de viernes por la MISMA plata.
	 * En 2026 hasta septiembre el domingo sumaba 250 millones y el viernes 247.
	 *
	 * El JOIN contra las semanas -que son domingos- deja por fuera las del viernes. No
	 * es casualidad y no se puede cambiar por un BETWEEN de fechas: con un BETWEEN,
	 * Rappi se contaria dos veces.
	 *
	 * aplica_porcentaje distingue el concepto cuyo valor_gasto YA es la comision del
	 * que trae la base. Hoy solo el 24, la venta con tarjeta, trae base.
	 */
	public static ArrayList<Valor> comisionesPorSemana(final ArrayList<String> semanas,
			final int idTienda) {
		final ArrayList<Valor> lista = new ArrayList<Valor>();
		if (semanas == null || semanas.isEmpty()) {
			return lista;
		}
		final StringBuilder enSemanas = new StringBuilder();
		for (int i = 0; i < semanas.size(); i++) {
			enSemanas.append(i == 0 ? "?" : ",?");
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final StringBuilder sql = new StringBuilder()
					.append("SELECT g.idtienda, m.idconcepto, g.fecha AS semana,")
					.append("       SUM(CASE WHEN m.aplica_porcentaje = 'S'")
					.append("                THEN g.valor_gasto * c.porcentaje_gasto / 100")
					.append("                ELSE g.valor_gasto END) AS valor")
					.append("  FROM inventarioamericana.gasto_semanal g")
					.append("  JOIN pizzaamericana.concepto_costo_gasto_semanal m")
					.append("    ON m.idgasto_conf = g.idgasto_conf")
					.append("  JOIN inventarioamericana.gasto_configuracion c")
					.append("    ON c.idgasto_conf = g.idgasto_conf")
					.append(" WHERE g.fecha IN (").append(enSemanas).append(")");
			if (idTienda > 0) {
				sql.append(" AND g.idtienda = ?");
			}
			sql.append(" GROUP BY g.idtienda, m.idconcepto, g.fecha");
			final PreparedStatement ps = cn.prepareStatement(sql.toString());
			int p = 1;
			for (final String s : semanas) {
				ps.setString(p++, s);
			}
			if (idTienda > 0) {
				ps.setInt(p, idTienda);
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
			LOG.error("CostoTiendaDAO.comisionesPorSemana: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * El detalle de las comisiones: que puso cada concepto de gasto_semanal.
	 *
	 * Vale la pena verlo desarmado: COMISION VIRTUAL junta Wompi, Rapyd, Rappi, DiDi y
	 * el QR de Bancolombia, y lo que hace subir el total casi siempre es una sola de
	 * las cinco.
	 */
	public static ArrayList<Detalle> detalleComisiones(final ArrayList<String> semanas,
			final int idTienda, final int idConcepto) {
		final ArrayList<Detalle> lista = new ArrayList<Detalle>();
		if (semanas == null || semanas.isEmpty()) {
			return lista;
		}
		final StringBuilder enSemanas = new StringBuilder();
		for (int i = 0; i < semanas.size(); i++) {
			enSemanas.append(i == 0 ? "?" : ",?");
		}
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT g.fecha, c.nombre_gasto, c.porcentaje_gasto, m.aplica_porcentaje,"
					+ "       g.valor_calculo, g.valor_gasto"
					+ "  FROM inventarioamericana.gasto_semanal g"
					+ "  JOIN pizzaamericana.concepto_costo_gasto_semanal m"
					+ "    ON m.idgasto_conf = g.idgasto_conf AND m.idconcepto = ?"
					+ "  JOIN inventarioamericana.gasto_configuracion c"
					+ "    ON c.idgasto_conf = g.idgasto_conf"
					+ " WHERE g.idtienda = ? AND g.fecha IN (" + enSemanas + ")"
					+ " ORDER BY g.fecha, c.idgasto_conf");
			ps.setInt(1, idConcepto);
			ps.setInt(2, idTienda);
			int p = 3;
			for (final String s : semanas) {
				ps.setString(p++, s);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Detalle d = new Detalle();
				final boolean aplica = "S".equals(rs.getString("aplica_porcentaje"));
				final double bruto = rs.getDouble("valor_gasto");
				d.fecha = rs.getString("fecha");
				d.descripcion = rs.getString("nombre_gasto");
				d.referencia = aplica
						? "Base " + Math.round(bruto) + " por "
							+ rs.getDouble("porcentaje_gasto") + "%"
						: "Comision ya liquidada";
				d.valor = aplica ? bruto * rs.getDouble("porcentaje_gasto") / 100.0D : bruto;
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.detalleComisiones: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/** Un renglon del detalle: de donde salio un concepto en una tienda y un periodo. */
	public static class Detalle {
		public String fecha = "";
		public String descripcion = "";
		public String referencia = "";
		public double valor = 0;
	}

	/**
	 * El detalle de un concepto de egresos: los movimientos uno por uno.
	 *
	 * Esto es lo que sostiene el numero. Si alguien discute una cifra, aqui estan las
	 * filas con su fecha, su descripcion y quien las registro.
	 */
	public static ArrayList<Detalle> detalleEgresos(final String desde, final String hasta,
			final int idTienda, final int idConcepto) {
		final ArrayList<Detalle> lista = new ArrayList<Detalle>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT e.fecha, e.descripcion, e.usuario, e.valoregreso,"
					+ "       t.nombre_tipo_egreso"
					+ "  FROM datamart.egreso_historico_tienda e"
					+ "  JOIN pizzaamericana.concepto_costo_tipo_egreso m"
					+ "    ON m.idtipo_egreso = e.idtipo_egreso AND m.idconcepto = ?"
					+ "  LEFT JOIN datamart.tipo_egreso t ON t.idtipo_egreso = e.idtipo_egreso"
					+ " WHERE e.idtienda = ? AND e.fecha BETWEEN ? AND ?"
					+ " ORDER BY e.fecha, e.idegreso");
			ps.setInt(1, idConcepto);
			ps.setInt(2, idTienda);
			ps.setString(3, desde);
			ps.setString(4, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Detalle d = new Detalle();
				d.fecha = rs.getString("fecha");
				d.descripcion = rs.getString("nombre_tipo_egreso") + " - " + rs.getString("descripcion");
				d.referencia = rs.getString("usuario");
				d.valor = rs.getDouble("valoregreso");
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.detalleEgresos: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	/**
	 * El detalle de la mensajeria externa: los turnos de los domiciliarios temporales.
	 *
	 * Se muestran los turnos y no la suma semanal porque la suma ya esta arriba; lo que
	 * se viene a ver aqui es quien, de que empresa y cuantas horas. Las horas se
	 * calculan igual que en el POS -MOD con 24 horas- para que un turno que cruza la
	 * medianoche no salga negativo.
	 *
	 * El valor del turno NO se recalcula aqui: la suma de estos renglones puede no dar
	 * exactamente el total de la semana, porque el total lo escribio el proceso de
	 * Servicios con las tarifas que estaban vigentes ese dia.
	 */
	public static ArrayList<Detalle> detalleTemporales(final String desde, final String hasta,
			final int idTienda) {
		final ArrayList<Detalle> lista = new ArrayList<Detalle>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT d.fecha_sistema, d.nombre, d.empresa, d.horaingreso, d.horasalida,"
					+ "       MOD(TIME_TO_SEC(d.horasalida) - TIME_TO_SEC(d.horaingreso) + 86400, 86400)"
					+ "         / 3600 AS horas,"
					+ "       IFNULL(e.valor_hora_normal, 0) AS valor_hora"
					+ "  FROM datamart.empleado_temporal_dia d"
					+ "  LEFT JOIN general.empresa_temporal e ON e.idempresa = d.idempresa"
					+ " WHERE d.idtienda = ? AND d.fecha_sistema BETWEEN ? AND ?"
					+ "   AND d.horasalida IS NOT NULL AND d.horasalida <> ''"
					+ " ORDER BY d.fecha_sistema, d.horaingreso");
			ps.setInt(1, idTienda);
			ps.setString(2, desde);
			ps.setString(3, hasta);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Detalle d = new Detalle();
				final double horas = rs.getDouble("horas");
				d.fecha = rs.getString("fecha_sistema");
				d.descripcion = rs.getString("nombre") + " (" + rs.getString("empresa") + ")";
				d.referencia = rs.getString("horaingreso") + " a " + rs.getString("horasalida")
						+ ", " + String.format("%.1f", Double.valueOf(horas)) + " horas";
				d.valor = horas * rs.getDouble("valor_hora");
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.detalleTemporales: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}


	/** El detalle de un concepto digitado: el valor de cada mes y quien lo digito. */
	public static ArrayList<Detalle> detalleDigitado(final String desde, final String hasta,
			final int idTienda, final int idConcepto) {
		final ArrayList<Detalle> lista = new ArrayList<Detalle>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT anio, mes, valor, usuario, fecha_cambio"
					+ "  FROM pizzaamericana.costo_tienda_digitado"
					+ " WHERE idtienda = ? AND idconcepto = ?"
					+ "   AND CONCAT(anio, '-', LPAD(mes, 2, '0')) BETWEEN ? AND ?"
					+ " ORDER BY anio, mes");
			ps.setInt(1, idTienda);
			ps.setInt(2, idConcepto);
			ps.setString(3, desde.substring(0, 7));
			ps.setString(4, hasta.substring(0, 7));
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Detalle d = new Detalle();
				d.fecha = String.format("%04d-%02d", Integer.valueOf(rs.getInt("anio")),
						Integer.valueOf(rs.getInt("mes")));
				d.descripcion = "Valor digitado del mes";
				d.referencia = "Lo digito " + rs.getString("usuario") + " el "
						+ rs.getString("fecha_cambio");
				d.valor = rs.getDouble("valor");
				lista.add(d);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("CostoTiendaDAO.detalleDigitado: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
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
