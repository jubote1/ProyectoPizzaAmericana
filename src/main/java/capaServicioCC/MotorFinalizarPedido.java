package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaDAOCC.ClienteDAO;
import capaControladorCC.motor.CatalogoPos;
import capaControladorCC.motor.ConectorTiendaPos;
import capaControladorCC.motor.EnrutadorDespachoTienda;
import capaControladorCC.motor.MotorPedidoV1;
import capaControladorCC.motor.ProyectorPedidoLegacy;
import capaModeloCC.Cliente;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;
import conexionCC.ConexionBaseDatos;

/**
 * Endpoint API REST para creacion, cotizacion, proyeccion y despacho de pedidos
 * con el Motor Nuevo (Etapa 3 - Piloto Manrique).
 *
 * Utilizado por la Nueva Pantalla de Contact Center y la Nueva Tienda Virtual.
 *
 * Rutas: /api/motor/finalizar-pedido y /MotorFinalizarPedido
 * Metodos: POST, OPTIONS
 */
@WebServlet({ "/api/motor/finalizar-pedido", "/MotorFinalizarPedido" })
public class MotorFinalizarPedido extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Gson GSON = new Gson();

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		configurarCors(response);
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");

		PrintWriter out = response.getWriter();
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String linea;
			while ((linea = reader.readLine()) != null) {
				sb.append(linea);
			}
		}

		String jsonEntrada = sb.toString().trim();
		if (jsonEntrada.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.write("{\"exito\":false,\"mensaje\":\"Cuerpo de peticion vacio.\"}");
			return;
		}

		try (Connection con = new ConexionBaseDatos().obtenerConexionBDPrincipal()) {
			JsonObject root = JsonParser.parseString(jsonEntrada).getAsJsonObject();

			int idTienda = root.has("idTienda") ? root.get("idTienda").getAsInt() : 1;
			int idCliente = root.has("idCliente") ? root.get("idCliente").getAsInt() : 0;
			String usuario = root.has("usuario") ? root.get("usuario").getAsString() : "CONTACT";
			int idFormaPagoTienda = root.has("idFormaPagoTienda") ? root.get("idFormaPagoTienda").getAsInt() : 1;
			double valorFormaPago = root.has("valorFormaPago") ? root.get("valorFormaPago").getAsDouble() : 0.0;
			double tiempoPedido = root.has("tiempoPedido") ? root.get("tiempoPedido").getAsDouble() : 30.0;
			String origen = root.has("origen") ? root.get("origen").getAsString() : "C";

			if (!root.has("solicitud")) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				out.write("{\"exito\":false,\"mensaje\":\"Se requiere el objeto 'solicitud' con la estructura MotorSolicitud.\"}");
				return;
			}

			MotorSolicitud solicitud = GSON.fromJson(root.get("solicitud"), MotorSolicitud.class);
			if (solicitud.contexto == null) {
				solicitud.contexto = new MotorSolicitud.Contexto();
			}
			if (solicitud.contexto.fechaHora == null || solicitud.contexto.fechaHora.trim().isEmpty()) {
				solicitud.contexto.fechaHora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(new java.util.Date());
			}

			// 1. Cotizar con el motor
			CatalogoPos cat = CatalogoPos.cargar(con);
			MotorPedidoV1 motor = new MotorPedidoV1(cat);
			MotorResultado res = motor.cotizar(solicitud);

			if (!MotorResultado.OK.equals(res.estado)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				Map<String, Object> respError = new HashMap<String, Object>();
				respError.put("exito", false);
				respError.put("mensaje", "El motor rechazo la cotizacion del pedido.");
				respError.put("resultado", res);
				out.write(GSON.toJson(respError));
				return;
			}

			// 2. Crear cabecera inicial en pizzaamericana.pedido para obtener idpedido central
			int idPedidoCentral = 0;
			String sqlInsPed = "insert into pizzaamericana.pedido (idtienda, idcliente, idestadopedido, fechapedido, "
					+ "usuariopedido, total_bruto, descuento, total_neto, tiempopedido, origen, programado, hora_programado, idtipopedido) "
					+ "values (?,?,1,current_date(),?,?,?,?,?,?,?,?,?)";

			try (PreparedStatement ps = con.prepareStatement(sqlInsPed, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, idTienda);
				ps.setInt(2, idCliente);
				ps.setString(3, usuario);
				ps.setDouble(4, res.subtotal);
				ps.setDouble(5, res.descuento);
				ps.setDouble(6, res.total);
				ps.setDouble(7, tiempoPedido);
				ps.setString(8, origen);
				ps.setString(9, solicitud.contexto.programado ? "S" : "N");
				ps.setString(10, solicitud.contexto.horaProgramado != null ? solicitud.contexto.horaProgramado : "");
				ps.setInt(11, solicitud.contexto.idtipopedido > 0 ? solicitud.contexto.idtipopedido : 1);
				ps.executeUpdate();
				try (ResultSet rs = ps.getGeneratedKeys()) {
					if (rs.next()) {
						idPedidoCentral = rs.getInt(1);
					}
				}
			}

			if (idPedidoCentral <= 0) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				out.write("{\"exito\":false,\"mensaje\":\"No fue posible generar el numero de pedido central.\"}");
				return;
			}

			// 3. Proyectar en pos_central y pizzaamericana.detalle_pedido
			ProyectorPedidoLegacy.proyectarPedido(con, idPedidoCentral, idTienda, idCliente, solicitud, res, usuario);

			// 4. Verificar si la tienda tiene habilitado el despacho directo
			boolean esPiloto = EnrutadorDespachoTienda.esTiendaPiloto(con, idTienda);
			ConectorTiendaPos.DespachoResultado dr = null;

			if (esPiloto) {
				Cliente cli = idCliente > 0 ? ClienteDAO.obtenerClienteporID(idCliente) : null;
				if (root.has("cliente")) {
					try {
						JsonObject clJson = root.getAsJsonObject("cliente");
						if (clJson != null) {
							if (cli == null) {
								cli = new Cliente();
								cli.setIdcliente(idCliente);
							}
							if (clJson.has("nombres")) cli.setNombres(clJson.get("nombres").getAsString());
							if (clJson.has("apellidos")) cli.setApellidos(clJson.get("apellidos").getAsString());
							if (clJson.has("telefono")) cli.setTelefonoCelular(clJson.get("telefono").getAsString());
							if (clJson.has("direccion")) cli.setDireccion(clJson.get("direccion").getAsString());
							if (clJson.has("municipio")) cli.setMunicipio(clJson.get("municipio").getAsString());
							if (clJson.has("zona")) cli.setZonaDireccion(clJson.get("zona").getAsString());
							if (clJson.has("observacion")) cli.setObservacion(clJson.get("observacion").getAsString());
							if (clJson.has("latitud")) {
								cli.setLatitud((float) clJson.get("latitud").getAsDouble());
							}
							if (clJson.has("longitud")) {
								cli.setLontitud((float) clJson.get("longitud").getAsDouble());
							}
							if (clJson.has("distanciatienda")) {
								cli.setDistanciaTienda(clJson.get("distanciatienda").getAsDouble());
							}
							if (clJson.has("email")) {
								cli.setEmail(clJson.get("email").getAsString());
							}
							if (clJson.has("emailfact")) {
								cli.setEmailFacturacion(clJson.get("emailfact").getAsString());
							}
							if (clJson.has("clientesiniden")) {
								cli.setClienteSinIden(clJson.get("clientesiniden").getAsString());
							}
							if (clJson.has("idtipopersona")) {
								cli.setIdTipoPersona(clJson.get("idtipopersona").getAsInt());
							}
							if (clJson.has("identificacion")) {
								cli.setIdentificacion(clJson.get("identificacion").getAsString());
							}
						}
					} catch (Exception exCli) {
						// Ignorado si no viene estructura completa
					}
				}
				dr = ConectorTiendaPos.despachar(con, idPedidoCentral, idTienda, cli, solicitud, res, idFormaPagoTienda,
						valorFormaPago, usuario, tiempoPedido, origen);
			}

			Map<String, Object> respFinal = new HashMap<String, Object>();
			respFinal.put("exito", true);
			respFinal.put("idPedido", idPedidoCentral);
			respFinal.put("total", res.total);
			respFinal.put("descuento", res.descuento);
			respFinal.put("subtotal", res.subtotal);
			respFinal.put("esTiendaPiloto", esPiloto);
			if (dr != null) {
				respFinal.put("despachoTienda", dr);
			} else {
				respFinal.put("mensaje", "Pedido registrado y proyectado en Central con id #" + idPedidoCentral);
			}
			respFinal.put("resultadoMotor", res);

			response.setStatus(HttpServletResponse.SC_OK);
			out.write(GSON.toJson(respFinal));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.write("{\"exito\":false,\"mensaje\":\"Error interno procesando pedido: " + t.getMessage() + "\"}");
		}
	}

	private void configurarCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
	}
}
