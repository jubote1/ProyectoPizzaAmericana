package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import conexionCC.ConexionBaseDatos;

/**
 * Lo que necesita el correo de confirmacion de un pedido hecho por el bot: la tienda, la forma de pago, si es
 * programado, cada producto con su valor y el total. Todo leido del pedido ya guardado en el central, para que el
 * correo diga lo mismo que quedo registrado y no lo que el bot creyo mandar.
 */
public class ConfirmacionBotDAO {

	private static final Logger logger = Logger.getLogger("log_file");

	/** Una linea del pedido. */
	public static class Linea {
		public String producto = "";
		public double cantidad;
		/** Especialidades elegidas ("" si el producto no las tiene). */
		public String especialidades = "";
		public String adicion = "";
		public double valorTotal;
	}

	public static class Resumen {
		public int idPedido;
		public String tienda = "";
		public String formaPago = "";
		public boolean programado;
		public String horaProgramado = "";
		public double total;
		public ArrayList<Linea> lineas = new ArrayList<Linea>();
	}

	/** @return el resumen, o null si el pedido no existe o no se pudo consultar */
	public static Resumen obtener(final int idPedido) {
		final ConexionBaseDatos con = new ConexionBaseDatos();
		final Connection cn = con.obtenerConexionBDPrincipal();
		if (cn == null) {
			return null;
		}
		try {
			final Resumen r = new Resumen();
			r.idPedido = idPedido;
			try (PreparedStatement ps = cn.prepareStatement(
					"SELECT t.nombre tienda, p.total_neto, p.programado, p.hora_programado, "
							+ "(SELECT GROUP_CONCAT(f.nombre SEPARATOR ' + ') FROM pedido_forma_pago pf, forma_pago f "
							+ " WHERE pf.idpedido = p.idpedido AND f.idforma_pago = pf.idforma_pago) forma_pago "
							+ "FROM pedido p, tienda t WHERE p.idpedido = ? AND t.idtienda = p.idtienda")) {
				ps.setInt(1, idPedido);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						cn.close();
						return null;
					}
					r.tienda = rs.getString("tienda") == null ? "" : rs.getString("tienda");
					r.total = rs.getDouble("total_neto");
					r.programado = "S".equals(rs.getString("programado"));
					r.horaProgramado = rs.getString("hora_programado") == null ? "" : rs.getString("hora_programado");
					r.formaPago = rs.getString("forma_pago") == null ? "" : rs.getString("forma_pago");
				}
			}
			try (PreparedStatement ps = cn.prepareStatement(
					"SELECT pr.nombre producto, d.cantidad, d.valorTotal, d.adicion, e1.nombre esp1, e2.nombre esp2 "
							+ "FROM detalle_pedido d INNER JOIN producto pr ON pr.idproducto = d.idproducto "
							+ "LEFT JOIN especialidad e1 ON e1.idespecialidad = d.idespecialidad1 "
							+ "LEFT JOIN especialidad e2 ON e2.idespecialidad = d.idespecialidad2 "
							+ "WHERE d.idpedido = ? ORDER BY d.iddetalle_pedido")) {
				ps.setInt(1, idPedido);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final Linea l = new Linea();
						l.producto = rs.getString("producto") == null ? "" : rs.getString("producto");
						l.cantidad = rs.getDouble("cantidad");
						l.valorTotal = rs.getDouble("valorTotal");
						l.adicion = rs.getString("adicion") == null ? "" : rs.getString("adicion").trim();
						final String e1 = rs.getString("esp1");
						final String e2 = rs.getString("esp2");
						if (e1 != null && e2 != null && !e1.equals(e2)) {
							l.especialidades = "Mitad " + e1 + " / mitad " + e2;
						} else if (e1 != null) {
							l.especialidades = e1;
						}
						r.lineas.add(l);
					}
				}
			}
			cn.close();
			return r;
		} catch (final Exception e) {
			logger.error("ConfirmacionBotDAO.obtener " + idPedido + ": " + e.toString());
			try {
				cn.close();
			} catch (final Exception e1) {
				// Ya estaba cerrada.
			}
			return null;
		}
	}
}
