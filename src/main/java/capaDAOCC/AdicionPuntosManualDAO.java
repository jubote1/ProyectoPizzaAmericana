package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Adicion manual de puntos a un cliente del plan de fidelizacion.
 *
 * TODO EN UNA SOLA TRANSACCION. Se hacen tres escrituras y las tres tienen que quedar o ninguna:
 *
 *   1. fidelizacion_transaccion: la acumulacion, que es lo que usan el vencimiento, la redencion
 *      (se debita de la mas vieja) y los avisos de vencimiento. Va con idtienda = 0 e
 *      idpedidotienda NEGATIVO (-1, -2, ...) por cliente: la llave primaria es
 *      (correo, idtienda, idpedidotienda) y un numero negativo nunca choca con un pedido real.
 *      usuario = 'AJUSTE' y origen = 'AJU', para distinguirla de una compra.
 *   2. cliente_fidelizacion.puntos_vigentes: el saldo que muestran las pantallas.
 *   3. fidelizacion_ajuste_manual: quien, cuanto y por que.
 *
 * La vigencia sale del mismo parametro que usa la acumulacion diaria (DIASVIGENCIAPUNTOS, hoy 365).
 * La fila del cliente se bloquea con FOR UPDATE para que dos ajustes seguidos no pisen el saldo.
 */
public class AdicionPuntosManualDAO {

	/** Con que queda marcada la acumulacion en fidelizacion_transaccion. */
	public static final String USUARIO_TRANSACCION = "AJUSTE";
	public static final String ORIGEN = "AJU";

	public static class Resultado {
		public boolean exitoso = false;
		public String error = "";
		public int idAjuste = 0;
		public double saldoAntes = 0;
		public double saldoDespues = 0;
		public String fechaVencimiento = "";
		public int diasVigencia = 0;
	}

	/** Dias de vigencia de un punto, del parametro; 365 si no esta. */
	public static int diasVigencia() {
		final Logger logger = Logger.getLogger("log_file");
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT valornumerico FROM parametros WHERE valorparametro = 'DIASVIGENCIAPUNTOS'");
			final ResultSet rs = ps.executeQuery();
			int dias = 365;
			if (rs.next() && rs.getInt(1) > 0) {
				dias = rs.getInt(1);
			}
			rs.close();
			ps.close();
			return (dias);
		} catch (final Exception e) {
			logger.error("AdicionPuntosManualDAO.diasVigencia: " + e.toString());
			return (365);
		} finally {
			cerrar(cn);
		}
	}

	/** Tope de puntos por operacion, del parametro; 5000 si no esta. */
	public static double topePorOperacion() {
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDGeneral();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT valornumerico FROM parametros WHERE valorparametro = 'TOPEAJUSTEPUNTOS'");
			final ResultSet rs = ps.executeQuery();
			double tope = 5000;
			if (rs.next() && rs.getInt(1) > 0) {
				tope = rs.getInt(1);
			}
			rs.close();
			ps.close();
			return (tope);
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AdicionPuntosManualDAO.topePorOperacion: " + e.toString());
			return (5000);
		} finally {
			cerrar(cn);
		}
	}

	public static Resultado adicionar(final String correo, final double puntos, final String motivo,
			final String usuario) {
		final Logger logger = Logger.getLogger("log_file");
		final Resultado res = new Resultado();
		res.diasVigencia = diasVigencia();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			cn.setAutoCommit(false);

			//La fila del cliente se bloquea: si hay otra operacion sobre el mismo saldo, espera.
			final PreparedStatement psc = cn.prepareStatement(
					"SELECT activo, puntos_vigentes FROM cliente_fidelizacion WHERE correo = ? FOR UPDATE");
			psc.setString(1, correo);
			final ResultSet rsc = psc.executeQuery();
			if (!rsc.next()) {
				rsc.close();
				psc.close();
				cn.rollback();
				res.error = "Ese correo no esta en el plan de fidelizacion.";
				return (res);
			}
			final String activo = rsc.getString(1);
			res.saldoAntes = rsc.getDouble(2);
			rsc.close();
			psc.close();
			if (!"S".equalsIgnoreCase(activo)) {
				cn.rollback();
				res.error = "El cliente esta inactivo en el plan. Hay que activarlo antes de adicionarle puntos.";
				return (res);
			}

			//Llave de la acumulacion: idtienda 0 y el siguiente numero negativo de este cliente.
			int idRef = -1;
			final PreparedStatement psm = cn.prepareStatement(
					"SELECT IFNULL(MIN(idpedidotienda), 0) - 1 FROM fidelizacion_transaccion"
					+ " WHERE correo = ? AND idtienda = 0 AND idpedidotienda < 0");
			psm.setString(1, correo);
			final ResultSet rsm = psm.executeQuery();
			if (rsm.next()) {
				idRef = Math.min(-1, rsm.getInt(1));
			}
			rsm.close();
			psm.close();

			final PreparedStatement psi = cn.prepareStatement(
					"INSERT INTO fidelizacion_transaccion (correo, idtienda, idpedidotienda, valor_neto, puntos,"
					+ " usuario, origen, fecha_vencimiento) VALUES (?, 0, ?, 0, ?, ?, ?, DATE_ADD(CURDATE(), INTERVAL ? DAY))");
			psi.setString(1, correo);
			psi.setInt(2, idRef);
			psi.setDouble(3, puntos);
			psi.setString(4, USUARIO_TRANSACCION);
			psi.setString(5, ORIGEN);
			psi.setInt(6, res.diasVigencia);
			psi.executeUpdate();
			psi.close();

			final PreparedStatement psu = cn.prepareStatement(
					"UPDATE cliente_fidelizacion SET puntos_vigentes = puntos_vigentes + ? WHERE correo = ?");
			psu.setDouble(1, puntos);
			psu.setString(2, correo);
			psu.executeUpdate();
			psu.close();
			res.saldoDespues = res.saldoAntes + puntos;

			final PreparedStatement psf = cn.prepareStatement(
					"SELECT DATE_ADD(CURDATE(), INTERVAL ? DAY)");
			psf.setInt(1, res.diasVigencia);
			final ResultSet rsf = psf.executeQuery();
			if (rsf.next()) {
				res.fechaVencimiento = rsf.getString(1);
			}
			rsf.close();
			psf.close();

			final PreparedStatement psa = cn.prepareStatement(
					"INSERT INTO fidelizacion_ajuste_manual (correo, puntos, motivo, usuario, fecha_vencimiento,"
					+ " idpedidotienda_ref, saldo_antes, saldo_despues) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					java.sql.Statement.RETURN_GENERATED_KEYS);
			psa.setString(1, correo);
			psa.setDouble(2, puntos);
			psa.setString(3, motivo);
			psa.setString(4, usuario);
			psa.setString(5, res.fechaVencimiento);
			psa.setInt(6, idRef);
			psa.setDouble(7, res.saldoAntes);
			psa.setDouble(8, res.saldoDespues);
			psa.executeUpdate();
			final ResultSet rsk = psa.getGeneratedKeys();
			if (rsk.next()) {
				res.idAjuste = rsk.getInt(1);
			}
			rsk.close();
			psa.close();

			cn.commit();
			res.exitoso = true;
			logger.info("AdicionPuntosManualDAO: " + usuario + " adiciono " + puntos + " puntos a " + correo
					+ " (ajuste " + res.idAjuste + ", vencen " + res.fechaVencimiento + ")");
		} catch (final Exception e) {
			logger.error("AdicionPuntosManualDAO.adicionar: " + e.toString());
			res.exitoso = false;
			res.error = "Error tecnico guardando el ajuste; no se hizo ningun cambio.";
			try {
				if (cn != null) {
					cn.rollback();
				}
			} catch (final Exception e2) {
				logger.error("AdicionPuntosManualDAO rollback: " + e2.toString());
			}
		} finally {
			cerrar(cn);
		}
		return (res);
	}

	/** Deja dicho si el correo al cliente salio. */
	public static void marcarCorreo(final int idAjuste, final boolean enviado, final String detalle) {
		if (idAjuste <= 0) {
			return;
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE fidelizacion_ajuste_manual SET correo_enviado = ?, detalle_correo = ? WHERE idajuste = ?");
			ps.setString(1, enviado ? "S" : "N");
			ps.setString(2, detalle == null ? null : (detalle.length() > 200 ? detalle.substring(0, 200) : detalle));
			ps.setInt(3, idAjuste);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("AdicionPuntosManualDAO.marcarCorreo: " + e.toString());
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
			Logger.getLogger("log_file").error("AdicionPuntosManualDAO: cerrando conexion " + e.toString());
		}
	}
}
