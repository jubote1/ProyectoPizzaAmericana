package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoBold;
import conexionCC.ConexionBaseDatos;

/**
 * Guarda los eventos que Bold notifica al webhook (tabla log_evento_bold, ver
 * sql/log_evento_bold.sql y sql/bold_eventos_por_tienda.sql) y lleva el estado
 * de su entrega a la tienda.
 */
public class LogEventoBoldDAO {

	public static final int GUARDADO = 1;
	/** Ese mismo cuerpo ya estaba guardado: es un reintento de Bold. */
	public static final int DUPLICADO = 0;
	public static final int ERROR = -1;

	/** Guarda el evento. Si se guardo, deja su id en {@code e.setIdLog}. */
	public static int insertar(final LogEventoBold e) {
		final Logger logger = Logger.getLogger("log_file");
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		if (con1 == null) {
			logger.error("LogEventoBoldDAO: sin conexion a la base");
			return ERROR;
		}
		try {
			final PreparedStatement ps = con1.prepareStatement(
					"insert into log_evento_bold (hash_cuerpo, id_notificacion, tipo_evento, payment_id, merchant_id,"
							+ " payment_method, monto_total, moneda, referencia, terminal_id, seller_email, bold_user_id,"
							+ " fecha_evento, firma_valida, motivo_firma, firma_recibida, content_type, ip_origen, json_evento)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, e.getHashCuerpo());
			ps.setString(2, recortar(e.getIdNotificacion(), 80));
			ps.setString(3, recortar(e.getTipoEvento(), 40));
			ps.setString(4, recortar(e.getPaymentId(), 100));
			ps.setString(5, recortar(e.getMerchantId(), 100));
			ps.setString(6, recortar(e.getPaymentMethod(), 40));
			ps.setBigDecimal(7, e.getMontoTotal());
			ps.setString(8, recortar(e.getMoneda(), 10));
			ps.setString(9, recortar(e.getReferencia(), 200));
			ps.setString(10, recortar(e.getTerminalId(), 100));
			ps.setString(11, recortar(e.getSellerEmail(), 150));
			ps.setString(12, recortar(e.getBoldUserId(), 80));
			ps.setString(13, recortar(e.getFechaEvento(), 60));
			ps.setBoolean(14, e.isFirmaValida());
			ps.setString(15, recortar(e.getMotivoFirma(), 40));
			ps.setString(16, recortar(e.getFirmaRecibida(), 300));
			ps.setString(17, recortar(e.getContentType(), 100));
			ps.setString(18, recortar(e.getIpOrigen(), 60));
			ps.setString(19, e.getJsonEvento());
			ps.executeUpdate();
			final ResultSet llaves = ps.getGeneratedKeys();
			if (llaves.next()) {
				e.setIdLog(llaves.getLong(1));
			}
			llaves.close();
			ps.close();
			con1.close();
			return GUARDADO;
		} catch (final SQLIntegrityConstraintViolationException dup) {
			cerrar(con1);
			return DUPLICADO;
		} catch (final Exception ex) {
			logger.error("LogEventoBoldDAO.insertar: " + ex.toString());
			cerrar(con1);
			return ERROR;
		}
	}

	/** Lo necesario para entregar un evento a su tienda. null si no existe. */
	public static LogEventoBold obtenerParaEntrega(final long idLog) {
		final Logger logger = Logger.getLogger("log_file");
		final Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
		if (con1 == null) {
			return null;
		}
		try {
			final PreparedStatement ps = con1.prepareStatement(
					"select idlog_evento_bold, tipo_evento, payment_id, payment_method, monto_total, moneda,"
							+ " referencia, fecha_evento, fecha_recepcion, seller_email, bold_user_id, idtienda,"
							+ " firma_valida, entregado_tienda from log_evento_bold where idlog_evento_bold = ?");
			ps.setLong(1, idLog);
			final ResultSet rs = ps.executeQuery();
			LogEventoBold e = null;
			if (rs.next()) {
				e = new LogEventoBold();
				e.setIdLog(rs.getLong("idlog_evento_bold"));
				e.setTipoEvento(rs.getString("tipo_evento"));
				e.setPaymentId(rs.getString("payment_id"));
				e.setPaymentMethod(rs.getString("payment_method"));
				e.setMontoTotal(rs.getBigDecimal("monto_total"));
				e.setMoneda(rs.getString("moneda"));
				e.setReferencia(rs.getString("referencia"));
				String fecha = rs.getString("fecha_evento");
				// Sin la fecha del propio evento se usa la de recepcion.
				e.setFechaEvento(fecha != null && !fecha.trim().isEmpty() ? fecha : rs.getString("fecha_recepcion"));
				e.setSellerEmail(rs.getString("seller_email"));
				e.setBoldUserId(rs.getString("bold_user_id"));
				e.setIdTienda(rs.getInt("idtienda"));
				e.setFirmaValida(rs.getBoolean("firma_valida"));
				e.setEntregadoTienda(rs.getBoolean("entregado_tienda"));
			}
			rs.close();
			ps.close();
			con1.close();
			return e;
		} catch (final Exception ex) {
			logger.error("LogEventoBoldDAO.obtenerParaEntrega: " + ex.toString());
			cerrar(con1);
			return null;
		}
	}

	/**
	 * Anota el resultado de un intento de entrega a la tienda. contarIntento es
	 * false cuando ni siquiera se intento (por ejemplo, la sede aun no esta en
	 * bold_sede_tienda): esos casos no deben gastar los intentos, porque se
	 * resuelven cuando alguien registra la sede, no reintentando.
	 */
	public static void registrarIntentoEntrega(final long idLog, final int idTienda, final boolean entregado,
			final String error, final boolean contarIntento) {
		final Logger logger = Logger.getLogger("log_file");
		final Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
		if (con1 == null) {
			return;
		}
		try {
			final PreparedStatement ps = con1.prepareStatement(
					"update log_evento_bold set idtienda = ?, entregado_tienda = ?, intentos_entrega = intentos_entrega + ?,"
							+ " fecha_entrega_tienda = if(?, now(), fecha_entrega_tienda), error_entrega = ?"
							+ " where idlog_evento_bold = ?");
			if (idTienda > 0) {
				ps.setInt(1, idTienda);
			} else {
				ps.setNull(1, java.sql.Types.INTEGER);
			}
			ps.setBoolean(2, entregado);
			ps.setInt(3, contarIntento ? 1 : 0);
			ps.setBoolean(4, entregado);
			ps.setString(5, recortar(error, 200));
			ps.setLong(6, idLog);
			ps.executeUpdate();
			ps.close();
			con1.close();
		} catch (final Exception ex) {
			logger.error("LogEventoBoldDAO.registrarIntentoEntrega: " + ex.toString());
			cerrar(con1);
		}
	}

	/**
	 * Eventos validos (firma correcta) que todavia no se han entregado a su
	 * tienda: los que fallaron porque la tienda estaba apagada y los que
	 * llegaron antes de que su sede estuviera en bold_sede_tienda. Solo los de
	 * los ultimos dias y con un tope de intentos, para no insistir para siempre.
	 */
	public static ArrayList<Long> pendientesDeEntrega(final int maxIntentos, final int diasAtras, final int limite,
			final boolean incluirSinFirmaValida) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<Long> ids = new ArrayList<Long>();
		final Connection con1 = new ConexionBaseDatos().obtenerConexionBDPrincipal();
		if (con1 == null) {
			return ids;
		}
		try {
			final PreparedStatement ps = con1.prepareStatement(
					"select idlog_evento_bold from log_evento_bold where (firma_valida = 1 or ?) and entregado_tienda = 0"
							+ " and intentos_entrega < ? and fecha_recepcion >= date_sub(now(), interval ? day)"
							+ " order by idlog_evento_bold limit ?");
			ps.setBoolean(1, incluirSinFirmaValida);
			ps.setInt(2, maxIntentos);
			ps.setInt(3, diasAtras);
			ps.setInt(4, limite);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(Long.valueOf(rs.getLong(1)));
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception ex) {
			logger.error("LogEventoBoldDAO.pendientesDeEntrega: " + ex.toString());
			cerrar(con1);
		}
		return ids;
	}

	private static String recortar(final String texto, final int maximo) {
		if (texto == null) {
			return null;
		}
		return texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}

	private static void cerrar(final Connection con) {
		try {
			con.close();
		} catch (final Exception ignorada) {
		}
	}

}
