package capaDAOCC;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import conexionCC.ConexionBaseDatos;

/**
 * DAO para la Pantalla Unificada de Consulta de Pedidos del Nuevo Modelo / Multicanal.
 *
 * Consolida los pedidos de todos los canales (Tienda Virtual Kuno, App, CRM Bot,
 * Rappi, DiDi, Contact Center) y permite auditar:
 *  - Estado en Central y POS.
 *  - Modo de procesamiento (NUEVO MOTOR, PARALELO en sombra, o LEGACY).
 *  - Concordancia y diferencias entre la cotización tradicional y el Nuevo Motor.
 */
public class ConsultaPedidosNuevoMotorDAO {

	private static final Logger logger = Logger.getLogger("log_file");

	/**
	 * Consulta el listado de pedidos con filtros unificados y genera los KPIs consolidados.
	 */
	public static JSONObject consultarPedidosUnificados(
			String fechaInicial,
			String fechaFinal,
			int idTienda,
			String canal,
			String modo,
			int idEstadoPedido,
			String numeroPedido) {

		JSONObject respuesta = new JSONObject();
		JSONArray listado = new JSONArray();

		// KPIs acumuladores
		int totalPedidos = 0;
		double totalVentas = 0.0;
		int cntKuno = 0, cntApp = 0, cntBot = 0, cntRappi = 0, cntDidi = 0, cntContact = 0, cntOtros = 0;
		int cntNuevo = 0, cntParalelo = 0, cntLegacy = 0, cntConDiferencia = 0;

		Connection con = null;
		try {
			con = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			// Normalizar fechas a yyyy-MM-dd si vienen en dd/MM/yyyy
			String fIni = normalizarFecha(fechaInicial);
			String fFin = normalizarFecha(fechaFinal);

			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ")
			   .append("  p.idpedido, p.idordencomercio, p.idtienda, t.nombre AS nombretienda, t.url AS urltienda, ")
			   .append("  p.idcliente, CONCAT(IFNULL(c.nombres,''), ' ', IFNULL(c.apellidos,'')) AS nombrecliente, ")
			   .append("  IFNULL(c.telefono, IFNULL(c.telefono_celular,'')) AS telefono, IFNULL(c.direccion,'') AS direccion, ")
			   .append("  IFNULL(c.municipio,'') AS municipio, p.fechapedido, p.fechainsercion, ")
			   .append("  p.total_bruto, p.descuento, p.total_neto, p.idestadopedido, IFNULL(ep.descripcion,'') AS estadopedido, ")
			   .append("  p.enviadopixel, p.numposheader, p.origen, p.usuariopedido, p.tiempopedido, ")
			   .append("  p.programado, p.hora_programado, IFNULL(fp.nombre, 'NO DEFINIDO') AS formapago, ")
			   .append("  IFNULL(s.idsombra, IFNULL(sv.idsombra, 0)) AS idsombra, ")
			   .append("  IFNULL(s.modo_ejecucion, IF(sv.idsombra IS NOT NULL, 'PARALELO', 'LEGACY')) AS modosombra, ")
			   .append("  IFNULL(s.estado_motor, IFNULL(sv.estado_motor, '')) AS estado_motor, ")
			   .append("  IFNULL(s.total_viejo, IFNULL(sv.subtotal_viejo, 0)) AS total_viejo, ")
			   .append("  IFNULL(s.total_motor, IFNULL(sv.subtotal_motor, 0)) AS total_motor, ")
			   .append("  IFNULL(s.diferencia, IFNULL(sv.diferencia_vs_viejo, 0)) AS diferenciasombra, ")
			   .append("  IFNULL(s.avisos_traduccion, IFNULL(sv.avisos_traduccion, 0)) AS avisos_traduccion, ")
			   .append("  pos_p.idpedidotienda AS idpedidoposcentral ")
			   .append("FROM pizzaamericana.pedido p ")
			   .append("JOIN pizzaamericana.tienda t ON t.idtienda = p.idtienda ")
			   .append("LEFT JOIN pizzaamericana.cliente c ON c.idcliente = p.idcliente ")
			   .append("LEFT JOIN pizzaamericana.estado_pedido ep ON ep.idestadopedido = p.idestadopedido ")
			   .append("LEFT JOIN pizzaamericana.pedido_forma_pago pfp ON pfp.idpedido = p.idpedido ")
			   .append("LEFT JOIN pizzaamericana.forma_pago fp ON fp.idforma_pago = pfp.idforma_pago ")
			   .append("LEFT JOIN pos_central.sombra_pedido_canal s ON s.idpedido_central = p.idpedido ")
			   .append("LEFT JOIN pos_central.sombra_vivo sv ON sv.idpedido = p.idpedido ")
			   .append("LEFT JOIN pos_central.pedido pos_p ON pos_p.idpedidocontact = p.idpedido ")
			   .append("WHERE p.fechapedido >= ? AND p.fechapedido <= ? ");

			List<Object> parametros = new ArrayList<Object>();
			parametros.add(fIni);
			parametros.add(fFin);

			if (idTienda > 0) {
				sql.append(" AND p.idtienda = ? ");
				parametros.add(idTienda);
			}

			if (canal != null && !canal.trim().isEmpty() && !"TODOS".equalsIgnoreCase(canal)) {
				String cNorm = canal.trim().toUpperCase();
				if ("KUNO".equals(cNorm) || "TK".equals(cNorm)) {
					sql.append(" AND p.origen IN ('TK', 'TW') ");
				} else if ("APP".equals(cNorm)) {
					sql.append(" AND p.origen = 'APP' ");
				} else if ("BOT".equals(cNorm) || "CRM".equals(cNorm)) {
					sql.append(" AND p.origen IN ('BOT', 'CRM') ");
				} else if ("RAPPI".equals(cNorm) || "RAP".equals(cNorm)) {
					sql.append(" AND p.origen IN ('RAPPI', 'RAP', 'R') ");
				} else if ("DIDI".equals(cNorm)) {
					sql.append(" AND p.origen IN ('DIDI', 'D') ");
				} else if ("CONTACT".equals(cNorm) || "C".equals(cNorm)) {
					sql.append(" AND p.origen = 'C' ");
				} else {
					sql.append(" AND p.origen = ? ");
					parametros.add(canal);
				}
			}

			if (idEstadoPedido > 0) {
				sql.append(" AND p.idestadopedido = ? ");
				parametros.add(idEstadoPedido);
			}

			if (numeroPedido != null && !numeroPedido.trim().isEmpty()) {
				String num = numeroPedido.trim();
				sql.append(" AND (p.idpedido = ? OR p.idordencomercio LIKE ? OR p.numposheader = ?) ");
				int numInt = 0;
				try { numInt = Integer.parseInt(num); } catch (Exception e) {}
				parametros.add(numInt);
				parametros.add("%" + num + "%");
				parametros.add(numInt);
			}

			sql.append(" GROUP BY p.idpedido ORDER BY p.idpedido DESC LIMIT 1500");

			try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
				for (int i = 0; i < parametros.size(); i++) {
					ps.setObject(i + 1, parametros.get(i));
				}

				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int idPed = rs.getInt("idpedido");
						String orig = rs.getString("origen");
						if (orig == null) orig = "C";
						orig = orig.trim().toUpperCase();

						long idSombra = rs.getLong("idsombra");
						int idPosCentral = rs.getInt("idpedidoposcentral");
						double totalNeto = rs.getDouble("total_neto");

						// Determinar Modo de Procesamiento
						String modoFila = "LEGACY";
						if (idPosCentral > 0 && idSombra == 0) {
							modoFila = "NUEVO";
						} else if (idSombra > 0) {
							modoFila = "PARALELO";
						}

						// Filtro de modo en memoria si se especifico
						if (modo != null && !modo.trim().isEmpty() && !"TODOS".equalsIgnoreCase(modo)) {
							if (!modo.trim().equalsIgnoreCase(modoFila)) {
								continue;
							}
						}

						JSONObject fila = new JSONObject();
						fila.put("idpedido", idPed);
						fila.put("idordencomercio", rs.getString("idordencomercio"));
						fila.put("idtienda", rs.getInt("idtienda"));
						fila.put("tienda", rs.getString("nombretienda"));
						fila.put("urltienda", rs.getString("urltienda"));
						fila.put("idcliente", rs.getInt("idcliente"));
						fila.put("cliente", rs.getString("nombrecliente").trim());
						fila.put("telefono", rs.getString("telefono"));
						fila.put("direccion", rs.getString("direccion"));
						fila.put("municipio", rs.getString("municipio"));
						fila.put("fechapedido", rs.getString("fechapedido"));
						fila.put("fechainsercion", rs.getString("fechainsercion"));
						fila.put("total_bruto", rs.getDouble("total_bruto"));
						fila.put("descuento", rs.getDouble("descuento"));
						fila.put("totalneto", totalNeto);
						fila.put("idestadopedido", rs.getInt("idestadopedido"));
						fila.put("estadopedido", rs.getString("estadopedido"));
						fila.put("enviadopixel", rs.getInt("enviadopixel"));
						fila.put("numposheader", rs.getInt("numposheader"));
						fila.put("origen", orig);
						fila.put("canal_legible", resolverCanalLegible(orig));
						fila.put("usuariopedido", rs.getString("usuariopedido"));
						fila.put("tiempopedido", rs.getDouble("tiempopedido"));
						fila.put("programado", rs.getString("programado"));
						fila.put("hora_programado", rs.getString("hora_programado"));
						fila.put("formapago", rs.getString("formapago"));
						fila.put("modo_procesamiento", modoFila);

						// Datos comparativos de Sombra / Paralelo si existen
						fila.put("idsombra", idSombra);
						if (idSombra > 0) {
							fila.put("estado_motor", rs.getString("estado_motor"));
							fila.put("total_viejo", rs.getDouble("total_viejo"));
							fila.put("total_motor", rs.getDouble("total_motor"));
							double dif = rs.getDouble("diferenciasombra");
							fila.put("diferencia_sombra", dif);
							fila.put("avisos_traduccion", rs.getInt("avisos_traduccion"));
							if (Math.abs(dif) >= 1.0) {
								cntConDiferencia++;
							}
						}

						// Acumular KPIs
						totalPedidos++;
						totalVentas += totalNeto;

						if ("TK".equals(orig) || "TW".equals(orig)) cntKuno++;
						else if ("APP".equals(orig)) cntApp++;
						else if ("BOT".equals(orig) || "CRM".equals(orig)) cntBot++;
						else if ("RAPPI".equals(orig) || "RAP".equals(orig) || "R".equals(orig)) cntRappi++;
						else if ("DIDI".equals(orig) || "D".equals(orig)) cntDidi++;
						else if ("C".equals(orig)) cntContact++;
						else cntOtros++;

						if ("NUEVO".equals(modoFila)) cntNuevo++;
						else if ("PARALELO".equals(modoFila)) cntParalelo++;
						else cntLegacy++;

						listado.add(fila);
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error en consultarPedidosUnificados: " + e.getMessage(), e);
		} finally {
			if (con != null) {
				try { con.close(); } catch (Exception ignorada) {}
			}
		}

		// Construir objeto de respuesta
		respuesta.put("pedidos", listado);

		JSONObject kpis = new JSONObject();
		kpis.put("total_pedidos", totalPedidos);
		kpis.put("total_ventas", totalVentas);
		kpis.put("kuno", cntKuno);
		kpis.put("app", cntApp);
		kpis.put("bot", cntBot);
		kpis.put("rappi", cntRappi);
		kpis.put("didi", cntDidi);
		kpis.put("contact", cntContact);
		kpis.put("otros", cntOtros);
		kpis.put("modo_nuevo", cntNuevo);
		kpis.put("modo_paralelo", cntParalelo);
		kpis.put("modo_legacy", cntLegacy);
		kpis.put("con_diferencia", cntConDiferencia);
		respuesta.put("kpis", kpis);

		return respuesta;
	}

	/**
	 * Consulta el detalle completo de un pedido, incluyendo sus líneas en Central,
	 * en pos_central y su comparativa de sombra si existe.
	 */
	public static JSONObject consultarDetallePedido(int idPedido) {
		JSONObject resultado = new JSONObject();
		JSONArray lineasLegacy = new JSONArray();
		JSONArray lineasPosCentral = new JSONArray();
		JSONObject datosSombra = null;

		Connection con = null;
		try {
			con = new ConexionBaseDatos().obtenerConexionBDPrincipal();

			// 1. Lineas en pizzaamericana.detalle_pedido
			String sqlLeg = "SELECT dp.iddetallepedido, dp.idproducto, IFNULL(p.descripcion, 'Item ' + dp.idproducto) AS nombreproducto, "
					+ "  dp.cantidad, dp.idespecialidad1, IFNULL(e1.descripcion, '') AS nomespecialidad1, "
					+ "  dp.idespecialidad2, IFNULL(e2.descripcion, '') AS nomespecialidad2, "
					+ "  dp.valorUnitario, dp.valorTotal, IFNULL(dp.adicion,'') AS adicion, IFNULL(dp.observacion,'') AS observacion "
					+ "FROM pizzaamericana.detalle_pedido dp "
					+ "LEFT JOIN pizzaamericana.producto p ON p.idproducto = dp.idproducto "
					+ "LEFT JOIN pizzaamericana.especialidad e1 ON e1.idespecialidad = dp.idespecialidad1 "
					+ "LEFT JOIN pizzaamericana.especialidad e2 ON e2.idespecialidad = dp.idespecialidad2 "
					+ "WHERE dp.idpedido = ? ORDER BY dp.iddetallepedido";

			try (PreparedStatement ps = con.prepareStatement(sqlLeg)) {
				ps.setInt(1, idPedido);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						JSONObject lin = new JSONObject();
						lin.put("iddetallepedido", rs.getInt("iddetallepedido"));
						lin.put("idproducto", rs.getInt("idproducto"));
						lin.put("nombreproducto", rs.getString("nombreproducto"));
						lin.put("cantidad", rs.getDouble("cantidad"));
						lin.put("especialidad1", rs.getString("nomespecialidad1"));
						lin.put("especialidad2", rs.getString("nomespecialidad2"));
						lin.put("valorunitario", rs.getDouble("valorUnitario"));
						lin.put("valortotal", rs.getDouble("valorTotal"));
						lin.put("adicion", rs.getString("adicion"));
						lin.put("observacion", rs.getString("observacion"));
						lineasLegacy.add(lin);
					}
				}
			}

			// 2. Lineas en pos_central.detalle_pedido si fue proyectado al POS
			String sqlPos = "SELECT dp.iddetallepedido, dp.idproducto, IFNULL(p.descripcion, 'POS ' + dp.idproducto) AS descripcion, "
					+ "  dp.cantidad, dp.valorunitario, dp.valortotal, dp.iddetalle_pedido_master, dp.iddetalle_modificador "
					+ "FROM pos_central.detalle_pedido dp "
					+ "JOIN pos_central.pedido ped ON ped.idpedidotienda = dp.idpedidotienda "
					+ "LEFT JOIN pos_central.producto p ON p.idproducto = dp.idproducto "
					+ "WHERE ped.idpedidocontact = ? ORDER BY dp.iddetallepedido";

			try (PreparedStatement ps = con.prepareStatement(sqlPos)) {
				ps.setInt(1, idPedido);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						JSONObject lin = new JSONObject();
						lin.put("iddetallepedido", rs.getInt("iddetallepedido"));
						lin.put("idproducto", rs.getInt("idproducto"));
						lin.put("descripcion", rs.getString("descripcion"));
						lin.put("cantidad", rs.getDouble("cantidad"));
						lin.put("valorunitario", rs.getDouble("valorunitario"));
						lin.put("valortotal", rs.getDouble("valortotal"));
						lin.put("idmaster", rs.getInt("iddetalle_pedido_master"));
						lin.put("modificador", rs.getInt("iddetalle_modificador"));
						lineasPosCentral.add(lin);
					}
				}
			}

			// 3. Informacion de auditoria sombra si corrio en paralelo
			String sqlSom = "SELECT idsombra, canal, modo_ejecucion, version_motor, estado_motor, "
					+ "  subtotal_viejo, total_viejo, subtotal_motor, total_motor, diferencia, "
					+ "  avisos_traduccion, duracion_ms, solicitud_json, resultado_json "
					+ "FROM pos_central.sombra_pedido_canal WHERE idpedido_central = ? ORDER BY idsombra DESC LIMIT 1";

			try (PreparedStatement ps = con.prepareStatement(sqlSom)) {
				ps.setInt(1, idPedido);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						datosSombra = new JSONObject();
						datosSombra.put("idsombra", rs.getLong("idsombra"));
						datosSombra.put("canal", rs.getString("canal"));
						datosSombra.put("modo_ejecucion", rs.getString("modo_ejecucion"));
						datosSombra.put("version_motor", rs.getString("version_motor"));
						datosSombra.put("estado_motor", rs.getString("estado_motor"));
						datosSombra.put("subtotal_viejo", rs.getDouble("subtotal_viejo"));
						datosSombra.put("total_viejo", rs.getDouble("total_viejo"));
						datosSombra.put("subtotal_motor", rs.getDouble("subtotal_motor"));
						datosSombra.put("total_motor", rs.getDouble("total_motor"));
						datosSombra.put("diferencia", rs.getDouble("diferencia"));
						datosSombra.put("avisos_traduccion", rs.getInt("avisos_traduccion"));
						datosSombra.put("duracion_ms", rs.getInt("duracion_ms"));
						datosSombra.put("solicitud_json", rs.getString("solicitud_json"));
						datosSombra.put("resultado_json", rs.getString("resultado_json"));
					}
				}
			}

			// 3.1 Si no esta en sombra_pedido_canal, buscar en sombra_vivo (BOT CRM)
			if (datosSombra == null) {
				String sqlSomBot = "SELECT idsombra, 'BOT' as canal, 'PARALELO' as modo_ejecucion, version_motor, estado_motor, "
						+ "  subtotal_viejo, subtotal_viejo as total_viejo, subtotal_motor, subtotal_motor as total_motor, diferencia_vs_viejo as diferencia, "
						+ "  avisos_traduccion, duracion_ms, solicitud_json, resultado_json "
						+ "FROM pos_central.sombra_vivo WHERE idpedido = ? ORDER BY idsombra DESC LIMIT 1";
				try (PreparedStatement ps = con.prepareStatement(sqlSomBot)) {
					ps.setInt(1, idPedido);
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							datosSombra = new JSONObject();
							datosSombra.put("idsombra", rs.getLong("idsombra"));
							datosSombra.put("canal", rs.getString("canal"));
							datosSombra.put("modo_ejecucion", rs.getString("modo_ejecucion"));
							datosSombra.put("version_motor", rs.getString("version_motor"));
							datosSombra.put("estado_motor", rs.getString("estado_motor"));
							datosSombra.put("subtotal_viejo", rs.getDouble("subtotal_viejo"));
							datosSombra.put("total_viejo", rs.getDouble("total_viejo"));
							datosSombra.put("subtotal_motor", rs.getDouble("subtotal_motor"));
							datosSombra.put("total_motor", rs.getDouble("total_motor"));
							datosSombra.put("diferencia", rs.getDouble("diferencia"));
							datosSombra.put("avisos_traduccion", rs.getInt("avisos_traduccion"));
							datosSombra.put("duracion_ms", rs.getInt("duracion_ms"));
							datosSombra.put("solicitud_json", rs.getString("solicitud_json"));
							datosSombra.put("resultado_json", rs.getString("resultado_json"));
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error en consultarDetallePedido: " + e.getMessage(), e);
		} finally {
			if (con != null) {
				try { con.close(); } catch (Exception ignorada) {}
			}
		}

		resultado.put("idpedido", idPedido);
		resultado.put("lineas_legacy", lineasLegacy);
		resultado.put("lineas_pos_central", lineasPosCentral);
		if (datosSombra != null) {
			resultado.put("sombra", datosSombra);
		}

		return resultado;
	}

	private static String normalizarFecha(String f) {
		if (f == null || f.trim().isEmpty()) {
			return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
		}
		f = f.trim();
		if (f.contains("/")) {
			// Formato dd/MM/yyyy -> yyyy-MM-dd
			String[] p = f.split("/");
			if (p.length == 3) {
				return p[2] + "-" + p[1] + "-" + p[0];
			}
		}
		return f;
	}

	private static String resolverCanalLegible(String origen) {
		if ("TK".equals(origen) || "TW".equals(origen)) return "Tienda Virtual";
		if ("APP".equals(origen)) return "App Móvil";
		if ("BOT".equals(origen) || "CRM".equals(origen)) return "CRM BOT";
		if ("RAPPI".equals(origen) || "RAP".equals(origen) || "R".equals(origen)) return "Rappi";
		if ("DIDI".equals(origen) || "D".equals(origen)) return "DiDi";
		if ("C".equals(origen)) return "Contact Center";
		if ("MOTOR".equals(origen) || "M".equals(origen)) return "Nuevo Motor";
		return origen;
	}
}
