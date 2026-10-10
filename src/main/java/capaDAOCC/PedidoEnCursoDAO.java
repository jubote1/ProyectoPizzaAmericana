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

	// ------------------------------------------------------------------
	// Edicion del contenido: lo que hace falta para decidir y para pintar
	// ------------------------------------------------------------------

	/** El encabezado de un pedido, solo lo que decide si se puede editar. */
	public static class Cabecera {
		public boolean existe = false;
		public int idTienda = 0;
		/** 1 = en curso. Solo ese se edita. */
		public int idEstado = 0;
		/** Mayor que cero si la tienda ya lo tiene: no se toca. */
		public int numPosHeader = 0;
		public int idCliente = 0;
	}

	/** Una linea del pedido, con lo necesario para pintarla y para decidir si se puede quitar. */
	public static class Linea {
		public int idDetalle;
		public int idProducto;
		public String producto = "";
		public String tipo = "";
		public double cantidad;
		public double valorUnitario;
		public double valorTotal;
		public String especialidad1 = "";
		public String especialidad2 = "";
		public String adicion = "";
		public String observacion = "";
		public int idSaborTipoLiquido;
		/**
		 * Una adicion, un modificador o un producto incluido: cuelga de otra linea y se quita con ella, no
		 * solo. Quitarla suelta dejaria el texto "adicion" de la linea principal diciendo algo que ya no es.
		 */
		public boolean esHija = false;
	}

	public static Cabecera cabecera(final int idPedido) {
		final Cabecera c = new Cabecera();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT idtienda, IFNULL(idestadopedido,0) estado, IFNULL(numposheader,0) numpos,"
					+ " IFNULL(idcliente,0) idcliente FROM pedido WHERE idpedido = ?");
			ps.setInt(1, idPedido);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				c.existe = true;
				c.idTienda = rs.getInt("idtienda");
				c.idEstado = rs.getInt("estado");
				c.numPosHeader = rs.getInt("numpos");
				c.idCliente = rs.getInt("idcliente");
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.cabecera pedido " + idPedido + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (c);
	}

	public static java.util.ArrayList<Linea> lineas(final int idPedido) {
		final java.util.ArrayList<Linea> lista = new java.util.ArrayList<Linea>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT d.iddetalle_pedido, d.idproducto, p.nombre, IFNULL(p.tipo,'') tipo, d.cantidad,"
					+ " IFNULL(d.valorUnitario,0) vunit, IFNULL(d.valorTotal,0) vtotal,"
					+ " IFNULL(e1.nombre,'') esp1, IFNULL(e2.nombre,'') esp2,"
					+ " IFNULL(d.adicion,'') adicion, IFNULL(d.observacion,'') observacion,"
					+ " IFNULL(d.idsabortipoliquido,0) sabor"
					+ " FROM detalle_pedido d"
					+ " INNER JOIN producto p ON p.idproducto = d.idproducto"
					+ " LEFT JOIN especialidad e1 ON e1.idespecialidad = d.idespecialidad1"
					+ " LEFT JOIN especialidad e2 ON e2.idespecialidad = d.idespecialidad2"
					+ " WHERE d.idpedido = ? ORDER BY d.iddetalle_pedido");
			ps.setInt(1, idPedido);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				final Linea l = new Linea();
				l.idDetalle = rs.getInt("iddetalle_pedido");
				l.idProducto = rs.getInt("idproducto");
				l.producto = rs.getString("nombre");
				l.tipo = rs.getString("tipo");
				l.cantidad = rs.getDouble("cantidad");
				l.valorUnitario = rs.getDouble("vunit");
				l.valorTotal = rs.getDouble("vtotal");
				l.especialidad1 = rs.getString("esp1");
				l.especialidad2 = rs.getString("esp2");
				l.adicion = rs.getString("adicion");
				l.observacion = rs.getString("observacion");
				l.idSaborTipoLiquido = rs.getInt("sabor");
				final String tipo = l.tipo == null ? "" : l.tipo.toUpperCase();
				l.esHija = tipo.equals("ADICION") || tipo.startsWith("MODIFICADOR")
						|| (l.observacion != null && l.observacion.startsWith("Producto Incluido-"));
				lista.add(l);
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.lineas pedido " + idPedido + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (lista);
	}

	/**
	 * Los productos incluidos que cuelgan de una linea: se marcan con la observacion "Producto Incluido-" + el
	 * id de la linea principal. El DAO que borra una linea NO los borra (la pantalla de pedidos los quita uno
	 * por uno), asi que hay que buscarlos aparte.
	 */
	public static java.util.ArrayList<Integer> incluidosDe(final int idPedido, final int idDetalle) {
		final java.util.ArrayList<Integer> ids = new java.util.ArrayList<Integer>();
		Connection cn = null;
		try {
			cn = new ConexionBaseDatos().obtenerConexionBDPrincipal();
			final PreparedStatement ps = cn.prepareStatement(
					"SELECT iddetalle_pedido FROM detalle_pedido WHERE idpedido = ? AND observacion = ?");
			ps.setInt(1, idPedido);
			ps.setString(2, "Producto Incluido-" + idDetalle);
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(Integer.valueOf(rs.getInt(1)));
			}
			rs.close();
			ps.close();
		} catch (final Exception e) {
			Logger.getLogger("log_file").error("PedidoEnCursoDAO.incluidosDe pedido " + idPedido + ": " + e.toString());
		} finally {
			cerrar(cn);
		}
		return (ids);
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
