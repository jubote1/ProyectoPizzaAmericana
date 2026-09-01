package capaControladorCC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;

import capaDAOCC.EspecialidadDAO;
import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.LogPedidoVirtualKunoDAO;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.ProductoDAO;
import capaDAOCC.SaborTipoLiquidoDAO;
import capaModeloCC.ExcepcionPrecio;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.Producto;
import capaModeloCC.ProductoIncluido;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Controlador encargado de construir la CONFIRMACIÓN de un pedido del CRM BOT
 * ANTES de registrarlo/insertarlo en el sistema.
 *
 * <p>
 * Toma los campos del lead en Kommo (los mismos que usa {@code PedidoCtrl.procesarPedidoBOTCRM}),
 * resuelve cada producto con sus precios reales, especialidades, modificadores,
 * adiciones, bebidas, acompañantes, productos incluidos y el valor del domicilio,
 * calculando el subtotal y total a pagar.
 * </p>
 *
 * <p>
 * <b>SOLO LECTURA:</b> No inserta encabezado de pedido, detalle de pedido ni cliente
 * en la base de datos de pedidos. Se puede invocar cuantas veces el bot lo requiera
 * sin generar pedidos basura.
 * </p>
 *
 * <p>
 * Además, actualiza el campo de texto de confirmación en el lead de Kommo (para que
 * el bot lo muestre al cliente) y retorna la respuesta en formato JSON.
 * </p>
 */
public class ConfirmacionPedidoCRMBOTCtrl {

	/**
	 * ID del campo personalizado en Kommo donde se guarda el mensaje completo
	 * de confirmación del pedido ("CONFIRMACION_PD", tipo textarea).
	 */
	public static final int FIELD_CONFIRMACION = 876709;

	/** Tipo de log usado para este servicio en log_pedido_virtual_kuno. */
	private static final String TIPO_LOG = "CF";

	private static final DecimalFormat FORMATO_MONEDA = construirFormatoMoneda();

	private static DecimalFormat construirFormatoMoneda() {
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
		simbolos.setGroupingSeparator('.');
		return new DecimalFormat("$#,##0", simbolos);
	}

	/**
	 * Método invocado desde el Servlet al recibir el webhook del CRM.
	 * Obtiene la información del lead, calcula la cotización sin insertar pedido,
	 * actualiza el lead en Kommo y retorna el JSON con el desglose.
	 *
	 * @param datos      cuerpo del webhook enviado por Kommo (form urlencoded o JSON)
	 * @param authHeader encabezado Authorization recibido
	 * @return JSON con el detalle valorizado, el total y el mensaje para el cliente
	 */
	public String confirmarPedidoCRMBOT(String datos, String authHeader) throws IOException {

		JSONObject respuesta = new JSONObject();

		try {
			String parametrosDecode = java.net.URLDecoder.decode(datos, StandardCharsets.UTF_8.name());

			Map parSep = PedidoCtrl.separarURL(parametrosDecode);

			String lead = (String) parSep.get("leads[status][0][id]");

			if (lead == null || lead.trim().isEmpty()) {
				// Intentar buscar "id" directo si llegó como JSON plano
				try {
					JSONParser parser = new JSONParser();
					JSONObject jsonDirecto = (JSONObject) parser.parse(datos);
					if (jsonDirecto.containsKey("id")) {
						lead = String.valueOf(jsonDirecto.get("id"));
					} else if (jsonDirecto.containsKey("lead_id")) {
						lead = String.valueOf(jsonDirecto.get("lead_id"));
					}
				} catch (Exception ignored) {
				}
			}

			if (lead == null || lead.trim().isEmpty()) {
				System.out.println("CONFIRMACION CRMBOT: lead inválido");
				respuesta.put("success", Boolean.FALSE);
				respuesta.put("mensaje", "No fue posible identificar el lead del pedido.");
				return respuesta.toJSONString();
			}

			lead = lead.trim();

			int leadNum;
			try {
				leadNum = Integer.parseInt(lead);
			} catch (NumberFormatException e) {
				System.out.println("CONFIRMACION CRMBOT: lead no numérico " + lead);
				respuesta.put("success", Boolean.FALSE);
				respuesta.put("mensaje", "No fue posible identificar el lead del pedido.");
				return respuesta.toJSONString();
			}

			int idLog = LogPedidoVirtualKunoDAO.insertarLogCRMBOT(datos, authHeader, TIPO_LOG, leadNum);

			PedidoCtrl pedidoCtrl = new PedidoCtrl();
			String infLead = pedidoCtrl.obtenerInformacionLeadCRM(lead);

			LogPedidoVirtualKunoDAO.actualizarLogCRMBOT(idLog, infLead, lead, TIPO_LOG);

			// Valorización del pedido, SIN insertar nada en BD
			respuesta = cotizarPedidoBOTCRM(infLead);
			respuesta.put("lead", lead);

			// Guardar el resumen en el log de BD para auditoría
			LogPedidoVirtualKunoDAO.actualizarLogCRMBOTInfLog(idLog, (String) respuesta.get("mensaje"));

			// Actualizar el lead en Kommo CRM con el texto de confirmación
			actualizarConfirmacionLeadCRMBOT(lead, respuesta);

		} catch (Exception e) {
			System.out.println("Error en confirmarPedidoCRMBOT: " + e.toString());
			e.printStackTrace();
			respuesta.put("success", Boolean.FALSE);
			respuesta.put("mensaje", "No fue posible calcular el pedido. Por favor comunícate con un asesor.");
		}

		return respuesta.toJSONString();
	}

	/**
	 * Toma la información del lead (JSON) y extrae todos los campos
	 * para armar el pedido valorizado.
	 *
	 * @param datosJSON información del lead entregada por el CRM
	 * @return JSON con encabezado, items valorizados, domicilio, subtotal y total
	 */
	public JSONObject cotizarPedidoBOTCRM(String datosJSON) {

		JSONObject resultado = new JSONObject();
		JSONArray items = new JSONArray();

		List<String> observaciones = new ArrayList<String>();

		// Datos del cliente y entrega
		String nombreCliente = "";
		String telefono = "";
		String direccion = "";
		String referencia = "";
		String barrio = "";
		String municipio = "";
		String tienda = "";
		String formaPago = "";
		String horaPedido = "";
		String fechaProgramado = "";
		String condimentos = "";
		String balon = "";
		String cantidadPizzas = "";

		// Primer producto / Combo 1
		String nombreDelCombo = "";
		String detalles = "";
		String sabor1 = "";
		String sabor2 = "";
		String sabor3 = "";
		String adicion = "";
		String bebida = "";
		String acompanamiento = "";
		String bebida2 = "";

		// Segundo producto / Combo 2
		String nombreDelCombo_2 = "";
		String detalles_2 = "";
		String sabor1_2 = "";
		String sabor2_2 = "";
		String adicion_2 = "";
		String bebida_2 = "";
		String acompanamiento2 = "";

		try {
			JSONParser parser = new JSONParser();
			JSONParser parserFinal = new JSONParser();

			Object objParser = parser.parse(datosJSON);
			JSONObject jsonGeneral = (JSONObject) objParser;

			JSONArray customFieldsArray = new JSONArray();

			try {
				Object cf = jsonGeneral.get("custom_fields_values");
				if (cf instanceof JSONArray) {
					customFieldsArray = (JSONArray) cf;
				} else if (cf instanceof String) {
					customFieldsArray = (JSONArray) parser.parse((String) cf);
				}
			} catch (Exception e1) {
				observaciones.add("El lead no tiene los campos del pedido diligenciados.");
			}

			for (int i = 0; i < customFieldsArray.size(); i++) {

				JSONObject objTemp = (JSONObject) customFieldsArray.get(i);

				// Obtener ID numérico del campo
				int fieldId = 0;
				if (objTemp.get("field_id") != null) {
					try {
						fieldId = Integer.parseInt(objTemp.get("field_id").toString());
					} catch (Exception ignored) {
					}
				}

				String clave = objTemp.get("field_name") != null ? objTemp.get("field_name").toString().toLowerCase().trim() : "";

				JSONArray valuesArray = new JSONArray();
				Object rawValues = objTemp.get("values");
				if (rawValues instanceof JSONArray) {
					valuesArray = (JSONArray) rawValues;
				} else if (rawValues instanceof String) {
					valuesArray = (JSONArray) parser.parse((String) rawValues);
				}

				if (valuesArray.isEmpty()) {
					continue;
				}

				JSONObject valor = (JSONObject) parserFinal.parse(valuesArray.get(0).toString());
				String strValor = valor.get("value") != null ? valor.get("value").toString() : "";
				strValor = strValor.replaceAll("'", " ").trim();

				// Mapeo por ID de campo exacto (con fallback por nombre de campo)
				switch (fieldId) {
					case 868051: // #PIZZAS#
						cantidadPizzas = strValor;
						break;
					case 862087: // nombre del cliente
						nombreCliente = strValor;
						break;
					case 861855: // nombre del combo
						nombreDelCombo = strValor;
						break;
					case 862081: // detalles
						detalles = strValor.toLowerCase();
						break;
					case 857712: // sabor 1
						sabor1 = strValor;
						break;
					case 857714: // sabor 2
						sabor2 = strValor;
						break;
					case 868957: // sabor 3
						sabor3 = strValor;
						break;
					case 861771: // adicion
						adicion = strValor;
						break;
					case 861775: // Bebida
						bebida = strValor;
						break;
					case 861773: // acompañamiento
						acompanamiento = strValor;
						break;
					case 862083: // bebida 2
						bebida2 = strValor;
						break;
					case 867749: // condimento
						condimentos = strValor;
						break;
					case 868055: // nombre del combo # 2
						nombreDelCombo_2 = strValor;
						break;
					case 868057: // detalle # 2
						detalles_2 = strValor.toLowerCase();
						break;
					case 868059: // sabor 1 # 2
						sabor1_2 = strValor;
						break;
					case 868061: // sabor 2 # 2
						sabor2_2 = strValor;
						break;
					case 868063: // adicion # 2
						adicion_2 = strValor;
						break;
					case 868065: // bebida #2
						bebida_2 = strValor;
						break;
					case 873301: // acompañante #2
						acompanamiento2 = strValor;
						break;
					case 873233: // forma de pago
						formaPago = strValor;
						break;
					case 858274: // dirección de envío
						direccion = strValor;
						break;
					case 858276: // barrio
						barrio = strValor;
						break;
					case 863427: // municipio
						municipio = strValor;
						break;
					case 866919: // referencia
						referencia = strValor;
						break;
					case 862089: // numero de teléfono
						telefono = strValor;
						break;
					case 862153: // tienda
						tienda = strValor;
						break;
					case 872191: // balon mundial
						balon = strValor;
						break;
					default:
						// Fallback por nombre de campo
						if (clave.equals("forma de pago")) {
							if (formaPago.isEmpty()) formaPago = strValor;
						} else if (clave.equals("nombre cliente") || clave.equals("nombre del cliente")) {
							if (nombreCliente.isEmpty()) nombreCliente = strValor;
						} else if (clave.equals("numero de telefono") || clave.equals("numero de teléfono")) {
							if (telefono.isEmpty()) telefono = strValor;
						} else if (clave.equals("nombre del combo")) {
							if (nombreDelCombo.isEmpty()) nombreDelCombo = strValor;
						} else if (clave.equals("nombre del combo # 2") || clave.equals("nombre del combo #2") || clave.equals("nombre del combo  #2")) {
							if (nombreDelCombo_2.isEmpty()) nombreDelCombo_2 = strValor;
						} else if (clave.equals("detalles") || clave.equals("detalle")) {
							if (detalles.isEmpty()) detalles = strValor.toLowerCase();
						} else if (clave.equals("detalles #2") || clave.equals("detalle # 2") || clave.equals("detalle #2")) {
							if (detalles_2.isEmpty()) detalles_2 = strValor.toLowerCase();
						} else if (clave.equals("sabor 1")) {
							if (sabor1.isEmpty()) sabor1 = strValor;
						} else if (clave.equals("sabor 1 #2") || clave.equals("sabor 1 # 2")) {
							if (sabor1_2.isEmpty()) sabor1_2 = strValor;
						} else if (clave.equals("sabor 2")) {
							if (sabor2.isEmpty()) sabor2 = strValor;
						} else if (clave.equals("sabor 2 #2") || clave.equals("sabor 2 # 2")) {
							if (sabor2_2.isEmpty()) sabor2_2 = strValor;
						} else if (clave.equals("sabor 3")) {
							if (sabor3.isEmpty()) sabor3 = strValor;
						} else if (clave.equals("adicion") || clave.equals("adición")) {
							if (adicion.isEmpty()) adicion = strValor;
						} else if (clave.equals("adicion #2") || clave.equals("adicion # 2") || clave.equals("adición # 2")) {
							if (adicion_2.isEmpty()) adicion_2 = strValor;
						} else if (clave.equals("bebida")) {
							if (bebida.isEmpty()) bebida = strValor;
						} else if (clave.equals("bebida #2") || clave.equals("bebida # 2")) {
							if (bebida_2.isEmpty()) bebida_2 = strValor;
						} else if (clave.equals("acompañamiento") || clave.equals("acompanamiento")) {
							if (acompanamiento.isEmpty()) acompanamiento = strValor;
						} else if (clave.equals("acompañante #2") || clave.equals("acompañante # 2") || clave.equals("acompanante #2")) {
							if (acompanamiento2.isEmpty()) acompanamiento2 = strValor;
						} else if (clave.equals("bebida 2")) {
							if (bebida2.isEmpty()) bebida2 = strValor;
						} else if (clave.equals("forma de pago")) {
							if (formaPago.isEmpty()) formaPago = strValor;
						} else if (clave.equals("dirección de envío") || clave.equals("direccion de envio")) {
							if (direccion.isEmpty()) direccion = strValor;
						} else if (clave.equals("referencia")) {
							if (referencia.isEmpty()) referencia = strValor;
						} else if (clave.equals("barrio")) {
							if (barrio.isEmpty()) barrio = strValor;
						} else if (clave.equals("municipio")) {
							if (municipio.isEmpty()) municipio = strValor;
						} else if (clave.equals("tienda")) {
							if (tienda.isEmpty()) tienda = strValor;
						} else if (clave.equals("hora pedido")) {
							if (horaPedido.isEmpty()) horaPedido = strValor;
						} else if (clave.equals("fecha pedido")) {
							if (fechaProgramado.isEmpty()) fechaProgramado = strValor;
						} else if (clave.equals("condimentos") || clave.equals("condimento")) {
							if (condimentos.isEmpty()) condimentos = strValor;
						} else if (clave.equals("balon mundial")) {
							if (balon.isEmpty()) balon = strValor;
						} else if (clave.equals("#pizzas#") || clave.equals("pizzas")) {
							if (cantidadPizzas.isEmpty()) cantidadPizzas = strValor;
						}
						break;
				}
			}

		} catch (Exception e) {
			System.out.println("Error interpretando el lead en cotizarPedidoBOTCRM: " + e.toString());
			observaciones.add("No fue posible interpretar la información del pedido.");
		}

		if (condimentos != null && condimentos.equalsIgnoreCase("No desea")) {
			condimentos = "";
		}

		// El pedido del CRM BOT siempre es tipo domicilio
		int idTipoPedido = 1;
		double valorDomicilio = 0;

		ParametrosCtrl parCtrl = new ParametrosCtrl();

		// ------------------------------------------------------------------
		// 1. Domicilio
		// ------------------------------------------------------------------
		if (idTipoPedido == 1) {
			int idProductoDomicilio = parCtrl.homologarProductoTiendaVirtual("Valor del domicilio");
			if (idProductoDomicilio > 0) {
				Producto prodDomicilio = ProductoDAO.retornarProducto(idProductoDomicilio);
				valorDomicilio = prodDomicilio.getPreciogeneral();
			} else {
				valorDomicilio = 5000; // Valor por defecto de seguridad
			}
			agregarItem(items, "Servicio de Domicilio", 1, valorDomicilio, false, true);
		}

		// ------------------------------------------------------------------
		// 2. Condimentos (incluidos, $0)
		// ------------------------------------------------------------------
		if (condimentos != null && !condimentos.trim().isEmpty()) {
			agregarItem(items, "Condimentos: " + condimentos, 1, 0, true, false);
		}

		// ------------------------------------------------------------------
		// 3. Balón
		// ------------------------------------------------------------------
		if (balon != null && !balon.trim().isEmpty()) {
			int idProductoBalon = parCtrl.homologarProductoTiendaVirtual(balon);
			if (idProductoBalon > 0) {
				Producto prodBalon = ProductoDAO.retornarProducto(idProductoBalon);
				agregarItem(items, nombreVisible(prodBalon, balon), 1, prodBalon.getPreciogeneral(), false, false);
			}
		}

		// ------------------------------------------------------------------
		// 4. Primer producto / Combo 1
		// ------------------------------------------------------------------
		if (nombreDelCombo != null && !nombreDelCombo.trim().isEmpty()) {
			valorizarProducto(items, observaciones, parCtrl, nombreDelCombo, sabor1, sabor2, sabor3, adicion, bebida, detalles,
					acompanamiento, bebida2, true);
		}

		// ------------------------------------------------------------------
		// 5. Segundo producto / Combo 2 (si aplica)
		// ------------------------------------------------------------------
		boolean procesarSegundo = false;
		if ("2".equals(cantidadPizzas) || "dos".equalsIgnoreCase(cantidadPizzas)) {
			procesarSegundo = true;
		} else if (!nombreDelCombo_2.trim().isEmpty() && !"1".equals(cantidadPizzas)) {
			procesarSegundo = true;
		}

		if (procesarSegundo && !nombreDelCombo_2.trim().isEmpty()) {
			valorizarProducto(items, observaciones, parCtrl, nombreDelCombo_2, sabor1_2, sabor2_2, "", adicion_2, bebida_2,
					detalles_2, acompanamiento2, "", false);
		}

		// ------------------------------------------------------------------
		// 6. Totales
		// ------------------------------------------------------------------
		double total = 0;
		for (int i = 0; i < items.size(); i++) {
			JSONObject item = (JSONObject) items.get(i);
			total += ((Number) item.get("valorTotal")).doubleValue();
		}

		double subtotalProductos = total - valorDomicilio;

		resultado.put("success", Boolean.TRUE);
		resultado.put("cliente", nombreCliente);
		resultado.put("telefono", telefono);
		resultado.put("direccion", direccion);
		resultado.put("referencia", referencia);
		resultado.put("barrio", barrio);
		resultado.put("municipio", municipio);
		resultado.put("tienda", tienda);
		resultado.put("formaPago", formaPago);
		resultado.put("horaPedido", horaPedido);
		resultado.put("fechaPedido", fechaProgramado);
		resultado.put("items", items);
		resultado.put("subtotalProductos", Double.valueOf(subtotalProductos));
		resultado.put("valorDomicilio", Double.valueOf(valorDomicilio));
		resultado.put("total", Double.valueOf(total));

		JSONArray jsonObser = new JSONArray();
		jsonObser.addAll(observaciones);
		resultado.put("observaciones", jsonObser);

		resultado.put("mensaje", construirMensajeConfirmacion(resultado));

		return resultado;
	}

	/**
	 * Valoriza un producto del pedido (combo/pizza) con sus especialidades,
	 * modificadores, adiciones, bebida y acompañante, replicando paso a paso
	 * el cálculo de PedidoCtrl.
	 *
	 * @param esPrimerProducto indica si es el primer producto del pedido.
	 */
	private void valorizarProducto(JSONArray items, List<String> observaciones, ParametrosCtrl parCtrl,
			String nombreDelCombo, String sabor1, String sabor2, String sabor3, String adicion, String bebida, String detalle,
			String acompanamiento, String bebida2, boolean esPrimerProducto) {

		if (nombreDelCombo == null || nombreDelCombo.trim().isEmpty()) {
			return;
		}

		int cantidad = 1;
		int idEspecialidad = 0;
		int idEspecialidad2 = 0;
		int idEspecialidad3 = 0;
		boolean esPromocion = false;
		double valorUnitario = 0;
		double valorAdicional = 0;
		double valorAdicional2 = 0;
		double valorAdicional3 = 0;
		String strCON = "";

		ExcepcionPrecio excepcionPrecioTemp = parCtrl.homologarExcepcionTiendaVirtual(nombreDelCombo);
		int idExcepcion = excepcionPrecioTemp.getIdExcepcion();

		int idProducto;
		Producto prodBase = null;

		if (idExcepcion != 0) {
			idProducto = excepcionPrecioTemp.getIdProducto();
			valorUnitario = excepcionPrecioTemp.getPrecio();
			esPromocion = true;
			if (idProducto > 0) {
				prodBase = ProductoDAO.retornarProducto(idProducto);
			}
		} else {
			idProducto = parCtrl.homologarProductoTiendaVirtual(nombreDelCombo);
			if (idProducto > 0) {
				prodBase = ProductoDAO.retornarProducto(idProducto);
				valorUnitario = prodBase.getPreciogeneral();
			}
		}

		if (idProducto == 0) {
			observaciones.add("No fue posible homologar el producto " + nombreDelCombo + ".");
		}

		// ------------------------------------------------------------------
		// Nombre amigable de la pizza para el cliente (basado en la BD)
		// Si el nombre del combo es promocional/interno (ej: PROMO MEDIANA MAX, INSUPERABLE),
		// se presenta de forma natural según la descripción del producto en BD (ej: Pizza Mediana).
		// ------------------------------------------------------------------
		String nombreVisibleProducto = nombreDelCombo;
		if (prodBase != null && prodBase.getDescripcion() != null && !prodBase.getDescripcion().trim().isEmpty()) {
			String comboUpper = nombreDelCombo.toUpperCase();
			if (comboUpper.contains("INSUPERABLE") || comboUpper.contains("PROMO") || comboUpper.contains("COMBO")
					|| comboUpper.contains("FUTBOLERO") || comboUpper.contains("MAX")) {
				nombreVisibleProducto = "Pizza " + prodBase.getDescripcion();
			}
		}

		// ------------------------------------------------------------------
		// Especialidades / Sabores
		// ------------------------------------------------------------------
		ArrayList<String> modificadores = new ArrayList<String>();

		if (detalle == null || detalle.trim().isEmpty()) {
			// Caso Pizza Burger
			if (nombreDelCombo.toLowerCase().contains("burger")) {
				if (!sabor1.equals("con todo") && !sabor1.trim().isEmpty()) {
					StringTokenizer tokens = new StringTokenizer(sabor1, ",");
					while (tokens.hasMoreTokens()) {
						String sinMod = tokens.nextToken().trim();
						strCON = strCON + " SIN " + sinMod;
					}
				}
			}
		} else if (detalle.equals("una sola especialidad") || detalle.equals("pizza una sola especialidad")) {

			idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor1);
			idEspecialidad2 = idEspecialidad;

			if (!sabor1.trim().isEmpty()) {
				if (idEspecialidad == 0) {
					observaciones.add("No fue posible homologar el sabor Pizza " + sabor1 + ".");
				}
				modificadores.add("Pizza " + sabor1);
			}

		} else if (detalle.equals("mitad y mitad") || detalle.equals("pizza mitad y mitad")) {

			idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor1);
			idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor2);

			if (!sabor1.trim().isEmpty() && idEspecialidad == 0) {
				observaciones.add("No fue posible homologar el sabor Pizza " + sabor1 + ".");
			}
			if (!sabor2.trim().isEmpty() && idEspecialidad2 == 0) {
				observaciones.add("No fue posible homologar el sabor Pizza " + sabor2 + ".");
			}

			modificadores.add("Mitad " + sabor1 + " / Mitad " + sabor2);

		} else if (detalle.contains("3") || (sabor3 != null && !sabor3.trim().isEmpty())) {
			// Caso 3 sabores / especialidades
			idEspecialidad = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor1);
			idEspecialidad2 = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor2);
			idEspecialidad3 = parCtrl.homologarEspecialidadTiendaVirtual("Pizza " + sabor3);

			modificadores.add("1/3 " + sabor1 + " / 1/3 " + sabor2 + " / 1/3 " + sabor3);
		}

		// ------------------------------------------------------------------
		// Modificadores CON en BD (por producto_asocia_adicion sin quemar nombres)
		// ------------------------------------------------------------------
		if (esPromocion && idEspecialidad == 0) {

			Producto modCon1 = buscarModificadorConEnBD(idProducto, sabor1);
			if (modCon1 != null) {
				strCON = strCON + " CON " + sabor1;
				if (modCon1.getPreciogeneral() > 0) {
					agregarItem(items, "Adicional " + sabor1, cantidad, modCon1.getPreciogeneral(), false, false);
				}
			}

			Producto modCon2 = buscarModificadorConEnBD(idProducto, sabor2);
			if (modCon2 != null) {
				strCON = strCON + " CON " + sabor2;
				if (modCon2.getPreciogeneral() > 0) {
					agregarItem(items, "Adicional " + sabor2, cantidad, modCon2.getPreciogeneral(), false, false);
				}
			}
		}

		// ------------------------------------------------------------------
		// Valor adicional por especialidad (consultado en especialidad_excepcion)
		// En el menú, si una especialidad tiene un valor diferencial (ej: Pepperoni),
		// ese valor se integra al precio propio de la pizza. Para que el cliente
		// tenga total claridad de su valor, se genera una aclaración informativa.
		// ------------------------------------------------------------------
		double precioBasePizza = valorUnitario;
		ArrayList<String> recargosEspecialidad = new ArrayList<String>();

		if (idEspecialidad3 > 0) {
			valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto)) / 3;
			valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto)) / 3;
			valorAdicional3 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad3, idProducto)) / 3;
			if (valorAdicional > 0) {
				String nomEsp = nombreEspecialidadBD(idEspecialidad, sabor1);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional) + " por " + nomEsp);
			}
			if (valorAdicional2 > 0) {
				String nomEsp2 = nombreEspecialidadBD(idEspecialidad2, sabor2);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional2) + " por " + nomEsp2);
			}
			if (valorAdicional3 > 0) {
				String nomEsp3 = nombreEspecialidadBD(idEspecialidad3, sabor3);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional3) + " por " + nomEsp3);
			}
			valorUnitario = valorUnitario + valorAdicional + valorAdicional2 + valorAdicional3;
		} else if (idEspecialidad > 0 && idEspecialidad2 == 0) {
			valorAdicional = EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto);
			if (valorAdicional > 0) {
				String nomEsp = nombreEspecialidadBD(idEspecialidad, sabor1);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional) + " por " + nomEsp);
			}
			valorUnitario = valorUnitario + valorAdicional;
		} else if (idEspecialidad > 0 && idEspecialidad2 > 0) {
			valorAdicional = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad, idProducto)) / 2;
			valorAdicional2 = (EspecialidadDAO.obtenerPrecioExcepcionEspecialidad(idEspecialidad2, idProducto)) / 2;
			if (valorAdicional > 0) {
				String nomEsp = nombreEspecialidadBD(idEspecialidad, sabor1);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional) + " por " + nomEsp);
			}
			if (valorAdicional2 > 0) {
				String nomEsp2 = nombreEspecialidadBD(idEspecialidad2, sabor2);
				recargosEspecialidad.add("+" + FORMATO_MONEDA.format(valorAdicional2) + " por " + nomEsp2);
			}
			valorUnitario = valorUnitario + valorAdicional + valorAdicional2;
		}

		String aclaracionPizza = "";
		if (!recargosEspecialidad.isEmpty()) {
			StringBuilder sbAcl = new StringBuilder("Precio base ").append(FORMATO_MONEDA.format(precioBasePizza));
			for (String rec : recargosEspecialidad) {
				sbAcl.append(" ").append(rec);
			}
			aclaracionPizza = sbAcl.toString();
		}

		// ------------------------------------------------------------------
		// Producto principal: Pizza con sus sabores
		// ------------------------------------------------------------------
		StringBuilder descripcion = new StringBuilder(nombreVisibleProducto);

		for (int i = 0; i < modificadores.size(); i++) {
			descripcion.append(" - ").append(modificadores.get(i));
		}

		if (!strCON.trim().isEmpty()) {
			descripcion.append(" (").append(strCON.trim()).append(")");
		}

		agregarItem(items, descripcion.toString(), cantidad, valorUnitario, false, false, aclaracionPizza);

		// ------------------------------------------------------------------
		// Adiciones (soporta adición simple o separada por comas)
		// ------------------------------------------------------------------
		if (adicion != null && !adicion.trim().isEmpty()) {
			String[] listaAdiciones = adicion.split(",");
			for (String adiIndividual : listaAdiciones) {
				adiIndividual = adiIndividual.trim();
				if (!adiIndividual.isEmpty()) {
					int idProductoAdicion = parCtrl.homologarProductoTiendaVirtual(adiIndividual);
					double valorUnitarioAdicion = 0;
					if (idProductoAdicion > 0) {
						valorUnitarioAdicion = ProductoDAO.retornarProducto(idProductoAdicion).getPreciogeneral();
					} else {
						observaciones.add("No fue posible homologar la adición " + adiIndividual + ".");
					}
					agregarItem(items, "Adición " + adiIndividual, 1, valorUnitarioAdicion, false, false);
				}
			}
		}

		// ------------------------------------------------------------------
		// Bebida
		// ------------------------------------------------------------------
		if (bebida != null && !bebida.trim().isEmpty()) {
			int idSaborTipoLiquido;

			if (esPromocion) {
				idSaborTipoLiquido = parCtrl
						.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida + " Extragrande (12 porciones)");
			} else {
				idSaborTipoLiquido = parCtrl
						.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida + " " + nombreDelCombo);
			}

			if (idSaborTipoLiquido > 0) {
				int idProductoGas = SaborTipoLiquidoDAO.retornarProductoSaborTipoLiquido(idSaborTipoLiquido);
				Producto prodGas = ProductoDAO.retornarProducto(idProductoGas);

				boolean bebidaIncluida = (esPromocion && "S".equals(excepcionPrecioTemp.getIncluyeliquido()))
						|| nombreDelCombo.toLowerCase().contains("burger");

				if (bebidaIncluida) {
					agregarItem(items, nombreVisible(prodGas, "Bebida " + bebida), cantidad, 0, true, false);
				} else {
					agregarItem(items, nombreVisible(prodGas, "Bebida " + bebida), cantidad, prodGas.getPrecioOferta(), false, false);
				}
			}
		}

		// ------------------------------------------------------------------
		// Productos incluidos sin costo
		// ------------------------------------------------------------------
		agregarProductosIncluidos(items, idProducto, cantidad);

		// ------------------------------------------------------------------
		// Acompañante (consulta directa en BD sin listas quemadas)
		// ------------------------------------------------------------------
		if (acompanamiento != null && !acompanamiento.trim().isEmpty()) {

			int idProductoAcompa = parCtrl.homologarProductoTiendaVirtual("Producto Adicional " + acompanamiento);
			if (idProductoAcompa == 0) {
				idProductoAcompa = parCtrl.homologarProductoTiendaVirtual(acompanamiento);
			}

			if (idProductoAcompa != 0) {
				Producto prodAcompa = ProductoDAO.retornarProducto(idProductoAcompa);
				agregarItem(items, nombreVisible(prodAcompa, "Acompañante: " + acompanamiento), 1, prodAcompa.getPreciogeneral(), false, false);

				// Bebida del acompañante (solo para el primer combo)
				if (esPrimerProducto && bebida2 != null && !bebida2.trim().isEmpty()) {

					int idSaborTipoLiquido2 = parCtrl
							.homologarLiquidoTiendaVirtual("Selecciona tu bebida " + bebida2 + " Pizzeta (4 porciones)");

					if (idSaborTipoLiquido2 > 0) {
						int idProductoGas2 = SaborTipoLiquidoDAO.retornarProductoSaborTipoLiquido(idSaborTipoLiquido2);
						Producto prodGas2 = ProductoDAO.retornarProducto(idProductoGas2);
						agregarItem(items, nombreVisible(prodGas2, "Bebida Acompañante " + bebida2), 1, prodGas2.getPrecioOferta(), false, false);
					}

					agregarProductosIncluidos(items, idProductoAcompa, 1);
				}

			} else {
				observaciones.add("No fue posible homologar el acompañante " + acompanamiento + ".");
			}
		}
	}

	/**
	 * Busca en la base de datos el modificador CON para el sabor y la pizza base,
	 * consultando la tabla producto por producto_asocia_adicion y tipo = 'MODIFICADOR CON'.
	 * Evita cualquier código quemado de tamaños o nombres de combos.
	 */
	private Producto buscarModificadorConEnBD(int idProductoBase, String sabor) {
		if (idProductoBase <= 0 || sabor == null || sabor.trim().isEmpty()) {
			return null;
		}
		conexionCC.ConexionBaseDatos con = new conexionCC.ConexionBaseDatos();
		java.sql.Connection con1 = con.obtenerConexionBDPrincipal();
		Producto prodMod = null;
		try {
			java.sql.Statement stm = con1.createStatement();
			String sql = "select idproducto, nombre, descripcion, preciogeneral from producto "
					+ "where tipo = 'MODIFICADOR CON' and producto_asocia_adicion = " + idProductoBase
					+ " and (nombre like '%" + sabor.trim() + "%' or descripcion like '%" + sabor.trim() + "%') and habilitado = 'S' limit 1";
			java.sql.ResultSet rs = stm.executeQuery(sql);
			if (rs.next()) {
				int idMod = rs.getInt("idproducto");
				prodMod = ProductoDAO.retornarProducto(idMod);
			}
			rs.close();
			stm.close();
			con1.close();
		} catch (Exception e) {
			try {
				con1.close();
			} catch (Exception ignored) {
			}
		}
		return prodMod;
	}

	/**
	 * Agrega al detalle los productos que vienen incluidos ($0) de un producto padre.
	 */
	private void agregarProductosIncluidos(JSONArray items, int idProductoPadre, int cantidad) {

		if (idProductoPadre <= 0) {
			return;
		}

		ArrayList<ProductoIncluido> productosIncluidos = PedidoDAO.obtenerProductosIncluidos();

		for (int j = 0; j < productosIncluidos.size(); j++) {
			ProductoIncluido proIncTemp = productosIncluidos.get(j);

			if (proIncTemp.getIdproductopadre() == idProductoPadre) {
				Producto prodInc = ProductoDAO.retornarProducto(proIncTemp.getIdproductohijo());
				int cantFinal = (int) (proIncTemp.getCantidad() * cantidad);
				agregarItem(items, nombreVisible(prodInc, "Producto incluido"), cantFinal, 0, true, false);
			}
		}
	}

	/** Agrega una línea al detalle valorizado especificando si corresponde al domicilio. */
	private void agregarItem(JSONArray items, String descripcion, int cantidad, double valorUnitario,
			boolean incluido, boolean esDomicilio) {
		agregarItem(items, descripcion, cantidad, valorUnitario, incluido, esDomicilio, "");
	}

	/** Agrega una línea al detalle valorizado con aclaración informativa opcional. */
	private void agregarItem(JSONArray items, String descripcion, int cantidad, double valorUnitario,
			boolean incluido, boolean esDomicilio, String aclaracion) {

		JSONObject item = new JSONObject();
		item.put("descripcion", descripcion);
		item.put("cantidad", Integer.valueOf(cantidad));
		item.put("valorUnitario", Double.valueOf(valorUnitario));
		item.put("valorTotal", Double.valueOf(valorUnitario * cantidad));
		item.put("incluido", Boolean.valueOf(incluido));
		item.put("esDomicilio", Boolean.valueOf(esDomicilio));
		if (aclaracion != null && !aclaracion.trim().isEmpty()) {
			item.put("aclaracion", aclaracion);
		}

		items.add(item);
	}

	/**
	 * Obtiene el nombre de la especialidad desde la tabla especialidad en BD.
	 * Si en BD o en el texto recibido corresponde a Pepperoni, asegura que se muestre como 'Pepperoni'.
	 */
	private String nombreEspecialidadBD(int idEspecialidad, String fallback) {
		if (idEspecialidad > 0) {
			capaModeloCC.Especialidad esp = EspecialidadDAO.retornarEspecialidad(idEspecialidad);
			if (esp != null && esp.getNombre() != null && !esp.getNombre().trim().isEmpty()) {
				String nom = esp.getNombre().trim();
				if (nom.toLowerCase().contains("peperoni") || nom.toLowerCase().contains("pepperoni")) {
					return "Pepperoni";
				}
				return nom;
			}
		}
		if (fallback != null) {
			if (fallback.toLowerCase().contains("peperoni") || fallback.toLowerCase().contains("pepperoni")) {
				return "Pepperoni";
			}
			return fallback.trim();
		}
		return "";
	}

	/**
	 * Devuelve el nombre del producto en base de datos o el texto alternativo
	 * recibido del bot si no se logró consultar.
	 */
	private String nombreVisible(Producto producto, String alternativo) {
		if (producto != null && producto.getNombre() != null && !producto.getNombre().trim().isEmpty()) {
			return producto.getNombre();
		}
		return (alternativo == null) ? "" : alternativo;
	}

	/**
	 * Construye el mensaje estructurado con formato estándar de WhatsApp
	 * para que el chatbot se lo muestre al cliente.
	 *
	 * <p>Incluye el saludo con el nombre del cliente y el detalle limpio
	 * de productos, aclaraciones de precio por especialidad, domicilio y total a pagar.</p>
	 */
	public String construirMensajeConfirmacion(JSONObject cotizacion) {

		StringBuilder mensaje = new StringBuilder();

		String cliente = (String) cotizacion.get("cliente");

		mensaje.append("🍕 *RESUMEN DE TU PEDIDO* 🍕\n\n");

		if (cliente != null && !cliente.trim().isEmpty()) {
			mensaje.append("Hola *").append(cliente).append("*, por favor revisa el detalle de tu pedido antes de confirmarlo:\n\n");
		} else {
			mensaje.append("Por favor revisa el detalle de tu pedido antes de confirmarlo:\n\n");
		}

		mensaje.append("📋 *PRODUCTOS:*\n");

		JSONArray items = (JSONArray) cotizacion.get("items");

		for (int i = 0; i < items.size(); i++) {

			JSONObject item = (JSONObject) items.get(i);
			boolean esDomicilio = Boolean.TRUE.equals(item.get("esDomicilio"));
			if (esDomicilio) {
				continue; // Se muestra en la sección de DETALLE DE PAGO
			}

			int cantidad = ((Number) item.get("cantidad")).intValue();
			double valorTotal = ((Number) item.get("valorTotal")).doubleValue();
			boolean incluido = Boolean.TRUE.equals(item.get("incluido"));

			mensaje.append("• ").append(cantidad).append(" x ").append(item.get("descripcion")).append(" : ");

			if (incluido || valorTotal == 0) {
				mensaje.append("_(Incluido)_");
			} else {
				mensaje.append("*").append(FORMATO_MONEDA.format(valorTotal)).append("*");
			}

			mensaje.append("\n");

			String aclaracion = (String) item.get("aclaracion");
			if (aclaracion != null && !aclaracion.trim().isEmpty()) {
				mensaje.append("  _(").append(aclaracion).append(")_\n");
			}
		}

		double valorDomicilio = ((Number) cotizacion.get("valorDomicilio")).doubleValue();
		double subtotal = ((Number) cotizacion.get("subtotalProductos")).doubleValue();
		double total = ((Number) cotizacion.get("total")).doubleValue();

		mensaje.append("\n💳 *DETALLE DE PAGO:*\n");
		mensaje.append("• Subtotal productos: ").append(FORMATO_MONEDA.format(subtotal)).append("\n");
		mensaje.append("• Domicilio: ").append(FORMATO_MONEDA.format(valorDomicilio)).append("\n");
		mensaje.append("💰 *TOTAL A PAGAR: ").append(FORMATO_MONEDA.format(total)).append("*\n");

		mensaje.append("\n👉 *¿Confirmas que este es el pedido que deseas registrar?*");

		return mensaje.toString();
	}

	/**
	 * Escribe el mensaje de confirmación en el campo "Isabel confirma" (ID: 876707)
	 * del lead en Kommo vía API PATCH para que el bot pueda leerlo y mostrárselo al cliente.
	 */
	public void actualizarConfirmacionLeadCRMBOT(String lead, JSONObject cotizacion) {

		int leadId;
		try {
			leadId = Integer.parseInt(lead);
		} catch (NumberFormatException e) {
			System.out.println("Lead inválido, no es un número: " + lead);
			return;
		}

		String mensajeTexto = cotizacion.get("mensaje") != null ? String.valueOf(cotizacion.get("mensaje")) : "";

		List<Map<String, Object>> customFields = new ArrayList<Map<String, Object>>();

		customFields.add(Map.of(
				"field_id", FIELD_CONFIRMACION,
				"values", List.of(Map.of("value", mensajeTexto))
		));

		Map<String, Object> leadData = Map.of(
				"id", leadId,
				"custom_fields_values", customFields
		);

		Gson gson = new Gson();
		String datos = gson.toJson(List.of(leadData));

		System.out.println("Datos enviados para confirmación del lead en Kommo (Campo " + FIELD_CONFIRMACION + "): " + datos);

		IntegracionCRM intCRM = IntegracionCRMDAO.obtenerInformacionIntegracion("KOMMO");
		if (intCRM == null || intCRM.getAccessToken() == null || intCRM.getAccessToken().trim().isEmpty()) {
			System.out.println("No se encontró token de integración para KOMMO.");
			return;
		}

		OkHttpClient client = new OkHttpClient();
		okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(mediaType, datos);

		Request request = new Request.Builder()
				.url("https://pizzaamericana.kommo.com/api/v4/leads")
				.patch(body)
				.addHeader("Authorization", "Bearer " + intCRM.getAccessToken())
				.build();

		try (okhttp3.Response response = client.newCall(request).execute()) {
			String respuestaJSON = response.body() != null ? response.body().string() : "";
			System.out.println("Response Code Kommo PATCH (Campo " + FIELD_CONFIRMACION + "): " + response.code());
			System.out.println("Response Body Kommo PATCH: " + respuestaJSON);

			if (!response.isSuccessful()) {
				System.out.println("⚠️ Aviso al actualizar campo " + FIELD_CONFIRMACION + " en Kommo. Código: " + response.code());
				if (respuestaJSON.contains("TooLong")) {
					System.out.println("💡 NOTA: En Kommo el campo actual es de tipo 'Texto' (límite 256 caracteres). Para permitir todo el desglose completo con saltos de línea y emojis, se recomienda configurarlo como tipo 'Texto largo' (Textarea) en Kommo.");
				}
			} else {
				System.out.println("✅ Campo " + FIELD_CONFIRMACION + " actualizado exitosamente en el Lead #" + leadId + " de Kommo.");
			}
		} catch (Exception e) {
			System.out.println("Error en request a CRM (confirmación): " + e.getMessage());
		}
	}

	/**
	 * Método de prueba / simulación para verificar con un Lead Real desde la API de Kommo
	 * o con un payload simulado si no hay conexión.
	 */
	public static void main(String[] args) {
		String leadId = "30161293";
		if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
			leadId = args[0].trim();
		}

		System.out.println("=================================================================");
		System.out.println("CONSULTA Y COTIZACIÓN CON LEAD REAL DE KOMMO: #" + leadId);
		System.out.println("=================================================================");

		ConfirmacionPedidoCRMBOTCtrl ctrl = new ConfirmacionPedidoCRMBOTCtrl();
		PedidoCtrl pedidoCtrl = new PedidoCtrl();

		try {
			System.out.println("1. Conectando a la API de Kommo para consultar lead " + leadId + "...");
			String jsonReal = pedidoCtrl.obtenerInformacionLeadCRM(leadId);

			System.out.println("\n2. RESPUESTA RAW DESDE KOMMO API (LEAD):");
			System.out.println(jsonReal);

			if (jsonReal == null || jsonReal.trim().isEmpty() || jsonReal.contains("\"status\":401") || jsonReal.contains("\"status\":404")) {
				System.out.println("\n⚠️ No se pudo obtener el lead desde Kommo (o no existe/error de token).");
				return;
			}

			System.out.println("\n3. Procesando cotización y desglose de precios...");
			JSONObject resultado = ctrl.cotizarPedidoBOTCRM(jsonReal);

			System.out.println("\n4. RESULTADO JSON CALCULADO:");
			System.out.println(resultado.toJSONString());

			System.out.println("\n5. MENSAJE GENERADO PARA EL CLIENTE EN EL CHATBOT:");
			System.out.println("-----------------------------------------------------------------");
			System.out.println(resultado.get("mensaje"));
			System.out.println("-----------------------------------------------------------------");

			System.out.println("\n6. Enviando actualización a Kommo (Campo " + FIELD_CONFIRMACION + " 'CONFIRMACION_PD')...");
			ctrl.actualizarConfirmacionLeadCRMBOT(leadId, resultado);

		} catch (Exception e) {
			System.out.println("Error durante la prueba con lead real: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
