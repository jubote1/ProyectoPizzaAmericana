package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaControladorCC.motor.ConectorTiendaPos;
import capaDAOCC.ClienteDAO;
import capaModeloCC.Cliente;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;
import conexionCC.ConexionBaseDatos;

/**
 * Servlet para reintento rápido de despacho a tienda y monitoreo de pedidos no enviados del día.
 *
 * Mapeo:
 *   - POST /api/motor/reintentar-despacho: Reintenta el envío a tienda de un pedido central existente (enviadopixel = 0).
 *   - GET  /api/motor/reintentar-despacho?accion=no-enviados-hoy: Consulta la lista y conteo de pedidos de hoy no enviados a tienda.
 */
@WebServlet({ "/api/motor/reintentar-despacho", "/ReintentarDespachoPedido" })
public class ReintentarDespachoPedido extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();

		String accion = request.getParameter("accion");
		if (accion == null || accion.trim().isEmpty()) {
			accion = "no-enviados-hoy";
		}

		try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal()) {
			if ("no-enviados-hoy".equalsIgnoreCase(accion.trim())) {
				String sql = "SELECT p.idpedido, p.idtienda, ifnull(t.nombre, concat('Tienda ', p.idtienda)) AS nombre_tienda, "
						+ "       p.fechapedido, p.fechainsercion, ifnull(p.usuariopedido, '') AS usuariopedido, "
						+ "       p.total_neto, p.enviadopixel, p.idestadopedido, "
						+ "       ifnull(c.idcliente, 0) AS idcliente, "
						+ "       concat(ifnull(c.nombre,''), ' ', ifnull(c.apellido,'')) AS nombre_cliente, "
						+ "       coalesce(nullif(c.telefono_celular,''), c.telefono, '') AS telefono_cliente, "
						+ "       ifnull(c.direccion, '') AS direccion_cliente "
						+ "FROM pizzaamericana.pedido p "
						+ "LEFT JOIN pizzaamericana.tienda t ON t.idtienda = p.idtienda "
						+ "LEFT JOIN pizzaamericana.cliente c ON c.idcliente = p.idcliente "
						+ "WHERE p.fechapedido = current_date() "
						+ "  AND p.enviadopixel = 0 "
						+ "ORDER BY p.idpedido DESC";

				List<Map<String, Object>> lista = new ArrayList<>();
				try (PreparedStatement ps = con.prepareStatement(sql);
						ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						Map<String, Object> ped = new HashMap<>();
						ped.put("idPedido", rs.getInt("idpedido"));
						ped.put("idTienda", rs.getInt("idtienda"));
						ped.put("nombreTienda", rs.getString("nombre_tienda"));
						ped.put("fechaPedido", rs.getString("fechapedido"));
						ped.put("fechaInsercion", rs.getString("fechainsercion"));
						ped.put("usuarioPedido", rs.getString("usuariopedido"));
						ped.put("totalNeto", rs.getDouble("total_neto"));
						ped.put("enviadoPixel", rs.getInt("enviadopixel"));
						ped.put("idEstadoPedido", rs.getInt("idestadopedido"));
						ped.put("idCliente", rs.getInt("idcliente"));
						ped.put("nombreCliente", rs.getString("nombre_cliente").trim());
						ped.put("telefonoCliente", rs.getString("telefono_cliente").trim());
						ped.put("direccionCliente", rs.getString("direccion_cliente").trim());
						lista.add(ped);
					}
				}

				Map<String, Object> resp = new HashMap<>();
				resp.put("exito", true);
				resp.put("total", lista.size());
				resp.put("pedidos", lista);
				response.setStatus(HttpServletResponse.SC_OK);
				out.write(GSON.toJson(resp));
				return;
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"exito\":false,\"mensaje\":\"Acción no soportada: " + accion + "\"}");
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"exito\":false,\"mensaje\":\"Error consultando pedidos no enviados: " + t.getMessage() + "\"}");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();

		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		}

		String jsonEntrada = sb.toString();
		if (jsonEntrada.trim().isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.write("{\"exito\":false,\"mensaje\":\"Cuerpo JSON vacío\"}");
			return;
		}

		try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal()) {
			JsonObject root = JsonParser.parseString(jsonEntrada).getAsJsonObject();
			int idPedido = root.has("idPedido") ? root.get("idPedido").getAsInt() : 0;
			int idPedidoPos = root.has("idPedidoPos") ? root.get("idPedidoPos").getAsInt() : 0;
			int idTiendaParam = root.has("idTienda") ? root.get("idTienda").getAsInt() : 0;

			if (idPedido <= 0) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"exito\":false,\"mensaje\":\"idPedido requerido y debe ser mayor a 0\"}");
				return;
			}

			// 1. Verificar estado actual del pedido en pizzaamericana.pedido
			int enviadopixel = 0;
			int numposheader = 0;
			int idTiendaBD = 0;
			int idClienteBD = 0;
			double totalNetoBD = 0.0;
			String usuarioPedidoBD = "";
			double tiempoPedidoBD = 30.0;

			String sqlVerif = "SELECT enviadopixel, numposheader, idtienda, idcliente, total_neto, usuariopedido, tiempopedido "
					+ "FROM pizzaamericana.pedido WHERE idpedido = ?";
			try (PreparedStatement psVer = con.prepareStatement(sqlVerif)) {
				psVer.setInt(1, idPedido);
				try (ResultSet rsVer = psVer.executeQuery()) {
					if (rsVer.next()) {
						enviadopixel = rsVer.getInt("enviadopixel");
						numposheader = rsVer.getInt("numposheader");
						idTiendaBD = rsVer.getInt("idtienda");
						idClienteBD = rsVer.getInt("idcliente");
						totalNetoBD = rsVer.getDouble("total_neto");
						usuarioPedidoBD = rsVer.getString("usuariopedido");
						tiempoPedidoBD = rsVer.getDouble("tiempopedido");
					} else {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						out.write("{\"exito\":false,\"mensaje\":\"Pedido #" + idPedido + " no encontrado en el sistema Central\"}");
						return;
					}
				}
			}

			// Si ya fue enviado previamente a la tienda, evitar duplicar comanda
			if (enviadopixel == 1 && numposheader > 0) {
				Map<String, Object> respYaEnviado = new HashMap<>();
				respYaEnviado.put("exito", true);
				respYaEnviado.put("yaEnviado", true);
				respYaEnviado.put("idPedido", idPedido);
				respYaEnviado.put("numeroFactura", numposheader);
				respYaEnviado.put("mensaje", "El pedido #" + idPedido + " ya fue despachado previamente a la tienda con comanda #" + numposheader);
				response.setStatus(HttpServletResponse.SC_OK);
				out.write(GSON.toJson(respYaEnviado));
				return;
			}

			int idTiendaEfectiva = idTiendaParam > 0 ? idTiendaParam : idTiendaBD;
			int idClienteEfectivo = idClienteBD;

			// 2. Extraer o reconstruir los objetos de despacho
			Cliente cli = null;
			MotorSolicitud solicitud = null;
			MotorResultado res = null;
			int idFormaPagoTienda = 1;
			double valorFormaPago = totalNetoBD;
			String usuarioEfectivo = usuarioPedidoBD != null && !usuarioPedidoBD.trim().isEmpty() ? usuarioPedidoBD : "MOTOR";
			double tiempoEfectivo = tiempoPedidoBD > 0 ? tiempoPedidoBD : 30.0;
			String origenDespacho = "CC-NUEVO";
			String altDespacho = "";
			String plataforma = "";

			JsonObject payloadObj = root.has("payload") && root.get("payload").isJsonObject()
					? root.getAsJsonObject("payload")
					: null;

			if (payloadObj != null) {
				// CASO A: Payload completo en memoria (reintento inmediato desde modal post-despacho)
				if (payloadObj.has("idCliente")) {
					idClienteEfectivo = payloadObj.get("idCliente").getAsInt();
				}
				if (payloadObj.has("idFormaPagoTienda")) {
					idFormaPagoTienda = payloadObj.get("idFormaPagoTienda").getAsInt();
				}
				if (payloadObj.has("valorFormaPago")) {
					valorFormaPago = payloadObj.get("valorFormaPago").getAsDouble();
				}
				if (payloadObj.has("tiempoPedido")) {
					tiempoEfectivo = payloadObj.get("tiempoPedido").getAsDouble();
				}
				if (payloadObj.has("origen")) {
					origenDespacho = payloadObj.get("origen").getAsString();
				}
				if (payloadObj.has("idPedidoAlt")) {
					altDespacho = payloadObj.get("idPedidoAlt").getAsString();
				}
				if (payloadObj.has("plataforma")) {
					plataforma = payloadObj.get("plataforma").getAsString();
				}
				if (payloadObj.has("solicitud")) {
					solicitud = GSON.fromJson(payloadObj.get("solicitud"), MotorSolicitud.class);
				}
				if (root.has("resultadoMotor")) {
					res = GSON.fromJson(root.get("resultadoMotor"), MotorResultado.class);
				} else if (payloadObj.has("resultadoMotor")) {
					res = GSON.fromJson(payloadObj.get("resultadoMotor"), MotorResultado.class);
				}
			}

			// Obtener cliente desde BD si aún no se tiene
			cli = ClienteDAO.obtenerClienteporID(idClienteEfectivo);

			// Si no viene solicitud/resultado en el payload, reconstruir desde pos_central o pizzaamericana
			if (solicitud == null) {
				solicitud = new MotorSolicitud();
				solicitud.contexto = new MotorSolicitud.Contexto();
				solicitud.contexto.idtienda = idTiendaEfectiva;
				solicitud.contexto.idtipopedido = 1;
			}

			if (res == null) {
				res = reconstruirResultadoMotorDesdeBD(con, idPedido, idPedidoPos);
			}

			if (res == null || res.lineas == null || res.lineas.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"exito\":false,\"mensaje\":\"No se pudieron recuperar las líneas de productos del pedido #" + idPedido + "\"}");
				return;
			}

			// Obtener forma de pago de BD si no vino en el payload
			if (payloadObj == null) {
				String sqlFP = "SELECT idforma_pago, valorformapago FROM pizzaamericana.pedido_forma_pago WHERE idpedido = ? LIMIT 1";
				try (PreparedStatement psFP = con.prepareStatement(sqlFP)) {
					psFP.setInt(1, idPedido);
					try (ResultSet rsFP = psFP.executeQuery()) {
						if (rsFP.next()) {
							idFormaPagoTienda = rsFP.getInt("idforma_pago");
							valorFormaPago = rsFP.getDouble("valorformapago");
						}
					}
				}
			}

			// 3. Ejecutar el despacho a tienda vía ConectorTiendaPos
			ConectorTiendaPos.DespachoResultado dr = ConectorTiendaPos.despachar(con, idPedido, idTiendaEfectiva,
					cli, solicitud, res, idFormaPagoTienda, valorFormaPago, usuarioEfectivo, tiempoEfectivo,
					origenDespacho, altDespacho, plataforma, idPedidoPos);

			Map<String, Object> respFinal = new HashMap<>();
			respFinal.put("exito", dr.exito);
			respFinal.put("idPedido", idPedido);
			respFinal.put("idPedidoPos", idPedidoPos);
			respFinal.put("numeroFactura", dr.numeroFactura);
			respFinal.put("mensaje", dr.mensaje);
			respFinal.put("despachoTienda", dr);

			response.setStatus(HttpServletResponse.SC_OK);
			out.write(GSON.toJson(respFinal));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"exito\":false,\"mensaje\":\"Error interno al reintentar despacho: " + t.getMessage() + "\"}");
		}
	}

	/**
	 * Reconstruye el MotorResultado desde pos_central o pizzaamericana.detalle_pedido
	 * cuando se invoca el reintento de un pedido anterior sin payload en memoria.
	 */
	private MotorResultado reconstruirResultadoMotorDesdeBD(Connection con, int idPedidoCentral, int idPedidoPos) {
		MotorResultado res = new MotorResultado();
		res.estado = MotorResultado.OK;
		res.lineas = new ArrayList<>();

		// Intento 1: Leer desde pos_central.pedido y pos_central.detalle_pedido
		try {
			int idPosEfectivo = idPedidoPos;
			if (idPosEfectivo <= 0) {
				String sqlBuscPos = "SELECT idpedidotienda, total_neto FROM pos_central.pedido WHERE idpedidocontact = ? LIMIT 1";
				try (PreparedStatement psBP = con.prepareStatement(sqlBuscPos)) {
					psBP.setInt(1, idPedidoCentral);
					try (ResultSet rsBP = psBP.executeQuery()) {
						if (rsBP.next()) {
							idPosEfectivo = rsBP.getInt("idpedidotienda");
							res.total = rsBP.getDouble("total_neto");
							res.subtotal = res.total;
						}
					}
				}
			}

			if (idPosEfectivo > 0) {
				String sqlDetPos = "SELECT iddetalle_pedido, idproducto, cantidad, valorunitario, valortotal, "
						+ "iddetalle_pedido_master, iddetalle_modificador "
						+ "FROM pos_central.detalle_pedido WHERE idpedidotienda = ? ORDER BY iddetalle_pedido";
				try (PreparedStatement psDP = con.prepareStatement(sqlDetPos)) {
					psDP.setInt(1, idPosEfectivo);
					try (ResultSet rsDP = psDP.executeQuery()) {
						int refCounter = 1;
						Map<Integer, Integer> mapIdDetToRef = new HashMap<>();
						while (rsDP.next()) {
							int idDet = rsDP.getInt("iddetalle_pedido");
							int idMaster = rsDP.getInt("iddetalle_pedido_master");
							int idMod = rsDP.getInt("iddetalle_modificador");

							MotorResultado.Linea l = new MotorResultado.Linea();
							l.ref = refCounter++;
							mapIdDetToRef.put(idDet, l.ref);

							l.idproducto = rsDP.getInt("idproducto");
							l.cantidad = rsDP.getDouble("cantidad");
							l.valorUnitario = rsDP.getDouble("valorunitario");
							l.valorTotal = rsDP.getDouble("valortotal");
							l.refMaster = (idMaster > 0 && mapIdDetToRef.containsKey(idMaster))
									? mapIdDetToRef.get(idMaster)
									: idMaster;
							l.refModificador = (idMod > 0 && mapIdDetToRef.containsKey(idMod))
									? mapIdDetToRef.get(idMod)
									: idMod;

							res.lineas.add(l);
						}
					}
				}

				if (!res.lineas.isEmpty()) {
					return res;
				}
			}
		} catch (Exception exPos) {
			System.err.println("Advertencia reconstruyendo desde pos_central: " + exPos.getMessage());
		}

		// Intento 2: Fallback desde pizzaamericana.detalle_pedido
		try {
			String sqlLeg = "SELECT idproducto, cantidad, valorUnitario, valorTotal FROM pizzaamericana.detalle_pedido WHERE idpedido = ?";
			try (PreparedStatement psL = con.prepareStatement(sqlLeg)) {
				psL.setInt(1, idPedidoCentral);
				try (ResultSet rsL = psL.executeQuery()) {
					int refCounter = 1;
					double acumTotal = 0.0;
					while (rsL.next()) {
						MotorResultado.Linea l = new MotorResultado.Linea();
						l.ref = refCounter++;
						l.idproducto = rsL.getInt("idproducto");
						l.cantidad = rsL.getDouble("cantidad");
						l.valorUnitario = rsL.getDouble("valorUnitario");
						l.valorTotal = rsL.getDouble("valorTotal");
						l.refMaster = 0;
						l.refModificador = 0;
						acumTotal += l.valorTotal;
						res.lineas.add(l);
					}
					if (res.total <= 0) {
						res.total = acumTotal;
						res.subtotal = acumTotal;
					}
				}
			}
		} catch (Exception exLeg) {
			System.err.println("Advertencia reconstruyendo desde pizzaamericana.detalle_pedido: " + exLeg.getMessage());
		}

		return res;
	}

	private void configurarCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
	}
}
