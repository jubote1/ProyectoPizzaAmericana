package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Lo que hace falta saber de un pedido que quedo a medio tomar.
 *
 * EN ESTOS PEDIDOS total_neto ESTA EN CERO
 *
 * Ese campo lo llena la finalizacion, y a estos nunca se les llego. Verificado
 * contra la base el 2026-10-07: los ocho mas recientes tenian total_neto = 0 y
 * detalles que sumaban entre 5.000 y 142.800 pesos.
 *
 * Por eso el total sale del detalle. Finalizar con el total_neto del pedido
 * mandaria a cocina un pedido de cero pesos.
 */
public class PedidoEnCursoDAO {

	/**
	 * Lo que suman los productos del pedido.
	 *
	 * Devuelve 0 si no tiene lineas, y ese cero es informacion: un pedido sin
	 * productos no se puede terminar, hay que descartarlo.
	 */
	public static double totalDelDetalle(final int idPedido) {
		double total = 0;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(SUM(valortotal),0) AS total"
					+ " FROM detalle_pedido WHERE idpedido = ?");
			ps.setInt(1, idPedido);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				total = rs.getDouble("total");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.totalDelDetalle pedido "
					+ idPedido + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (total);
	}

	/**
	 * El tiempo prometido que ya traia el pedido.
	 *
	 * Se respeta el que tenia en vez de poner cero: ese numero es el que la
	 * tienda usa para el semaforo de cocina, y un cero lo pinta como vencido
	 * desde que entra.
	 */
	public static double tiempoDelPedido(final int idPedido) {
		double tiempo = 0;
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT IFNULL(tiempopedido,0) AS tiempo FROM pedido WHERE idpedido = ?");
			ps.setInt(1, idPedido);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				tiempo = rs.getDouble("tiempo");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.tiempoDelPedido pedido "
					+ idPedido + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (tiempo);
	}

	/**
	 * Deja dicho quien lo termino.
	 *
	 * Va en usuarioreenvio, que es la columna que ya usan las ocho pantallas
	 * para marcar un reenvio manual. Terminar un pedido ajeno es la misma
	 * clase de acto: alguien distinto del que lo tomo lo mando a cocina, y eso
	 * tiene que quedar con nombre.
	 */
	public static void anotarQuienTermino(final int idPedido, final String usuario) {
		if (usuario == null || usuario.trim().length() == 0) {
			return;
		}
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"UPDATE pedido SET usuarioreenvio = ? WHERE idpedido = ?");
			ps.setString(1, usuario.trim());
			ps.setInt(2, idPedido);
			ps.executeUpdate();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.anotarQuienTermino pedido "
					+ idPedido + ": " + e.toString());
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
			Logger.getLogger("log_file").error("PedidoEnCursoDAO: no cerro la conexion, "
					+ e.toString());
		}
	}
}
