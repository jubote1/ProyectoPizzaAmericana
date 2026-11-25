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
import javax.servlet.http.HttpSession;

import capaModeloCC.Usuario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.simple.JSONObject;

import capaControladorCC.PedidoCtrl;
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
			// El pedido queda a nombre de QUIEN LO TOMA: el usuario de la sesion. No se
			// confia en lo que mande el navegador (antes la pantalla mandaba "PILOTO"
			// quemado y todo quedaba a ese nombre). Solo si no hay sesion -llamadas de
			// maquina a maquina- se usa el "usuario" del cuerpo, y por defecto CONTACT.
			String usuario = root.has("usuario") ? root.get("usuario").getAsString() : "CONTACT";
			HttpSession sesionPedido = request.getSession(false);
			Usuario usuarioSesion = (sesionPedido != null) ? (Usuario) sesionPedido.getAttribute("usuario") : null;
			if (usuarioSesion != null && usuarioSesion.getNombreUsuario() != null
					&& !usuarioSesion.getNombreUsuario().trim().isEmpty()) {
				usuario = usuarioSesion.getNombreUsuario().trim();
			}
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

			// 1.1 Extraer datos de cliente si vienen en el cuerpo de la peticion
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
						if (clJson.has("politicadatos")) {
							cli.setPoliticaDatos(clJson.get("politicadatos").getAsString());
						}
						if (cli.getIdcliente() > 0) {
							try (PreparedStatement psUpdCli = con.prepareStatement(
									"update pizzaamericana.cliente set politica_datos = ?, telefono_celular = ?, email = ? where idcliente = ?")) {
								psUpdCli.setString(1, cli.getPoliticaDatos() != null ? cli.getPoliticaDatos() : "S");
								psUpdCli.setString(2, cli.getTelefonoCelular() != null ? cli.getTelefonoCelular() : "");
								psUpdCli.setString(3, cli.getEmail() != null ? cli.getEmail() : "");
								psUpdCli.setInt(4, cli.getIdcliente());
								psUpdCli.executeUpdate();
							} catch (Exception exUpdCli) {
								// Ignorado si falla el update puntual
							}
						}
					}
				} catch (Exception exCli) {
					// Ignorado si no viene estructura completa
				}
			}

			// 1.2 Regla Paramétrica: Si la forma de pago es Virtual (virtual = 'S'), exige política de datos aceptada
			boolean esFormaPagoVirtual = false;
			String nombreFormaPago = "";
			if (idFormaPagoTienda > 0) {
				String sqlCheckFP = "select virtual, nombre from pizzaamericana.forma_pago where idforma_pago = ?";
				try (PreparedStatement psFP = con.prepareStatement(sqlCheckFP)) {
					psFP.setInt(1, idFormaPagoTienda);
					try (ResultSet rsFP = psFP.executeQuery()) {
						if (rsFP.next()) {
							String esVirtual = rsFP.getString("virtual");
							nombreFormaPago = rsFP.getString("nombre");
							if ("S".equalsIgnoreCase(esVirtual)) {
								esFormaPagoVirtual = true;
								String polDatos = (cli != null && cli.getPoliticaDatos() != null) ? cli.getPoliticaDatos().trim() : "N";
								if (!"S".equalsIgnoreCase(polDatos)) {
									response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
									out.write("{\"exito\":false,\"mensaje\":\"La forma de pago '" + nombreFormaPago
											+ "' es virtual y requiere obligatoriamente que el cliente acepte la Política de Tratamiento de Datos (Habeas Data) para el envío de notificaciones y links de pago.\"}");
									return;
								}
							}
						}
					}
				} catch (Exception exFPCheck) {
					System.err.println("Advertencia verificando parámetro virtual de forma_pago: " + exFPCheck.getMessage());
				}
			}

			// 2. Crear cabecera inicial en pizzaamericana.pedido para obtener idpedido central
			// Si es virtual: idestadopedido = 2 (Pendiente Pago Virtual), enviadopixel = 2 (Pendiente confirmación)
			// Si no es virtual: idestadopedido = 1 (Nuevo), enviadopixel = 0
			int idEstadoInicial = esFormaPagoVirtual ? 2 : 1;
			int enviadoPixelInicial = esFormaPagoVirtual ? 2 : 0;
			int idPedidoCentral = 0;

			// Descomposición fiscal oficial (Impoconsumo 8% restaurantes)
			double totalNeto = res.total;
			double totalBruto = totalNeto / 1.08;
			double valorImpuesto = totalNeto - totalBruto;

			String sqlInsPed = "insert into pizzaamericana.pedido (idtienda, idcliente, idestadopedido, fechapedido, "
					+ "usuariopedido, total_bruto, impuesto, descuento, total_neto, tiempopedido, origen, programado, hora_programado, idtipopedido, enviadopixel, fechafinalizacion) "
					+ "values (?,?,?,current_date(),?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP())";

			try (PreparedStatement ps = con.prepareStatement(sqlInsPed, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, idTienda);
				ps.setInt(2, idCliente);
				ps.setInt(3, idEstadoInicial);
				ps.setString(4, usuario);
				ps.setDouble(5, totalBruto);
				ps.setDouble(6, valorImpuesto);
				ps.setDouble(7, res.descuento);
				ps.setDouble(8, totalNeto);
				ps.setDouble(9, tiempoPedido);
				ps.setString(10, origen);
				ps.setString(11, solicitud.contexto.programado ? "S" : "N");
				ps.setString(12, solicitud.contexto.horaProgramado != null ? solicitud.contexto.horaProgramado : "");
				ps.setInt(13, solicitud.contexto.idtipopedido > 0 ? solicitud.contexto.idtipopedido : 1);
				ps.setInt(14, enviadoPixelInicial);
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

			// 2.1 Registrar forma de pago en pizzaamericana.pedido_forma_pago (requerido para visualizacion en pantallas y reportes)
			String sqlInsFP = "insert into pizzaamericana.pedido_forma_pago (idpedido, idforma_pago, valortotal, valorformapago) "
					+ "values (?, ?, ?, ?)";
			try (PreparedStatement psFP = con.prepareStatement(sqlInsFP)) {
				psFP.setInt(1, idPedidoCentral);
				psFP.setInt(2, idFormaPagoTienda > 0 ? idFormaPagoTienda : 1);
				psFP.setDouble(3, res.total);
				psFP.setDouble(4, (!esFormaPagoVirtual && idFormaPagoTienda == 1 && valorFormaPago > 0) ? valorFormaPago : res.total);
				psFP.executeUpdate();
			} catch (Exception exFP) {
				System.err.println("Advertencia insertando pedido_forma_pago para pedido " + idPedidoCentral + ": " + exFP.getMessage());
			}

			// 3. Proyectar en pos_central y pizzaamericana.detalle_pedido
			int idPedidoPosGenerado = ProyectorPedidoLegacy.proyectarPedidoRetornandoIdPos(con, idPedidoCentral, idTienda, idCliente, solicitud, res, usuario);

			// 3.1 Registrar forma de pago en pos_central.pedido_forma_pago
			if (idPedidoPosGenerado > 0) {
				String sqlInsFPPos = "insert into pos_central.pedido_forma_pago (idpedidotienda, idforma_pago, valortotal, valorformapago) "
						+ "values (?, ?, ?, ?)";
				try (PreparedStatement psFPPos = con.prepareStatement(sqlInsFPPos)) {
					psFPPos.setInt(1, idPedidoPosGenerado);
					psFPPos.setInt(2, idFormaPagoTienda > 0 ? idFormaPagoTienda : 1);
					psFPPos.setDouble(3, res.total);
					psFPPos.setDouble(4, (!esFormaPagoVirtual && idFormaPagoTienda == 1 && valorFormaPago > 0) ? valorFormaPago : res.total);
					psFPPos.executeUpdate();
				} catch (Exception exFPPos) {
					System.err.println("Advertencia insertando pos_central.pedido_forma_pago: " + exFPPos.getMessage());
				}
			}

			// 4. Extraer datos de trazabilidad, plataforma y contingencia operativa
			String idPedidoAlt = root.has("idPedidoAlt") ? root.get("idPedidoAlt").getAsString() : "";
			String plataforma = root.has("plataforma") ? root.get("plataforma").getAsString() : "";
			if (root.has("marcacion")) {
				try {
					JsonObject marcJson = root.getAsJsonObject("marcacion");
					if (marcJson != null && marcJson.has("nombremarcacion")) {
						plataforma = marcJson.get("nombremarcacion").getAsString();
						String obsMarc = marcJson.has("observacion") ? marcJson.get("observacion").getAsString() : "";
						if (obsMarc != null && !obsMarc.trim().isEmpty()) {
							idPedidoAlt = obsMarc.trim();
						}
					}
				} catch (Exception ignorada) {
				}
			}

			String altDespacho = "";
			if (idPedidoAlt != null && !idPedidoAlt.trim().isEmpty()) {
				altDespacho = (plataforma != null && !plataforma.trim().isEmpty() ? (plataforma + "-" + idPedidoAlt) : idPedidoAlt)
						+ " [NM-" + idPedidoPosGenerado + "]";
			} else {
				altDespacho = "NM-" + idPedidoPosGenerado;
			}
			String origenDespacho = (origen != null && !origen.trim().isEmpty() && !origen.equals("C")) ? origen : "CC-NUEVO";

			// 5. Bifurcación: ¿Pago Virtual o Despacho Directo a Tienda?
			if (esFormaPagoVirtual) {
				// FLUJO PAGO VIRTUAL: Generar link de pago Wompi, enviar por WhatsApp y correo, y dejar en espera de pago.
				// NO SE DESPACHA A TIENDA hasta que el cliente pague.
				JSONObject respWompi = null;
				try {
					PedidoCtrl pedCtrl = new PedidoCtrl();
					if (cli == null) {
						cli = ClienteDAO.obtenerClienteporID(idCliente);
					}
					if (cli != null) {
						if (cli.getTelefonoCelular() == null || cli.getTelefonoCelular().trim().isEmpty()) {
							if (root.has("cliente") && root.getAsJsonObject("cliente").has("telefono")) {
								cli.setTelefonoCelular(root.getAsJsonObject("cliente").get("telefono").getAsString().trim());
							}
						}
						if (cli.getEmail() == null || cli.getEmail().trim().isEmpty()) {
							if (root.has("cliente") && root.getAsJsonObject("cliente").has("email")) {
								cli.setEmail(root.getAsJsonObject("cliente").get("email").getAsString().trim());
							}
						}
					}

					respWompi = pedCtrl.verificarEnvioLinkPagosParametrico(idPedidoCentral, cli, res.total, idTienda, "CONTACT");
				} catch (Exception exWompi) {
					System.err.println("Error procesando pago virtual Wompi para pedido " + idPedidoCentral + ": " + exWompi.getMessage());
					exWompi.printStackTrace();
				}

				boolean wompiSuccess = (respWompi != null && Boolean.TRUE.equals(respWompi.get("success")));
				String urlPago = (respWompi != null && respWompi.get("urlPago") != null) ? respWompi.get("urlPago").toString() : "";
				String idLinkGenerado = (respWompi != null && respWompi.get("idLink") != null) ? respWompi.get("idLink").toString() : "";
				if ((idLinkGenerado == null || idLinkGenerado.isEmpty()) && urlPago != null && urlPago.contains("/")) {
					idLinkGenerado = urlPago.substring(urlPago.lastIndexOf("/") + 1);
				}

				// Asegurar que pedido en BD tenga el idlink guardado
				if (idLinkGenerado != null && !idLinkGenerado.isEmpty()) {
					try (PreparedStatement psUpdPed = con.prepareStatement(
							"update pizzaamericana.pedido set idlink = ? where idpedido = ?")) {
						psUpdPed.setString(1, idLinkGenerado);
						psUpdPed.setInt(2, idPedidoCentral);
						psUpdPed.executeUpdate();
					} catch (Exception exIdL) {
					}
				}

				Map<String, Object> respFinal = new HashMap<String, Object>();
				respFinal.put("exito", true);
				respFinal.put("idPedido", idPedidoCentral);
				respFinal.put("idPedidoPos", idPedidoPosGenerado);
				respFinal.put("idPedidoAlt", altDespacho);
				respFinal.put("total", res.total);
				respFinal.put("descuento", res.descuento);
				respFinal.put("subtotal", res.subtotal);
				respFinal.put("pagoVirtual", true);
				respFinal.put("wompiSuccess", wompiSuccess);
				respFinal.put("urlPago", urlPago);
				respFinal.put("idLink", idLinkGenerado);
				respFinal.put("mensaje", wompiSuccess
						? ("Pedido #" + idPedidoCentral + " registrado con Pago Virtual. Link Wompi generado y enviado por WhatsApp y correo. Pendiente de pago.")
						: ("Pedido #" + idPedidoCentral + " registrado con Pago Virtual. Atención Wompi: "
								+ (respWompi != null ? respWompi.get("mensaje") : "Inconveniente al generar link")
								+ ". Puede gestionarlo en Monitoreo de Pagos Virtuales."));
				respFinal.put("resultadoMotor", res);

				response.setStatus(HttpServletResponse.SC_OK);
				out.write(GSON.toJson(respFinal));
				return;
			}

			// FLUJO NORMAL (Efectivo / Datáfono): Despacho directo a la tienda
			boolean esPiloto = EnrutadorDespachoTienda.esTiendaPiloto(con, idTienda);
			ConectorTiendaPos.DespachoResultado dr = null;

			if (esPiloto) {
				if (root.has("contingenciaTienda")) {
					try {
						JsonObject contJson = root.getAsJsonObject("contingenciaTienda");
						String origTienda = contJson.has("tiendaCoberturaOriginal") ? contJson.get("tiendaCoberturaOriginal").getAsString() : "";
						if (cli != null && origTienda != null && !origTienda.trim().isEmpty()) {
							String obsAct = cli.getObservacion() != null ? cli.getObservacion() : "";
							cli.setObservacion(("[CONTINGENCIA: Cobertura " + origTienda + "] " + obsAct).trim());
						}
					} catch (Exception ignorada) {
					}
				}

				dr = ConectorTiendaPos.despachar(con, idPedidoCentral, idTienda, cli, solicitud, res, idFormaPagoTienda,
						valorFormaPago, usuario, tiempoPedido, origenDespacho, altDespacho, plataforma, idPedidoPosGenerado);
			}

			Map<String, Object> respFinal = new HashMap<String, Object>();
			respFinal.put("exito", true);
			respFinal.put("idPedido", idPedidoCentral);
			respFinal.put("idPedidoPos", idPedidoPosGenerado);
			respFinal.put("idPedidoAlt", altDespacho);
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
