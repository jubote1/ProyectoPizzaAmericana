package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

import org.apache.log4j.Logger;

import capaModeloCC.LogEventoBold;
import conexionCC.ConexionBaseDatos;

/**
 * Guarda los eventos que Bold notifica al webhook (tabla log_evento_bold, ver
 * sql/log_evento_bold.sql).
 */
public class LogEventoBoldDAO {

	public static final int GUARDADO = 1;
	/** Ese mismo cuerpo ya estaba guardado: es un reintento de Bold. */
	public static final int DUPLICADO = 0;
	public static final int ERROR = -1;

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
							+ " payment_method, monto_total, moneda, referencia, terminal_id, fecha_evento,"
							+ " firma_valida, motivo_firma, ip_origen, json_evento)"
							+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
			ps.setString(11, recortar(e.getFechaEvento(), 60));
			ps.setBoolean(12, e.isFirmaValida());
			ps.setString(13, recortar(e.getMotivoFirma(), 40));
			ps.setString(14, recortar(e.getIpOrigen(), 60));
			ps.setString(15, e.getJsonEvento());
			ps.executeUpdate();
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
