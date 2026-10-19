package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Lo facturado electronicamente y las notas credito de cada tienda en un mes, leido del DATAMART.
 *
 * La tabla de cada tienda llega al datamart por la replica diaria (ServicioReplicaPedidos), asi que el reporte
 * no depende de que las tiendas esten encendidas ni las consulta en vivo.
 *
 * QUE SE CUENTA
 *
 * - Facturas: las que la DIAN acepto (validacion_dian = 1), por su fecha de EMISION.
 * - Notas credito: las que la DIAN acepto, por SU fecha de emision. Una nota credito de hoy sobre una factura de
 *   hace dos meses resta hoy: asi lo registra la contabilidad.
 * - Neto = facturado - notas credito. Cuando se anula una factura y se vuelve a emitir a nombre de otra persona
 *   el "facturado" sube dos veces (la original y la nueva) y la nota credito resta la original: el neto queda en
 *   lo que de verdad se vendio.
 *
 * EL VALOR
 *
 * Las facturas emitidas antes de la migracion 2026_10_06_01 no guardaron su valor (queda NULL). Para esas se usa el
 * total del pedido, si el datamart lo tiene, y se cuentan aparte (sin_valor) las que ni asi se pudo valorar.
 * El impuesto al consumo es el 8%: si la factura no trae el impuesto separado se saca del total.
 */
public class FacturacionElectronicaDAO {

	private static final Logger logger = Logger.getLogger("log_file");

	/** Las cifras de un conjunto de documentos (facturas o notas credito) de una tienda. */
	public static class Cifras {
		public int cantidad;
		public double base;
		public double impuesto;
		public double total;
		/** Documentos que no se pudieron valorar: ni valor propio ni pedido en el datamart. */
		public int sinValor;
	}

	public static class ResumenTienda {
		public int idTienda;
		public Cifras facturas = new Cifras();
		public Cifras notas = new Cifras();
		/** Facturas que la DIAN rechazo o que nunca se validaron; no suman. */
		public int facturasNoAceptadas;
		/** Notas credito rechazadas o sin validar; no restan. */
		public int notasNoAceptadas;
		/** Dias del mes que la replica tiene bien copiados (OK, YA o CERO) para las facturas. */
		public int diasConDatos;
	}

	/** Una nota credito del mes, para el detalle. */
	public static class NotaDetalle {
		public int idTienda;
		public String fecha;
		public String hora;
		public String documentoNota;
		public String documentoFactura;
		/** Fecha de emision de la factura anulada, "" si el datamart no la tiene. */
		public String fechaFactura;
		public int idPedidoTienda;
		public String motivo;
		public double total;
	}

	/** Expresion SQL del valor con impuestos de una factura f, con respaldo en el total del pedido p. */
	private static final String TOTAL_FACTURA = "COALESCE(f.valor_total, p.total_neto)";

	/**
	 * @param desde yyyy-MM-dd primer dia del mes
	 * @param hasta yyyy-MM-dd ultimo dia del mes
	 * @return el resumen por tienda, o null si no se pudo consultar el datamart (distinto de "sin facturas")
	 */
	public static Map<Integer, ResumenTienda> resumen(final String desde, final String hasta) {
		final Map<Integer, ResumenTienda> mapa = new HashMap<Integer, ResumenTienda>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		if (cn == null) {
			return null;
		}
		try {
			// Facturas aceptadas, valoradas con el total del pedido cuando la factura no trae el suyo.
			String sql = "SELECT f.idtienda, COUNT(*) cantidad, "
					+ "SUM(CASE WHEN " + TOTAL_FACTURA + " IS NULL THEN 1 ELSE 0 END) sin_valor, "
					+ "IFNULL(SUM(" + TOTAL_FACTURA + "), 0) total, "
					+ "IFNULL(SUM(COALESCE(f.valor_impuesto, " + TOTAL_FACTURA + " - " + TOTAL_FACTURA + " / 1.08)), 0) impuesto "
					+ "FROM factura_electronica_generada f "
					+ "LEFT JOIN pedido p ON p.idtienda = f.idtienda AND p.idpedidotienda = f.idpedidotienda "
					+ "WHERE f.fecha >= ? AND f.fecha <= ? AND f.validacion_dian = 1 AND f.cufe <> '' "
					+ "GROUP BY f.idtienda";
			try (PreparedStatement ps = cn.prepareStatement(sql)) {
				ps.setString(1, desde);
				ps.setString(2, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						llenar(de(mapa, rs.getInt("idtienda")).facturas, rs);
					}
				}
			}
			// Notas credito aceptadas; si la nota no trae valor se usa el de la factura que anula.
			sql = "SELECT n.idtienda, COUNT(*) cantidad, "
					+ "SUM(CASE WHEN COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + ") IS NULL THEN 1 ELSE 0 END) sin_valor, "
					+ "IFNULL(SUM(COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + ")), 0) total, "
					+ "IFNULL(SUM(COALESCE(n.valor_impuesto, COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + ") "
					+ "- COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + ") / 1.08)), 0) impuesto "
					+ "FROM nota_credito_electronica_generada n "
					+ "LEFT JOIN factura_electronica_generada f ON f.idtienda = n.idtienda AND f.cufe = n.cufe_factura AND n.cufe_factura <> '' "
					+ "LEFT JOIN pedido p ON p.idtienda = f.idtienda AND p.idpedidotienda = f.idpedidotienda "
					+ "WHERE n.fecha >= ? AND n.fecha <= ? AND n.validacion_dian = 1 "
					+ "GROUP BY n.idtienda";
			try (PreparedStatement ps = cn.prepareStatement(sql)) {
				ps.setString(1, desde);
				ps.setString(2, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						llenar(de(mapa, rs.getInt("idtienda")).notas, rs);
					}
				}
			}
			// Lo que no suma: rechazadas o sin validar.
			contarNoAceptadas(cn, mapa, desde, hasta, "factura_electronica_generada", true);
			contarNoAceptadas(cn, mapa, desde, hasta, "nota_credito_electronica_generada", false);
			// Cobertura de la replica: cuantos dias del mes estan copiados.
			sql = "SELECT idtienda, COUNT(DISTINCT fecha_datos) dias FROM replica_log "
					+ "WHERE tabla = 'factura_electronica_generada' AND fecha_datos >= ? AND fecha_datos <= ? "
					+ "AND estado IN ('OK', 'YA', 'CERO') GROUP BY idtienda";
			try (PreparedStatement ps = cn.prepareStatement(sql)) {
				ps.setString(1, desde);
				ps.setString(2, hasta);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						de(mapa, rs.getInt("idtienda")).diasConDatos = rs.getInt("dias");
					}
				}
			} catch (final Exception e) {
				// Sin replica_log no hay cobertura, pero el reporte sale igual.
				logger.info("FacturacionElectronicaDAO: sin cobertura de la replica: " + e);
			}
			cn.close();
			return mapa;
		} catch (final Exception e) {
			logger.error("FacturacionElectronicaDAO.resumen: " + e.toString());
			try {
				cn.close();
			} catch (final Exception e1) {
				// Ya estaba cerrada.
			}
			return null;
		}
	}

	/** Las notas credito aceptadas del mes, una por una. */
	public static ArrayList<NotaDetalle> notasDelMes(final String desde, final String hasta) {
		final ArrayList<NotaDetalle> lista = new ArrayList<NotaDetalle>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		if (cn == null) {
			return lista;
		}
		final String sql = "SELECT n.idtienda, n.fecha, n.hora, n.documento_nota, n.documento_factura, n.idpedidotienda, "
				+ "n.motivo, COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + ") total, f.fecha fecha_factura "
				+ "FROM nota_credito_electronica_generada n "
				+ "LEFT JOIN factura_electronica_generada f ON f.idtienda = n.idtienda AND f.cufe = n.cufe_factura AND n.cufe_factura <> '' "
				+ "LEFT JOIN pedido p ON p.idtienda = f.idtienda AND p.idpedidotienda = f.idpedidotienda "
				+ "WHERE n.fecha >= ? AND n.fecha <= ? AND n.validacion_dian = 1 "
				+ "ORDER BY n.fecha, n.hora, n.idtienda";
		try (PreparedStatement ps = cn.prepareStatement(sql)) {
			ps.setString(1, desde);
			ps.setString(2, hasta);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final NotaDetalle n = new NotaDetalle();
					n.idTienda = rs.getInt("idtienda");
					n.fecha = rs.getString("fecha");
					n.hora = rs.getString("hora");
					n.documentoNota = rs.getString("documento_nota");
					n.documentoFactura = rs.getString("documento_factura");
					n.fechaFactura = rs.getString("fecha_factura") == null ? "" : rs.getString("fecha_factura");
					n.idPedidoTienda = rs.getInt("idpedidotienda");
					n.motivo = rs.getString("motivo");
					n.total = rs.getDouble("total");
					lista.add(n);
				}
			}
			cn.close();
		} catch (final Exception e) {
			logger.error("FacturacionElectronicaDAO.notasDelMes: " + e.toString());
			try {
				cn.close();
			} catch (final Exception e1) {
				// Ya estaba cerrada.
			}
		}
		return lista;
	}

	/**
	 * Todos los documentos del mes -facturas y notas credito, aceptados o no- en filas de texto para el CSV que se
	 * le entrega a la contadora. La primera fila son los encabezados.
	 */
	public static ArrayList<String[]> documentosDelMes(final String desde, final String hasta) {
		final ArrayList<String[]> filas = new ArrayList<String[]>();
		filas.add(new String[] { "tipo", "idtienda", "fecha", "hora", "documento", "pedido", "factura_anulada",
				"valor_sin_impuestos", "impuesto", "valor_total", "estado_dian", "motivo" });
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDDatamartLocal();
		if (cn == null) {
			return filas;
		}
		final String sql = "SELECT 'FACTURA' tipo, f.idtienda, f.fecha, f.hora, CONCAT(f.prefijo, f.numerodocumento) documento, "
				+ "f.idpedidotienda pedido, '' factura_anulada, "
				+ "COALESCE(f.valor_sin_impuestos, " + TOTAL_FACTURA + " / 1.08) sin_imp, "
				+ "COALESCE(f.valor_impuesto, " + TOTAL_FACTURA + " - " + TOTAL_FACTURA + " / 1.08) impuesto, "
				+ TOTAL_FACTURA + " total, f.validacion_dian estado, '' motivo "
				+ "FROM factura_electronica_generada f "
				+ "LEFT JOIN pedido p ON p.idtienda = f.idtienda AND p.idpedidotienda = f.idpedidotienda "
				+ "WHERE f.fecha >= ? AND f.fecha <= ? AND f.cufe <> '' "
				+ "UNION ALL "
				+ "SELECT 'NOTA CREDITO', n.idtienda, n.fecha, n.hora, n.documento_nota, n.idpedidotienda, n.documento_factura, "
				+ "n.valor_sin_impuestos, n.valor_impuesto, COALESCE(NULLIF(n.valor_total, 0), " + TOTAL_FACTURA + "), "
				+ "n.validacion_dian, n.motivo "
				+ "FROM nota_credito_electronica_generada n "
				+ "LEFT JOIN factura_electronica_generada f ON f.idtienda = n.idtienda AND f.cufe = n.cufe_factura AND n.cufe_factura <> '' "
				+ "LEFT JOIN pedido p ON p.idtienda = f.idtienda AND p.idpedidotienda = f.idpedidotienda "
				+ "WHERE n.fecha >= ? AND n.fecha <= ? "
				+ "ORDER BY idtienda, fecha, hora";
		try (PreparedStatement ps = cn.prepareStatement(sql)) {
			ps.setString(1, desde);
			ps.setString(2, hasta);
			ps.setString(3, desde);
			ps.setString(4, hasta);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final String estado = rs.getString("estado");
					filas.add(new String[] { rs.getString("tipo"), rs.getString("idtienda"), rs.getString("fecha"),
							rs.getString("hora"), rs.getString("documento"), rs.getString("pedido"),
							rs.getString("factura_anulada"), plano(rs.getString("sin_imp")), plano(rs.getString("impuesto")),
							plano(rs.getString("total")),
							"1".equals(estado) ? "ACEPTADO" : ("0".equals(estado) ? "RECHAZADO" : "SIN VALIDAR"),
							rs.getString("motivo") == null ? "" : rs.getString("motivo") });
				}
			}
			cn.close();
		} catch (final Exception e) {
			logger.error("FacturacionElectronicaDAO.documentosDelMes: " + e.toString());
			try {
				cn.close();
			} catch (final Exception e1) {
				// Ya estaba cerrada.
			}
		}
		return filas;
	}

	private static String plano(final String numero) {
		if (numero == null) {
			return "";
		}
		try {
			return String.valueOf(Math.round(Double.parseDouble(numero) * 100.0) / 100.0);
		} catch (final Exception e) {
			return numero;
		}
	}

	private static void contarNoAceptadas(final Connection cn, final Map<Integer, ResumenTienda> mapa,
			final String desde, final String hasta, final String tabla, final boolean facturas) throws Exception {
		final String condicionCufe = facturas ? " AND cufe <> ''" : "";
		try (PreparedStatement ps = cn.prepareStatement("SELECT idtienda, COUNT(*) c FROM " + tabla
				+ " WHERE fecha >= ? AND fecha <= ? AND (validacion_dian IS NULL OR validacion_dian <> 1)" + condicionCufe
				+ " GROUP BY idtienda")) {
			ps.setString(1, desde);
			ps.setString(2, hasta);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final ResumenTienda r = de(mapa, rs.getInt("idtienda"));
					if (facturas) {
						r.facturasNoAceptadas = rs.getInt("c");
					} else {
						r.notasNoAceptadas = rs.getInt("c");
					}
				}
			}
		}
	}

	private static void llenar(final Cifras c, final ResultSet rs) throws Exception {
		c.cantidad = rs.getInt("cantidad");
		c.sinValor = rs.getInt("sin_valor");
		c.total = rs.getDouble("total");
		c.impuesto = rs.getDouble("impuesto");
		c.base = c.total - c.impuesto;
	}

	private static ResumenTienda de(final Map<Integer, ResumenTienda> mapa, final int idTienda) {
		ResumenTienda r = mapa.get(Integer.valueOf(idTienda));
		if (r == null) {
			r = new ResumenTienda();
			r.idTienda = idTienda;
			mapa.put(Integer.valueOf(idTienda), r);
		}
		return r;
	}
}
