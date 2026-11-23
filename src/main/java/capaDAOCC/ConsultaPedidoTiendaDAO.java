package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaModeloCC.Tienda;
import conexionCC.ConexionBaseDatos;

/**
 * DAO especializado para consultar en tiempo real el contenido detallado
 * de lo que pidió un cliente utilizando el número de pedido en la tienda.
 * 
 * Implementa estrategia dual de alta disponibilidad:
 * 1. Consulta en vivo a la base MySQL de la tienda (tiendaamericana).
 * 2. Si la tienda está apagada o inalcanzable, consulta de respaldo en la base
 *    central (pizzaamericana) cruzando por idtienda y numposheader.
 */
public class ConsultaPedidoTiendaDAO {

	private static final Logger logger = Logger.getLogger("log_file");

	@SuppressWarnings("unchecked")
	public static JSONObject consultarDetallePedidoTienda(int idTienda, int idPedidoTienda) {
		JSONObject res = new JSONObject();

		if (idTienda <= 0 || idPedidoTienda <= 0) {
			res.put("exito", false);
			res.put("mensaje", "La tienda y el número de pedido deben ser válidos.");
			return res;
		}

		Tienda tienda = TiendaDAO.obtenerTienda(idTienda);
		if (tienda == null || tienda.getNombreTienda() == null || tienda.getNombreTienda().trim().isEmpty()) {
			res.put("exito", false);
			res.put("mensaje", "La tienda seleccionada no existe en el sistema.");
			return res;
		}

		String nombreTienda = tienda.getNombreTienda();
		String hosbd = tienda.getHosbd();
		ConexionBaseDatos con = new ConexionBaseDatos();

		// =========================================================================
		// 1. INTENTO EN VIVO: Base de datos de la tienda local (tiendaamericana)
		// =========================================================================
		if (hosbd != null && !hosbd.trim().isEmpty()) {
			Connection conTienda = null;
			try {
				conTienda = con.obtenerConexionBDTiendaRemota(hosbd);
				if (conTienda != null && !conTienda.isClosed()) {
					boolean encontradoEnTienda = procesarConsultaTiendaLocal(conTienda, idPedidoTienda, nombreTienda, res);
					if (encontradoEnTienda) {
						return res;
					}
				}
			} catch (Exception e) {
				logger.warn("No fue posible consultar directamente a la tienda " + nombreTienda + " (" + hosbd + "): " + e.getMessage());
			} finally {
				if (conTienda != null) {
					try {
						conTienda.close();
					} catch (Exception ignored) {}
				}
			}
		}

		// =========================================================================
		// 2. RESPALDO CENTRAL: Base de datos pizzaamericana (numposheader / idpedido)
		// =========================================================================
		Connection conCentral = null;
		try {
			conCentral = con.obtenerConexionBDPrincipal();
			if (conCentral != null && !conCentral.isClosed()) {
				boolean encontradoEnCentral = procesarConsultaCentralFallback(conCentral, idTienda, idPedidoTienda, nombreTienda, res);
				if (encontradoEnCentral) {
					return res;
				}
			}
		} catch (Exception e) {
			logger.error("Error consultando respaldo central de pedido #" + idPedidoTienda + ": " + e.getMessage(), e);
		} finally {
			if (conCentral != null) {
				try {
					conCentral.close();
				} catch (Exception ignored) {}
			}
		}

		res.put("exito", false);
		res.put("mensaje", "No se encontró el pedido #" + idPedidoTienda + " en la tienda " + nombreTienda + ".");
		return res;
	}

	@SuppressWarnings("unchecked")
	private static boolean procesarConsultaTiendaLocal(Connection conTienda, int idPedidoTienda, String nombreTienda, JSONObject res) {
		String sqlPed = "SELECT p.*, c.nombre, c.apellido, c.telefono_celular, c.telefono, c.direccion, c.zona, "
				+ "tp.descripcion AS tipo_pedido_desc "
				+ "FROM pedido p "
				+ "LEFT JOIN cliente c ON p.idcliente = c.idcliente "
				+ "LEFT JOIN tipo_pedido tp ON p.idtipopedido = tp.idtipopedido "
				+ "WHERE p.idpedidotienda = ?";

		try (PreparedStatement ps = conTienda.prepareStatement(sqlPed)) {
			ps.setInt(1, idPedidoTienda);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return false;
				}

				res.put("exito", true);
				res.put("fuente", "TIENDA_LOCAL");
				res.put("tienda", nombreTienda);
				res.put("idpedidotienda", idPedidoTienda);
				res.put("fechapedido", rs.getString("fechapedido"));
				res.put("fechainsercion", rs.getString("fechainsercion"));
				res.put("total_neto", rs.getDouble("total_neto"));
				res.put("total_bruto", rs.getDouble("total_bruto"));
				res.put("impuesto", rs.getDouble("impuesto"));
				res.put("tiempopedido", rs.getInt("tiempopedido"));
				res.put("usuariopedido", rs.getString("usuariopedido") != null ? rs.getString("usuariopedido") : "");
				res.put("idtipopedido", rs.getInt("idtipopedido"));
				res.put("tipopedido", rs.getString("tipo_pedido_desc") != null ? rs.getString("tipo_pedido_desc") : "Domicilio");
				res.put("observacion", rs.getString("observacion") != null ? rs.getString("observacion") : "");
				res.put("obs_domiciliario", rs.getString("obs_domiciliario") != null ? rs.getString("obs_domiciliario") : "");

				JSONObject cli = new JSONObject();
				String nom = (rs.getString("nombre") != null ? rs.getString("nombre").trim() : "") + " "
						+ (rs.getString("apellido") != null ? rs.getString("apellido").trim() : "");
				cli.put("nombre", nom.trim().isEmpty() ? "Cliente Tienda" : nom.trim());
				String tel = rs.getString("telefono_celular");
				if (tel == null || tel.trim().isEmpty()) tel = rs.getString("telefono");
				cli.put("telefono", tel != null ? tel.trim() : "");
				cli.put("direccion", rs.getString("direccion") != null ? rs.getString("direccion").trim() : "");
				cli.put("zona", rs.getString("zona") != null ? rs.getString("zona").trim() : "");
				res.put("cliente", cli);
			}
		} catch (Exception e) {
			logger.warn("Error leyendo cabecera en tienda local: " + e.getMessage());
			return false;
		}

		// Formas de pago
		JSONArray pagos = new JSONArray();
		String sqlPagos = "SELECT fp.nombre, pfp.valorformapago "
				+ "FROM pedido_forma_pago pfp "
				+ "LEFT JOIN forma_pago fp ON pfp.idforma_pago = fp.idforma_pago "
				+ "WHERE pfp.idpedidotienda = ?";
		try (PreparedStatement ps = conTienda.prepareStatement(sqlPagos)) {
			ps.setInt(1, idPedidoTienda);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					JSONObject p = new JSONObject();
					p.put("forma_pago", rs.getString("nombre") != null ? rs.getString("nombre") : "Efectivo");
					p.put("valor", rs.getDouble("valorformapago"));
					pagos.add(p);
				}
			}
		} catch (Exception e) {
			logger.warn("Error leyendo formas de pago en tienda: " + e.getMessage());
		}
		res.put("formas_pago", pagos);

		// Productos y Renglones del Pedido
		JSONArray items = new JSONArray();
		String sqlItems = "SELECT a.iddetalle_pedido, a.idproducto, b.descripcion, b.tamano, b.tipo_producto, "
				+ "a.cantidad, a.valorunitario, a.valortotal, a.observacion, a.iddetalle_pedido_master, a.iddetalle_modificador "
				+ "FROM detalle_pedido a "
				+ "JOIN producto b ON a.idproducto = b.idproducto "
				+ "WHERE a.idpedidotienda = ? AND a.idmotivoanulacion IS NULL "
				+ "ORDER BY IF(a.iddetalle_pedido_master = 0, a.iddetalle_pedido, a.iddetalle_pedido_master), a.iddetalle_pedido ASC";
		try (PreparedStatement ps = conTienda.prepareStatement(sqlItems)) {
			ps.setInt(1, idPedidoTienda);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					JSONObject it = new JSONObject();
					it.put("iddetalle", rs.getInt("iddetalle_pedido"));
					it.put("idproducto", rs.getInt("idproducto"));
					it.put("descripcion", rs.getString("descripcion"));
					it.put("tamano", rs.getString("tamano") != null ? rs.getString("tamano") : "");
					it.put("tipo_producto", rs.getString("tipo_producto") != null ? rs.getString("tipo_producto") : "");
					it.put("cantidad", rs.getDouble("cantidad"));
					it.put("valorunitario", rs.getDouble("valorunitario"));
					it.put("valortotal", rs.getDouble("valortotal"));
					it.put("observacion", rs.getString("observacion") != null ? rs.getString("observacion") : "");
					it.put("idmaster", rs.getInt("iddetalle_pedido_master"));
					it.put("idmodificador", rs.getInt("iddetalle_modificador"));
					items.add(it);
				}
			}
		} catch (Exception e) {
			logger.warn("Error leyendo renglones de pedido en tienda: " + e.getMessage());
		}
		res.put("productos", items);

		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean procesarConsultaCentralFallback(Connection conCentral, int idTienda, int idPedidoTienda, String nombreTienda, JSONObject res) {
		String sqlPed = "SELECT p.*, c.nombres, c.apellidos, c.telefono, c.direccion, c.municipio, c.zona "
				+ "FROM pizzaamericana.pedido p "
				+ "LEFT JOIN pizzaamericana.cliente c ON p.idcliente = c.idcliente "
				+ "WHERE p.idtienda = ? AND (p.numposheader = ? OR p.idpedido = ?) "
				+ "ORDER BY p.idpedido DESC LIMIT 1";

		int idPedidoCentral = 0;
		try (PreparedStatement ps = conCentral.prepareStatement(sqlPed)) {
			ps.setInt(1, idTienda);
			ps.setInt(2, idPedidoTienda);
			ps.setInt(3, idPedidoTienda);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return false;
				}

				idPedidoCentral = rs.getInt("idpedido");
				res.put("exito", true);
				res.put("fuente", "CENTRAL_FALLBACK");
				res.put("tienda", nombreTienda);
				res.put("idpedidotienda", rs.getInt("numposheader") > 0 ? rs.getInt("numposheader") : idPedidoTienda);
				res.put("idpedidocentral", idPedidoCentral);
				res.put("fechapedido", rs.getString("fechapedido"));
				res.put("fechainsercion", rs.getString("fechainsercion"));
				res.put("total_neto", rs.getDouble("total_neto"));
				res.put("total_bruto", rs.getDouble("total_bruto"));
				res.put("impuesto", rs.getDouble("impuesto"));
				res.put("tiempopedido", (int) rs.getDouble("tiempopedido"));
				res.put("usuariopedido", rs.getString("usuariopedido") != null ? rs.getString("usuariopedido") : "");
				res.put("idtipopedido", rs.getInt("idtipopedido"));
				res.put("tipopedido", rs.getInt("idtipopedido") == 1 ? "Domicilio" : "Punto de Venta");
				res.put("observacion", "");

				JSONObject cli = new JSONObject();
				String nom = (rs.getString("nombres") != null ? rs.getString("nombres").trim() : "") + " "
						+ (rs.getString("apellidos") != null ? rs.getString("apellidos").trim() : "");
				cli.put("nombre", nom.trim().isEmpty() ? "Cliente Central" : nom.trim());
				cli.put("telefono", rs.getString("telefono") != null ? rs.getString("telefono").trim() : "");
				cli.put("direccion", rs.getString("direccion") != null ? rs.getString("direccion").trim() : "");
				cli.put("zona", rs.getString("zona") != null ? rs.getString("zona").trim() : "");
				res.put("cliente", cli);
			}
		} catch (Exception e) {
			logger.warn("Error leyendo cabecera central fallback: " + e.getMessage());
			return false;
		}

		// Formas de pago central
		JSONArray pagos = new JSONArray();
		String sqlPagos = "SELECT fp.nombre, pfp.valor "
				+ "FROM pizzaamericana.pedido_forma_pago pfp "
				+ "LEFT JOIN pizzaamericana.forma_pago fp ON pfp.idforma_pago = fp.idforma_pago "
				+ "WHERE pfp.idpedido = ?";
		try (PreparedStatement ps = conCentral.prepareStatement(sqlPagos)) {
			ps.setInt(1, idPedidoCentral);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					JSONObject p = new JSONObject();
					p.put("forma_pago", rs.getString("nombre") != null ? rs.getString("nombre") : "Efectivo");
					p.put("valor", rs.getDouble("valor"));
					pagos.add(p);
				}
			}
		} catch (Exception ignored) {}
		res.put("formas_pago", pagos);

		// Productos y Renglones del Pedido Central
		JSONArray items = new JSONArray();
		String sqlItems = "SELECT a.iddetalle_pedido, a.idproducto, b.nombre AS descripcion, "
				+ "a.cantidad, a.valorUnitario, a.valorTotal, a.observacion, "
				+ "CONCAT(IFNULL(c.nombre,''), ' ', IFNULL(a.modespecialidad1,'')) AS esp1, "
				+ "CONCAT(IFNULL(d.nombre,''), ' ', IFNULL(a.modespecialidad2,'')) AS esp2 "
				+ "FROM pizzaamericana.detalle_pedido a "
				+ "JOIN pizzaamericana.producto b ON a.idproducto = b.idproducto "
				+ "LEFT JOIN pizzaamericana.especialidad c ON a.idespecialidad1 = c.idespecialidad "
				+ "LEFT JOIN pizzaamericana.especialidad d ON a.idespecialidad2 = d.idespecialidad "
				+ "WHERE a.idpedido = ? "
				+ "ORDER BY a.iddetalle_pedido ASC";
		try (PreparedStatement ps = conCentral.prepareStatement(sqlItems)) {
			ps.setInt(1, idPedidoCentral);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					JSONObject it = new JSONObject();
					it.put("iddetalle", rs.getInt("iddetalle_pedido"));
					it.put("idproducto", rs.getInt("idproducto"));
					it.put("descripcion", rs.getString("descripcion"));
					it.put("tamano", "");
					it.put("tipo_producto", "P");
					it.put("cantidad", rs.getDouble("cantidad"));
					it.put("valorunitario", rs.getDouble("valorUnitario"));
					it.put("valortotal", rs.getDouble("valorTotal"));

					StringBuilder obs = new StringBuilder();
					String esp1 = rs.getString("esp1") != null ? rs.getString("esp1").trim() : "";
					String esp2 = rs.getString("esp2") != null ? rs.getString("esp2").trim() : "";
					if (!esp1.isEmpty() && !esp2.isEmpty()) {
						obs.append("1/2 ").append(esp1).append(" - 1/2 ").append(esp2);
					} else if (!esp1.isEmpty()) {
						obs.append(esp1);
					}
					String o = rs.getString("observacion") != null ? rs.getString("observacion").trim() : "";
					if (!o.isEmpty()) {
						if (obs.length() > 0) obs.append(" | ");
						obs.append(o);
					}
					it.put("observacion", obs.toString());
					it.put("idmaster", 0);
					it.put("idmodificador", 0);
					items.add(it);
				}
			}
		} catch (Exception e) {
			logger.warn("Error leyendo renglones central fallback: " + e.getMessage());
		}
		res.put("productos", items);

		return true;
	}
}
