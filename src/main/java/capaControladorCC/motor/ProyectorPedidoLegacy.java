package capaControladorCC.motor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Capa de Proyeccion Bidireccional de Pedidos (Etapa 3 - Piloto Manrique).
 *
 * Cuando un pedido se procesa con el motor nuevo:
 *  1. Lo guarda en el esquema pos_central (pos_central.pedido y pos_central.detalle_pedido)
 *     con la estructura jerarquica maestra/hijas nativa del POS.
 *  2. Lo proyecta en las tablas tradicionales (pizzaamericana.detalle_pedido)
 *     para asegurar que el Datamart, la Facturacion Electronica, el CRM de Kommo,
 *     los reportes de ventas y la varianza semanal continuen funcionando sin alteraciones.
 */
public class ProyectorPedidoLegacy {

	/**
	 * Guarda el pedido en pos_central y lo proyecta a pizzaamericana.
	 */
	public static boolean proyectarPedido(Connection con, int idPedidoCentral, int idTienda, int idCliente,
			MotorSolicitud solicitud, MotorResultado resultado, String usuario) {

		if (idPedidoCentral <= 0 || resultado == null || !MotorResultado.OK.equals(resultado.estado)) {
			return false;
		}

		try {
			// 1. Guardar en pos_central.pedido
			String sqlPosPed = "insert into pos_central.pedido (idtienda, total_bruto, impuesto, total_neto, idcliente, "
					+ "fechapedido, idpedidocontact, usuariopedido, tiempopedido, idtipopedido, idestado, programado, hora_programado) "
					+ "values (?,?,?,?,?,current_date(),?,?,?,?,1,?,?)";

			int idPedidoPosGenerado = 0;
			try (PreparedStatement ps = con.prepareStatement(sqlPosPed, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, idTienda);
				ps.setDouble(2, resultado.subtotal);
				ps.setDouble(3, 0.0);
				ps.setDouble(4, resultado.total);
				ps.setInt(5, idCliente);
				ps.setInt(6, idPedidoCentral);
				ps.setString(7, usuario != null && !usuario.trim().isEmpty() ? usuario : "MOTOR");
				ps.setInt(8, 30);
				ps.setInt(9, solicitud.contexto != null ? solicitud.contexto.idtipopedido : 1);
				ps.setString(10, solicitud.contexto != null && solicitud.contexto.programado ? "S" : "N");
				ps.setString(11,
						solicitud.contexto != null && solicitud.contexto.horaProgramado != null
								? solicitud.contexto.horaProgramado
								: "");
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys()) {
					if (rs.next()) {
						idPedidoPosGenerado = rs.getInt(1);
					}
				}
			}

			// 2. Guardar lineas en pos_central.detalle_pedido
			String sqlPosDet = "insert into pos_central.detalle_pedido (idpedidotienda, idtienda, idproducto, cantidad, "
					+ "valorunitario, valortotal, valorimpuesto, iddetalle_pedido_master, iddetalle_modificador) "
					+ "values (?,?,?,?,?,?,0,?,?)";

			Map<Integer, Integer> mapaRefToIdDetalle = new HashMap<Integer, Integer>();
			try (PreparedStatement ps = con.prepareStatement(sqlPosDet, Statement.RETURN_GENERATED_KEYS)) {
				for (MotorResultado.Linea l : resultado.lineas) {
					ps.setInt(1, idPedidoPosGenerado > 0 ? idPedidoPosGenerado : idPedidoCentral);
					ps.setInt(2, idTienda);
					ps.setInt(3, l.idproducto);
					ps.setDouble(4, l.cantidad);
					ps.setDouble(5, l.valorUnitario);
					ps.setDouble(6, l.valorTotal);
					int masterId = (l.refMaster > 0 && mapaRefToIdDetalle.containsKey(l.refMaster))
							? mapaRefToIdDetalle.get(l.refMaster)
							: l.refMaster;
					ps.setInt(7, masterId);
					ps.setInt(8, l.refModificador);
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						if (rs.next()) {
							mapaRefToIdDetalle.put(l.ref, rs.getInt(1));
						}
					}
				}
			}

			// 3. Proyeccion hacia pizzaamericana.detalle_pedido
			// Limpiar lineas viejas si se trata de una re-proyeccion
			try (PreparedStatement ps = con
					.prepareStatement("delete from pizzaamericana.detalle_pedido where idpedido = ?")) {
				ps.setInt(1, idPedidoCentral);
				ps.executeUpdate();
			}

			// Cargar catalogo basico para nombres y equivalencias
			Map<Integer, String> descripciones = new HashMap<Integer, String>();
			try (PreparedStatement ps = con
					.prepareStatement("select idproducto, descripcion from pos_central.producto");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					descripciones.put(rs.getInt(1), rs.getString(2));
				}
			}

			// Cargar mapa equivalencia_producto (pos -> especialidad central)
			Map<Integer, Integer> posToEspCentral = new HashMap<Integer, Integer>();
			try (PreparedStatement ps = con.prepareStatement(
					"select idproducto_pos, idespecialidad_central from pos_central.equivalencia_producto where idespecialidad_central > 0");
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					posToEspCentral.put(rs.getInt(1), rs.getInt(2));
				}
			}

			// Agrupar hijas por refMaster
			Map<Integer, List<MotorResultado.Linea>> hijasPorMaster = new HashMap<Integer, List<MotorResultado.Linea>>();
			for (MotorResultado.Linea l : resultado.lineas) {
				if (l.refMaster > 0) {
					if (!hijasPorMaster.containsKey(l.refMaster)) {
						hijasPorMaster.put(l.refMaster, new ArrayList<MotorResultado.Linea>());
					}
					hijasPorMaster.get(l.refMaster).add(l);
				}
			}

			String sqlLegDet = "insert into pizzaamericana.detalle_pedido (idproducto, idpedido, cantidad, "
					+ "idespecialidad1, idespecialidad2, valorUnitario, valorTotal, adicion, observacion) "
					+ "values (?,?,?,?,?,?,?,?,?)";

			try (PreparedStatement ps = con.prepareStatement(sqlLegDet)) {
				for (MotorResultado.Linea l : resultado.lineas) {
					if (l.refMaster == 0) {
						// Linea maestra (pizza, combo, acompanamiento o bebida)
						int esp1 = 0;
						int esp2 = 0;
						StringBuilder adiciones = new StringBuilder();

						List<MotorResultado.Linea> hijas = hijasPorMaster.get(l.ref);
						if (hijas != null) {
							for (MotorResultado.Linea h : hijas) {
								if (posToEspCentral.containsKey(h.idproducto)) {
									int espId = posToEspCentral.get(h.idproducto);
									if (esp1 == 0) {
										esp1 = espId;
									} else if (esp2 == 0) {
										esp2 = espId;
									}
								} else {
									// Es adicion o modificador
									if (adiciones.length() > 0) {
										adiciones.append(", ");
									}
									String nomH = descripciones.containsKey(h.idproducto)
											? descripciones.get(h.idproducto)
											: "Item " + h.idproducto;
									if (h.refModificador == 1) {
										adiciones.append("CON ").append(nomH);
									} else if (h.refModificador == 2) {
										adiciones.append("SIN ").append(nomH);
									} else {
										adiciones.append(nomH);
									}
								}
							}
						}

						// Calcular valor total de la linea con sus hijas
						double totalConHijas = l.valorTotal;
						if (hijas != null) {
							for (MotorResultado.Linea h : hijas) {
								totalConHijas += h.valorTotal;
							}
						}

						ps.setInt(1, l.idproducto);
						ps.setInt(2, idPedidoCentral);
						ps.setDouble(3, l.cantidad);
						ps.setInt(4, esp1);
						ps.setInt(5, esp2);
						ps.setDouble(6, l.cantidad > 0 ? (totalConHijas / l.cantidad) : totalConHijas);
						ps.setDouble(7, totalConHijas);
						ps.setString(8, adiciones.toString());
						ps.setString(9, l.origen != null ? l.origen : "MOTOR");
						ps.executeUpdate();
					}
				}
			}

			// 4. Actualizar totales en pizzaamericana.pedido
			String sqlUpdPed = "update pizzaamericana.pedido set total_bruto = ?, descuento = ?, total_neto = ? where idpedido = ?";
			try (PreparedStatement ps = con.prepareStatement(sqlUpdPed)) {
				ps.setDouble(1, resultado.subtotal);
				ps.setDouble(2, resultado.descuento);
				ps.setDouble(3, resultado.total);
				ps.setInt(4, idPedidoCentral);
				ps.executeUpdate();
			}

			return true;

		} catch (Throwable t) {
			System.out.println("ERROR en ProyectorPedidoLegacy: " + t.getMessage());
			t.printStackTrace();
			return false;
		}
	}
}
