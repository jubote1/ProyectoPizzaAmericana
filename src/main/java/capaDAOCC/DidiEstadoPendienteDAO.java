package capaDAOCC;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Los cambios de estado de DiDi que hay que llevarle a una tienda, con su
 * resultado.
 *
 * DiDi avisa que el domiciliario recogio (delivery_status 140) o que entrego
 * (160), y el central traduce eso en una llamada HTTP al servicio de la tienda.
 * Antes esa llamada no dejaba rastro: si la tienda no contestaba, el pedido se
 * quedaba sin subir de estado y no habia donde verlo. Ahora cada intento queda
 * escrito y el que falle lo recoge el reintento.
 *
 * Ver sql/didi_estado_pendiente.sql.
 */
public class DidiEstadoPendienteDAO {

	private static final Logger LOG = Logger.getLogger("log_file");

	/** Un cambio de estado pendiente de llevarle a su tienda. */
	public static class Pendiente {
		public long idPendiente;
		public BigInteger idOrdenComercio;
		public int deliveryStatus;
		public int idTienda;
		public int numPosHeader;
		public int intentos;
	}

	/**
	 * Anota que hay que llevarle este cambio a la tienda y devuelve su id.
	 *
	 * Si DiDi reenvia el mismo evento, cae sobre la fila que ya existe en vez de
	 * crear otra: la llave es (orden, estado). El ON DUPLICATE no toca
	 * `entregado` a proposito, para que un reenvio de algo ya entregado no lo
	 * vuelva a poner pendiente y la tienda reciba dos veces el mismo cambio.
	 *
	 * Devuelve 0 si no se pudo anotar, y en ese caso quien llama debe intentar
	 * la entrega de todas formas: perder el rastro es malo, no entregar es peor.
	 */
	public static long registrar(final BigInteger idOrdenComercio, final int deliveryStatus,
			final int idTienda, final int numPosHeader) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDPrincipal();
		if (cn == null) {
			LOG.error("DidiEstadoPendienteDAO.registrar: sin conexion");
			return 0;
		}
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"INSERT INTO didi_estado_pendiente"
					+ " (idordencomercio, delivery_status, idtienda, numposheader)"
					+ " VALUES (?, ?, ?, ?)"
					+ " ON DUPLICATE KEY UPDATE idtienda = ?, numposheader = ?");
			ps.setString(1, idOrdenComercio.toString());
			ps.setInt(2, deliveryStatus);
			ps.setInt(3, idTienda);
			ps.setInt(4, numPosHeader);
			ps.setInt(5, idTienda);
			ps.setInt(6, numPosHeader);
			ps.executeUpdate();
			ps.close();
			return idDe(cn, idOrdenComercio, deliveryStatus);
		} catch (final Exception e) {
			LOG.error("DidiEstadoPendienteDAO.registrar " + idOrdenComercio + "/" + deliveryStatus
					+ ": " + e.toString());
			return 0;
		} finally {
			cerrar(cn);
		}
	}

	private static long idDe(final Connection cn, final BigInteger idOrdenComercio,
			final int deliveryStatus) throws Exception {
		final PreparedStatement ps = cn.prepareStatement(
				"SELECT idpendiente FROM didi_estado_pendiente"
				+ " WHERE idordencomercio = ? AND delivery_status = ?");
		ps.setString(1, idOrdenComercio.toString());
		ps.setInt(2, deliveryStatus);
		final ResultSet rs = ps.executeQuery();
		long id = 0;
		if (rs.next()) {
			id = rs.getLong(1);
		}
		rs.close();
		ps.close();
		return id;
	}

	/** La tienda lo recibio. */
	public static void marcarEntregado(final long idPendiente) {
		ejecutar(idPendiente,
				"UPDATE didi_estado_pendiente"
				+ " SET entregado = 1, intentos = intentos + 1, ultimo_error = NULL,"
				+ "     fecha_intento = NOW(), fecha_entrega = NOW()"
				+ " WHERE idpendiente = ?");
	}

	/**
	 * No se pudo. Se guarda el motivo recortado: lo que importa es saber que
	 * fallo y por que, no tener la traza entera.
	 */
	public static void marcarFallo(final long idPendiente, final String motivo) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDPrincipal();
		if (cn == null) {
			return;
		}
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE didi_estado_pendiente"
					+ " SET intentos = intentos + 1, ultimo_error = ?, fecha_intento = NOW()"
					+ " WHERE idpendiente = ?");
			ps.setString(1, motivo == null ? "sin detalle"
					: motivo.substring(0, Math.min(motivo.length(), 300)));
			ps.setLong(2, idPendiente);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			LOG.error("DidiEstadoPendienteDAO.marcarFallo " + idPendiente + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	private static void ejecutar(final long idPendiente, final String sql) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDPrincipal();
		if (cn == null) {
			return;
		}
		try {
			final PreparedStatement ps = cn.prepareStatement(sql);
			ps.setLong(1, idPendiente);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			LOG.error("DidiEstadoPendienteDAO: " + e.toString());
		} finally {
			cerrar(cn);
		}
	}

	/**
	 * Los que siguen sin entregar y todavia vale la pena reintentar.
	 *
	 * Se deja de insistir despues de maxIntentos: una tienda que lleva un dia
	 * apagada no se arregla reintentando, y la fila queda con su ultimo_error
	 * para que alguien la vea. Tampoco se miran los viejos: subirle el estado a
	 * un pedido de hace tres dias no le sirve a nadie y solo confunde.
	 */
	public static ArrayList<Pendiente> pendientes(final int maxIntentos, final int diasAtras,
			final int limite) {
		final ArrayList<Pendiente> lista = new ArrayList<Pendiente>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDPrincipal();
		if (cn == null) {
			return lista;
		}
		try {
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idpendiente, idordencomercio, delivery_status, idtienda, numposheader, intentos"
					+ "  FROM didi_estado_pendiente"
					+ " WHERE entregado = 0 AND intentos < ?"
					+ "   AND fecha_creacion >= DATE_SUB(NOW(), INTERVAL ? DAY)"
					+ " ORDER BY fecha_creacion LIMIT ?");
			ps.setInt(1, maxIntentos);
			ps.setInt(2, diasAtras);
			ps.setInt(3, limite);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Pendiente p = new Pendiente();
				p.idPendiente = rs.getLong("idpendiente");
				p.idOrdenComercio = new BigInteger(rs.getString("idordencomercio"));
				p.deliveryStatus = rs.getInt("delivery_status");
				p.idTienda = rs.getInt("idtienda");
				p.numPosHeader = rs.getInt("numposheader");
				p.intentos = rs.getInt("intentos");
				lista.add(p);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			LOG.error("DidiEstadoPendienteDAO.pendientes: " + e.toString());
		} finally {
			cerrar(cn);
		}
		return lista;
	}

	private static void cerrar(final Connection cn) {
		try {
			if (cn != null) {
				cn.close();
			}
		} catch (final Exception e) {
			LOG.error("DidiEstadoPendienteDAO.cerrar: " + e.toString());
		}
	}
}
