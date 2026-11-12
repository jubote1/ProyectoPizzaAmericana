package capaControladorCC.motor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import capaModeloCC.Cliente;
import capaModeloCC.motor.MotorResultado;
import capaModeloCC.motor.MotorSolicitud;

/**
 * Conector de Despacho Directo a Tienda (Etapa 3 - Piloto Manrique).
 *
 * Envia el pedido cotizado por el motor nuevo directamente al servicio FinalizarPedidoPixel
 * de la tienda seleccionada, utilizando los codigos de producto nativos de pos_central y
 * su jerarquia maestra/hijas (POS = 1), ELIMINANDO la tabla de homologacion (7.801 filas).
 */
public class ConectorTiendaPos {

	private static final Gson GSON = new Gson();
	private static final int TIMEOUT_CONEXION_MS = 12_000;
	private static final int TIMEOUT_LECTURA_MS = 25_000;

	public static class DespachoResultado {
		public boolean exito = false;
		public int numeroFactura = 0;
		public int memberCode = 0;
		public boolean creoCliente = false;
		public String mensaje = "";
		public String respuestaCruda = "";
		public int codigoHttp = 0;
	}

	/**
	 * Construye el payload JSON nativo para POS Pizza Americana (POS = 1) y lo envia a la tienda.
	 */
	public static DespachoResultado despachar(Connection con, int idPedidoCentral, int idTienda, Cliente cliente,
			MotorSolicitud solicitud, MotorResultado resultado, int idFormaPagoTienda, double valorFormaPago,
			String usuario, double tiempoPedido, String origen) {

		DespachoResultado dr = new DespachoResultado();

		try {
			// 1. Obtener URL y configuracion de la tienda
			String urlTienda = "";
			String dsnTienda = "PixelServicio";
			int pos = 1;

			String sqlT = "select url, dsn, pos from pizzaamericana.tienda where idtienda = ?";
			try (PreparedStatement ps = con.prepareStatement(sqlT)) {
				ps.setInt(1, idTienda);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						urlTienda = rs.getString("url");
						dsnTienda = rs.getString("dsn");
						pos = rs.getInt("pos");
					} else {
						dr.mensaje = "Tienda " + idTienda + " no encontrada.";
						return dr;
					}
				}
			}

			if (urlTienda == null || urlTienda.trim().isEmpty()) {
				dr.mensaje = "La tienda " + idTienda + " no tiene URL configurada.";
				return dr;
			}
			urlTienda = urlTienda.trim();
			if (!urlTienda.endsWith("/")) {
				urlTienda += "/";
			}

			// 2. Construir objeto Cliente en formato JSON para la tienda
			Map<String, Object> mapCliente = new HashMap<String, Object>();
			mapCliente.put("idcliente", cliente != null ? cliente.getIdcliente() : 0);
			mapCliente.put("memcode", cliente != null ? cliente.getMemcode() : 0);
			mapCliente.put("nombres", cliente != null && cliente.getNombres() != null ? cliente.getNombres() : "");
			mapCliente.put("apellidos", cliente != null && cliente.getApellidos() != null ? cliente.getApellidos() : "");
			mapCliente.put("nombrecompania",
					cliente != null && cliente.getNombreCompania() != null ? cliente.getNombreCompania() : "");
			mapCliente.put("direccion", cliente != null && cliente.getDireccion() != null ? cliente.getDireccion() : "");
			mapCliente.put("telefono", cliente != null && cliente.getTelefono() != null ? cliente.getTelefono() : "");
			mapCliente.put("telefonocelular",
					cliente != null && cliente.getTelefonoCelular() != null ? cliente.getTelefonoCelular() : "");
			mapCliente.put("email", cliente != null && cliente.getEmail() != null ? cliente.getEmail() : "");
			mapCliente.put("municipio", cliente != null && cliente.getMunicipio() != null ? cliente.getMunicipio() : "");
			mapCliente.put("idmunicipio", cliente != null ? cliente.getIdMunicipio() : 0);
			mapCliente.put("latitud", cliente != null ? cliente.getLatitud() : 0.0f);
			mapCliente.put("longitud", cliente != null ? cliente.getLontitud() : 0.0f);
			mapCliente.put("distanciatienda", cliente != null ? cliente.getDistanciaTienda() : 0.0);
			mapCliente.put("zonadireccion",
					cliente != null && cliente.getZonaDireccion() != null ? cliente.getZonaDireccion() : "");
			mapCliente.put("observacion", cliente != null && cliente.getObservacion() != null ? cliente.getObservacion() : "");
			mapCliente.put("idnomenclatura", cliente != null ? cliente.getIdnomenclatura() : 0);
			mapCliente.put("nomenclatura",
					cliente != null && cliente.getNomenclatura() != null ? cliente.getNomenclatura() : "");
			mapCliente.put("numnomenclatura",
					cliente != null && cliente.getNumNomenclatura() != null ? cliente.getNumNomenclatura() : "");
			mapCliente.put("numnomenclatura2",
					cliente != null && cliente.getNumNomenclatura2() != null ? cliente.getNumNomenclatura2() : "");
			mapCliente.put("num3", cliente != null && cliente.getNum3() != null ? cliente.getNum3() : "");
			mapCliente.put("politicadatos",
					cliente != null && cliente.getPoliticaDatos() != null ? cliente.getPoliticaDatos() : "S");
			mapCliente.put("emailfact",
					cliente != null && cliente.getEmailFacturacion() != null ? cliente.getEmailFacturacion() : "");
			mapCliente.put("clientesiniden",
					cliente != null && cliente.getClienteSinIden() != null ? cliente.getClienteSinIden() : "S");
			mapCliente.put("idtipopersona", (cliente != null && cliente.getIdTipoPersona() > 0) ? cliente.getIdTipoPersona() : 2);
			mapCliente.put("identificacion",
					cliente != null && cliente.getIdentificacion() != null ? cliente.getIdentificacion() : "");

			List<Map<String, Object>> arrayCliente = new ArrayList<Map<String, Object>>();
			arrayCliente.add(mapCliente);

			// 3. Construir JSON principal del pedido para la tienda
			Map<String, Object> mapPedido = new HashMap<String, Object>();
			mapPedido.put("idpedido", idPedidoCentral);
			mapPedido.put("valortotal", resultado.total);
			mapPedido.put("descuento", resultado.descuento);

			String motivoDesc = "";
			if (solicitud != null && solicitud.descuentos != null && solicitud.descuentos.motivoDescuento != null
					&& !solicitud.descuentos.motivoDescuento.trim().isEmpty()) {
				motivoDesc = solicitud.descuentos.motivoDescuento.trim();
			} else if (resultado.descuento > 0) {
				motivoDesc = "DESCUENTO MOTOR";
			}
			mapPedido.put("motivodescuento", motivoDesc);

			double reintegroVal = 0.0;
			if (idFormaPagoTienda == 1 && valorFormaPago > resultado.total) {
				reintegroVal = valorFormaPago - resultado.total;
			}
			mapPedido.put("reintegro", reintegroVal > 0 ? String.valueOf((long) reintegroVal) : "");
			mapPedido.put("insertado", "true");
			mapPedido.put("dsntienda", dsnTienda);
			mapPedido.put("pos", pos);
			mapPedido.put("memcode", cliente != null ? cliente.getMemcode() : 0);
			mapPedido.put("indicadoract", cliente == null || cliente.getMemcode() == 0);
			mapPedido.put("valorformapago", (idFormaPagoTienda == 1 && valorFormaPago > 0) ? valorFormaPago : resultado.total);
			mapPedido.put("idformapagotienda", idFormaPagoTienda > 0 ? idFormaPagoTienda : 1);
			mapPedido.put("usuariopedido", usuario != null && !usuario.trim().isEmpty() ? usuario : "MOTOR");
			mapPedido.put("tiempopedido", (int) tiempoPedido);
			mapPedido.put("origen", origen != null && !origen.trim().isEmpty() ? origen : "C");
			mapPedido.put("programado",
					solicitud.contexto != null && solicitud.contexto.programado ? "S" : "N");
			mapPedido.put("horaprogramado",
					solicitud.contexto != null && solicitud.contexto.horaProgramado != null
							? solicitud.contexto.horaProgramado
							: "");
			mapPedido.put("idtipopedido", solicitud.contexto != null ? solicitud.contexto.idtipopedido : 1);
			mapPedido.put("plataforma", "");
			mapPedido.put("idpedidoalt", "");
			mapPedido.put("domiciliotercerizado", "N");
			mapPedido.put("empresatercerizada", "");
			mapPedido.put("cliente", GSON.toJson(arrayCliente));

			// 4. Agregar cada linea de MotorResultado indexada 1..N
			int contador = 1;
			for (MotorResultado.Linea l : resultado.lineas) {
				mapPedido.put("idproductoext" + contador, l.idproducto);
				mapPedido.put("cantidad" + contador, l.cantidad);
				mapPedido.put("esmaster" + contador, l.refMaster == 0);
				mapPedido.put("idmaster" + contador, l.refMaster);
				mapPedido.put("idmodificador" + contador, l.refModificador);
				mapPedido.put("iddetalle" + contador, l.ref);
				mapPedido.put("valor" + contador, l.valorUnitario);
				contador++;
			}
			int totalItems = contador - 1;
			mapPedido.put("cantidaditempedido", totalItems);

			String payloadJson = GSON.toJson(mapPedido);

			// 5. Enviar HTTP POST a FinalizarPedidoPixel
			String endpoint = urlTienda + "FinalizarPedidoPixel";
			String postData = "datos=" + URLEncoder.encode(payloadJson, "UTF-8");

			URL url = new URL(endpoint);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			http.setConnectTimeout(TIMEOUT_CONEXION_MS);
			http.setReadTimeout(TIMEOUT_LECTURA_MS);
			http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			http.setRequestProperty("Accept", "application/json");

			try (OutputStream os = http.getOutputStream()) {
				os.write(postData.getBytes(StandardCharsets.UTF_8));
			}

			dr.codigoHttp = http.getResponseCode();
			StringBuilder respSb = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
					(dr.codigoHttp >= 200 && dr.codigoHttp < 300) ? http.getInputStream() : http.getErrorStream(),
					StandardCharsets.UTF_8))) {
				String l;
				while ((l = br.readLine()) != null) {
					respSb.append(l);
				}
			}
			http.disconnect();

			dr.respuestaCruda = respSb.toString().trim();

			// 6. Procesar respuesta de la tienda
			if (dr.codigoHttp >= 200 && dr.codigoHttp < 300 && !dr.respuestaCruda.isEmpty()) {
				JsonElement parsed = JsonParser.parseString(dr.respuestaCruda);
				if (parsed.isJsonArray() && parsed.getAsJsonArray().size() > 0) {
					JsonObject itemResp = parsed.getAsJsonArray().get(0).getAsJsonObject();
					dr.numeroFactura = itemResp.has("numerofactura") ? itemResp.get("numerofactura").getAsInt() : 0;
					dr.memberCode = itemResp.has("membercode") ? itemResp.get("membercode").getAsInt() : 0;
					dr.creoCliente = itemResp.has("creacliente") && itemResp.get("creacliente").getAsBoolean();

					if (dr.numeroFactura > 0) {
						dr.exito = true;
						dr.mensaje = "Pedido insertado exitosamente en tienda con comanda #" + dr.numeroFactura;

						// Actualizar estado en central: enviadopixel = 1, numposheader = numeroFactura
						actualizarEstadoCentral(con, idPedidoCentral, dr.numeroFactura);
					} else {
						dr.exito = false;
						dr.mensaje = "La tienda respondio pero no genero numero de factura/comanda.";
					}
				} else {
					dr.exito = false;
					dr.mensaje = "Respuesta no esperada de la tienda: " + dr.respuestaCruda;
				}
			} else {
				dr.exito = false;
				dr.mensaje = "Error HTTP " + dr.codigoHttp + " comunicandose con tienda: " + dr.respuestaCruda;
			}

		} catch (Throwable t) {
			dr.exito = false;
			dr.mensaje = "Excepcion en despacho directo a tienda: " + t.getMessage();
		}

		return dr;
	}

	private static void actualizarEstadoCentral(Connection con, int idPedidoCentral, int numFactura) {
		String sql = "update pizzaamericana.pedido set enviadopixel = 1, numposheader = ?, fechaenviotienda = current_timestamp where idpedido = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, numFactura);
			ps.setInt(2, idPedidoCentral);
			ps.executeUpdate();
		} catch (Exception ignorada) {
		}
	}
}
