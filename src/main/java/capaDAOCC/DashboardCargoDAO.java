package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import capaModeloCC.AsignacionCargoCancelada;
import capaModeloCC.PedidoCargo;
import conexionCC.ConexionBaseDatos;

/**
 * Datos para el Dashboard Cargo: los pedidos que efectivamente llevo Rappi
 * Cargo (pedido.domicilio_tercerizado = 'S') con sus tiempos, y los pedidos
 * que se alcanzaron a asignar a Cargo pero cuya asignacion se cancelo o
 * reverti (tienen fila en tercerizado_domicilio_evento, pero hoy
 * domicilio_tercerizado ya no es 'S').
 *
 * El tiempo total del pedido se mide de fechainsercion a fecha_entregado: no
 * hay una columna confiable de "hora de recogida" (fecha_domiciliario se
 * descarto para esto por indicacion del usuario), asi que todo se basa en la
 * entrega real.
 */
public class DashboardCargoDAO {

	public static ArrayList<PedidoCargo> consultarPedidosCargo(final int idTienda, final String fechaDesde,
			final String fechaHasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<PedidoCargo> pedidos = new ArrayList<PedidoCargo>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT p.idpedido, p.idtienda, IFNULL(t.nombre,'') AS nombretienda, p.numposheader,")
					.append(" p.fechapedido, p.fechainsercion, p.fecha_entregado,")
					.append(" p.fecha_cancelacion, p.entregado, p.cancelado")
					.append(" FROM pedido p LEFT JOIN tienda t ON t.idtienda = p.idtienda")
					.append(" WHERE p.domicilio_tercerizado = 'S'");
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				sql.append(" AND p.fechapedido >= ?");
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				sql.append(" AND p.fechapedido <= ?");
			}
			if (idTienda > 0) {
				sql.append(" AND p.idtienda = ?");
			}
			sql.append(" ORDER BY p.fechapedido, p.idpedido");

			final PreparedStatement ps = con1.prepareStatement(sql.toString());
			int i = 1;
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				ps.setString(i++, fechaDesde);
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				ps.setString(i++, fechaHasta);
			}
			if (idTienda > 0) {
				ps.setInt(i++, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final PedidoCargo p = new PedidoCargo();
				p.setIdPedido(rs.getInt("idpedido"));
				p.setIdTienda(rs.getInt("idtienda"));
				p.setNombreTienda(rs.getString("nombretienda"));
				p.setNumPosHeader(rs.getInt("numposheader"));
				p.setFechaPedido(rs.getString("fechapedido"));
				final Timestamp insercion = rs.getTimestamp("fechainsercion");
				final Timestamp entregado = rs.getTimestamp("fecha_entregado");
				final Timestamp cancelacion = rs.getTimestamp("fecha_cancelacion");
				p.setFechaInsercion(insercion == null ? null : insercion.toString());
				p.setFechaEntregado(entregado == null ? null : entregado.toString());
				p.setFechaCancelacion(cancelacion == null ? null : cancelacion.toString());
				p.setEntregado(rs.getBoolean("entregado"));
				p.setCancelado(rs.getBoolean("cancelado"));
				if (insercion != null && entregado != null) {
					p.setMinutosTotal(Double.valueOf((entregado.getTime() - insercion.getTime()) / 60000.0));
				}
				pedidos.add(p);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("DashboardCargoDAO.consultarPedidosCargo: " + e.toString());
			try {
				con1.close();
			} catch (final Exception e1) {
			}
		}
		return pedidos;
	}

	/**
	 * Pedidos con evento de asignacion en tercerizado_domicilio_evento cuya
	 * asignacion no se quedo (domicilio_tercerizado &lt;&gt; 'S' hoy). Se toma el
	 * ultimo evento de cada pedido para saber en que estado quedo Cargo.
	 */
	public static ArrayList<AsignacionCargoCancelada> consultarAsignacionesCanceladas(final int idTienda,
			final String fechaDesde, final String fechaHasta) {
		final Logger logger = Logger.getLogger("log_file");
		final ArrayList<AsignacionCargoCancelada> resultado = new ArrayList<AsignacionCargoCancelada>();
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection con1 = con.obtenerConexionBDPrincipal();
		try {
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT p.idpedido, p.idtienda, IFNULL(t.nombre,'') AS nombretienda, p.numposheader,")
					.append(" p.fechapedido, ev.proveedor, ev.estado AS ultimo_estado, ev.fecha_recepcion,")
					.append(" ev.fecha_cancelacion")
					.append(" FROM pedido p")
					.append(" JOIN (SELECT id_pedido, MAX(id_evento) AS max_evento FROM tercerizado_domicilio_evento GROUP BY id_pedido) u")
					.append(" ON u.id_pedido = p.idpedido")
					.append(" JOIN tercerizado_domicilio_evento ev ON ev.id_evento = u.max_evento")
					.append(" LEFT JOIN tienda t ON t.idtienda = p.idtienda")
					.append(" WHERE IFNULL(p.domicilio_tercerizado,'N') <> 'S'");
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				sql.append(" AND p.fechapedido >= ?");
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				sql.append(" AND p.fechapedido <= ?");
			}
			if (idTienda > 0) {
				sql.append(" AND p.idtienda = ?");
			}
			sql.append(" ORDER BY p.fechapedido, p.idpedido");

			final PreparedStatement ps = con1.prepareStatement(sql.toString());
			int i = 1;
			if (fechaDesde != null && fechaDesde.trim().length() > 0) {
				ps.setString(i++, fechaDesde);
			}
			if (fechaHasta != null && fechaHasta.trim().length() > 0) {
				ps.setString(i++, fechaHasta);
			}
			if (idTienda > 0) {
				ps.setInt(i++, idTienda);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final AsignacionCargoCancelada a = new AsignacionCargoCancelada();
				a.setIdPedido(rs.getInt("idpedido"));
				a.setIdTienda(rs.getInt("idtienda"));
				a.setNombreTienda(rs.getString("nombretienda"));
				a.setNumPosHeader(rs.getInt("numposheader"));
				a.setFechaPedido(rs.getString("fechapedido"));
				a.setProveedor(rs.getString("proveedor"));
				a.setUltimoEstadoCargo(rs.getString("ultimo_estado"));
				final Timestamp asignacion = rs.getTimestamp("fecha_recepcion");
				final Timestamp cancelacionCargo = rs.getTimestamp("fecha_cancelacion");
				a.setFechaAsignacion(asignacion == null ? null : asignacion.toString());
				a.setFechaCancelacionCargo(cancelacionCargo == null ? null : cancelacionCargo.toString());
				resultado.add(a);
			}
			rs.close();
			ps.close();
			con1.close();
		} catch (final Exception e) {
			logger.error("DashboardCargoDAO.consultarAsignacionesCanceladas: " + e.toString());
			try {
				con1.close();
			} catch (final Exception e1) {
			}
		}
		return resultado;
	}

}
