package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.TercerizadoDomicilioEventoDAO;
import capaModeloCC.DetallePedido;

import capaModeloCC.Parametro;
import capaModeloCC.Pedido;
import capaModeloCC.RappiCargoOrden;
import capaModeloCC.RappiCargoOrden.RappiCargoProducto;
import capaModeloCC.RespuestaRappiCargo;
import capaModeloCC.TercerizadoDomicilioEvento;
import capaModeloCC.Tienda;
import capaModeloCC.Ubicacion;
public class TercerizadoDomicilioCtrl {
	
	// Numero fijo usado para calcular distancias con latitud y longitud.
	// No se cambia.
	private static final double RADIO_TIERRA_KM = 6371.0088;

	private static final double KM_MIN_RAPPI_CARGO = 1.5;
	private static final double KM_MAX_RAPPI_CARGO = 3.0;

	private static final double FACTOR_ESTIMACION_RUTA = 1.25;
	
	private static final double LAT_MIN = -90.0;
	private static final double LAT_MAX = 90.0;
	private static final double LNG_MIN = -180.0;
	private static final double LNG_MAX = 180.0;
	private static final int DECIMALES_MINIMOS_RAPPI = 3; // Rappi exige "more than 2 decimal digits"

	public static RespuestaRappiCargo crearOrden(RappiCargoOrden orden) {

	    RespuestaRappiCargo respuesta = new RespuestaRappiCargo();

	    try {
	    	
	    	String errorValidacion = validarOrden(orden);

	    	if (errorValidacion != null) {
	    	    respuesta.setExito(false);
	    	    respuesta.setMensaje(errorValidacion);
	    	    return respuesta;
	    	}

	        String url_service = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_URL_SERVICE");
	        String token = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_TOKEN");

	        if (url_service == null || url_service.trim().isEmpty()) {

	            respuesta.setExito(false);
	            respuesta.setMensaje("No está configurado RP_CARGO_URL_SERVICE");
	            return respuesta;
	        }


	        if (token == null || token.trim().isEmpty()) {

	            respuesta.setExito(false);
	            respuesta.setMensaje("No está configurado RP_CARGO_TOKEN");
	            return respuesta;
	        }


	        String url_order_create = url_service.endsWith("/")
	                ? url_service + "v3/order-create/"
	                : url_service + "/v3/order-create/";


	        ObjectMapper mapper = new ObjectMapper();

	        ObjectNode root = mapper.createObjectNode();


	        root.put("total_value", orden.getTotalValue());
	        root.put("user_tip", orden.getUserTip());
	        root.put("vehicle_type", orden.getVehicleType());
	        root.put("payment_method", orden.getPaymentMethod());
	        root.put("order_id", orden.getOrderId());
	        root.put("in_store_reference_id", orden.getInStoreReferenceId());


	        ObjectNode cliente = mapper.createObjectNode();

	        cliente.put("email", orden.getEmail());
	        cliente.put("phone", orden.getPhone());
	        cliente.put("phone_country_code", orden.getPhoneCountryCode());
	        cliente.put("first_name", orden.getFirstName());
	        cliente.put("last_name", orden.getLastName());
	        cliente.put("address", orden.getAddress());
	        cliente.put("lat", orden.getLat());
	        cliente.put("lng", orden.getLng());
	        cliente.put("complement", orden.getComplement());
	        cliente.put("city", orden.getCity());
	        cliente.put("comments", orden.getComments());
	        
	        root.set("client_info", cliente);



	        ArrayNode actions = mapper.createArrayNode();


	        ObjectNode pickup = mapper.createObjectNode();
	        
	        String externalPickingPointId = orden.getExternalPickingPointId();

	        int pruebaActiva = ParametrosDAO.retornarValorNumerico("TIENDAPRUEBACARGO");

	        if (pruebaActiva > 0) {
	            externalPickingPointId = ParametrosDAO.retornarValorAlfanumerico("TIENDAPRUEBACARGO");
	        }

	        pickup.put("external_picking_point_id", externalPickingPointId);
	        pickup.put("instructions",
	                orden.getInstructions());

	        pickup.put("action_type", "PICK_UP");
	        pickup.put("location_type", "STORE");


	        ArrayNode products = mapper.createArrayNode();


	        for(RappiCargoOrden.RappiCargoProducto producto : orden.getProducts()) {

	            ObjectNode item = mapper.createObjectNode();

	            item.put("product_id", producto.getProductId());
	            item.put("name", producto.getProductName());
	            item.put("description", producto.getProductDescription());
	            item.put("units", producto.getUnits());
	            item.put("unit_price", producto.getUnitPrice());

	            products.add(item);
	        }


	        pickup.set("products", products);



	        ObjectNode drop = mapper.createObjectNode();

	        drop.set("products", mapper.createArrayNode());
	        drop.put("action_type", "DROP_OFF");
	        drop.put("location_type", "CLIENT");


	        actions.add(pickup);
	        actions.add(drop);


	        root.set("action_points", actions);



	        String json =
	                mapper.writeValueAsString(root);



	        HttpPost request = new HttpPost(url_order_create);

	        request.setHeader("user-token", token);
	        request.setHeader("Content-Type", "application/json");


	        request.setEntity(
	                new StringEntity(json, StandardCharsets.UTF_8)
	        );


	        HttpClient client =
	                HttpClientBuilder.create().build();


	        HttpResponse response =
	                client.execute(request);



	        int status =
	                response.getStatusLine().getStatusCode();



	        StringBuilder retorno = new StringBuilder();


	        if(response.getEntity()!=null){

	            BufferedReader rd =
	                    new BufferedReader(
	                            new InputStreamReader(
	                                    response.getEntity().getContent()
	                            )
	                    );


	            String line;

	            while((line = rd.readLine())!=null){
	                retorno.append(line);
	            }

	            rd.close();
	        }



	        respuesta.setHttpStatus(status);
	        respuesta.setBody(retorno.toString());


	        if (status >= 200 && status < 300) {

	            respuesta.setExito(true);
	            respuesta.setMensaje("OK");

	        } else if (status >= 400) {

	            try {

	                JsonNode error = mapper.readTree(retorno.toString());

	                if (error.has("message")) {
	                    respuesta.setMensaje(error.get("message").asText());
	                } else {
	                    respuesta.setMensaje("HTTP " + status);
	                }

	            } catch (Exception e) {

	                respuesta.setMensaje(
	                        "HTTP " + status + " " + response.getStatusLine().getReasonPhrase()
	                );
	            }

	            respuesta.setExito(false);

	        } else {

	            respuesta.setExito(false);
	            respuesta.setMensaje(
	                    "HTTP " + status + " " + response.getStatusLine().getReasonPhrase()
	            );
	        }

	    } catch(Exception e){

	        e.printStackTrace();

	        respuesta.setExito(false);
	        respuesta.setMensaje(
	                "ERROR: " + e.getMessage()
	        );
	    }


	    return respuesta;
	}
	
	public String crearOrdenRappiCargo(int idpedido , int idtienda ,int numposheader) {

	    JSONArray listJSON = new JSONArray();
	    JSONObject respuestaJSON = new JSONObject();

	    try {

	        RappiCargoOrden orden =
	                TercerizadoDomicilioEventoDAO.ConsultarPedidoRappiCargo(idpedido ,idtienda, numposheader);


	        if (orden == null) {

	            respuestaJSON.put("resultado", false);
	            respuestaJSON.put("mensaje",
	                    "No fue posible construir la orden Rappi Cargo.");

	            listJSON.add(respuestaJSON);

	            return listJSON.toJSONString();
	        }
	        
	        int idPedidoReal = idpedido;

	        try {
	            idPedidoReal = Integer.parseInt(orden.getOrderId());
	        } catch (Exception e) {

	            respuestaJSON.put("resultado", false);
	            respuestaJSON.put("mensaje",
	                    "No fue posible identificar el idpedido real de la orden Rappi Cargo.");

	            listJSON.add(respuestaJSON);

	            return listJSON.toJSONString();
	        }


	        if (orden.getPhone() == null ||
	                orden.getPhone().trim().isEmpty()) {

	            respuestaJSON.put("resultado", false);
	            respuestaJSON.put("mensaje",
	                    "El cliente no tiene teléfono registrado.");

	            listJSON.add(respuestaJSON);

	            return listJSON.toJSONString();
	        }

	        if (!coordenadaValidaParaRappiCargo(orden.getLat(), orden.getLng())) {
	            System.out.println("Pedido " + idPedidoReal + " llega con coordenadas en 0 para Rappi Cargo, intentando geocodificar...");
	            UbicacionCtrl ubicacionCtrl = new UbicacionCtrl();
	            Ubicacion ubicacionEncontrada = ubicacionCtrl.ubicarDireccionEnTiendaBatch(
	                    orden.getAddress(), orden.getCity(), ""
	            );
	            if (ubicacionEncontrada != null
	                    && coordenadaValidaParaRappiCargo(ubicacionEncontrada.getLatitud(), ubicacionEncontrada.getLongitud())) {
	                orden.setLat(ubicacionEncontrada.getLatitud());
	                orden.setLng(ubicacionEncontrada.getLongitud());
	                if (orden.getIdcliente() > 0) {
	                    new ClienteCtrl().actualizarClienteCoordenadas(
	                            orden.getIdcliente(),
	                            (float) ubicacionEncontrada.getLatitud(),
	                            (float) ubicacionEncontrada.getLongitud(),
	                            orden.getAddress()
	                    );
	                }
	            } else {
	                respuestaJSON.put("resultado", false);
	                respuestaJSON.put("mensaje",
	                        "No fue posible determinar las coordenadas de la dirección del cliente. "
	                        + "Verifica o corrige la dirección antes de crear la orden en Rappi Cargo.");
	                listJSON.add(respuestaJSON);
	                return listJSON.toJSONString();
	            }
	        }

	        // Crear orden en Rappi Cargo
	        RespuestaRappiCargo respuestaRappi =
	                TercerizadoDomicilioCtrl.crearOrden(orden);



	        System.out.println("=====================================");
	        System.out.println("Respuesta crearOrden()");
	        System.out.println("HTTP: " + respuestaRappi.getHttpStatus());
	        System.out.println("Mensaje: " + respuestaRappi.getMensaje());
	        System.out.println("Body: " + respuestaRappi.getBody());
	        System.out.println("=====================================");



	        /*
	         * Validación de respuesta negativa de Rappi
	         */
	        if (!respuestaRappi.isExito()) {

	            String mensaje;


	            switch (respuestaRappi.getHttpStatus()) {

	                case 307:

	                    mensaje =
	                    "El servicio de Rappi redireccionó la petición. Verifique la URL configurada.";

	                    break;

	                case 401:

	                    mensaje =
	                    "Token de autenticación inválido.";

	                    break;


	                case 400:
	                case 422:

	                    mensaje = respuestaRappi.getMensaje();

	                    break;


	                default:
	                    mensaje = respuestaRappi.getMensaje();
	                    break;
	            }



	            respuestaJSON.put("resultado", false);
	            respuestaJSON.put("mensaje", mensaje);
	            respuestaJSON.put("respuestaRappi",
	                    respuestaRappi.getBody());


	            listJSON.add(respuestaJSON);


	            return listJSON.toJSONString();
	        }



	        /*
	         * Procesar respuesta exitosa de Rappi
	         */
	        ObjectMapper mapper = new ObjectMapper();


	        JsonNode respuesta =
	                mapper.readTree(respuestaRappi.getBody());


	        JsonNode cargoNode =
	                respuesta.get("cargo_order_id");



	        if (cargoNode != null &&
	                !cargoNode.asText().trim().isEmpty()) {


	            BigInteger cargoOrderId =
	                    new BigInteger(cargoNode.asText().trim());
	            
	            JsonNode rappiNode =
		                respuesta.get("rappi_order_id");
	            
	            JsonNode tracking_urlNode =
		                respuesta.get("tracking_url");
	            
	            String rappiOrderId = rappiNode != null
	                    ? rappiNode.asText().trim()
	                    : "";

	            String trackingUrl = tracking_urlNode != null
	                    ? tracking_urlNode.asText().trim()
	                    : "";
	            long idPedidoEv = idPedidoReal;
	            
	            
	            Long idEvento = TercerizadoDomicilioEventoDAO.guardarEvento(
	            		idPedidoEv,
	            		"RAPPICARGO",
	            		cargoNode.toString(),
	            		rappiOrderId,
	                    "created",
	                    trackingUrl,
	                    null,
	                    null,
	                    respuestaRappi.getBody(),
	                    "OK"
	            );


	            long cargoOrderIdLong = cargoOrderId.longValueExact();

	            boolean actualizado =
	                    TercerizadoDomicilioEventoDAO.actualizarPedidoRappiCargo(
	                    		idPedidoReal,
	                            cargoOrderId
	                    );
	           
	            String msg = "Orden creada correctamente.";

	            if (!actualizado) {

	                long idActualBd =
	                        TercerizadoDomicilioEventoDAO
	                                .consultarIdOrdenComercioPedido(idPedidoReal);

	                if (idActualBd != cargoOrderIdLong) {
                        
	                	
	                	
	                	msg =  " La orden fue creada en Rappi Cargo, pero no fue posible "
	                            + "asociarla al pedido interno porque ya existe otro "
	                            + "idordencomercio en BD. se debe revisar para evitar inconvenientes con el pedido. Actual BD: "
	                            + idActualBd
	                            + ", recibido: "
	                            + cargoOrderIdLong;
	                }
	            }
	                       
	            if (numposheader > 0 && idtienda > 0) {
	                JSONObject respuestaMarcacionTienda =
	                        marcarPedidoTercerizadoEnTienda(idtienda, numposheader);

	                respuestaJSON.put("marcadoTienda",
	                        Boolean.TRUE.equals(respuestaMarcacionTienda.get("exito")));

	                respuestaJSON.put("mensajeMarcacionTienda",
	                        respuestaMarcacionTienda.get("mensaje"));
	            }


	            respuestaJSON.put("resultado", true);

	            respuestaJSON.put("mensaje",msg);

	            respuestaJSON.put("cargo_order_id",
	                    cargoOrderId.toString());

	            respuestaJSON.put("respuestaRappi",
	                    respuestaRappi.getBody());



	        } else {


	            respuestaJSON.put("resultado", false);

	            respuestaJSON.put("mensaje",
	                    "Rappi respondió correctamente pero no devolvió el cargo_order_id.");

	            respuestaJSON.put("respuestaRappi",
	                    respuestaRappi.getBody());
	        }



	    } catch (Exception e) {


	        e.printStackTrace();


	        respuestaJSON.put("resultado", false);

	        respuestaJSON.put("mensaje",
	                "ERROR: " + e.getMessage());
	    }



	    listJSON.add(respuestaJSON);



	    System.out.println("JSON enviado al Front:");
	    System.out.println(listJSON.toJSONString());


	    return listJSON.toJSONString();
	}
	
	private static String validarOrden(RappiCargoOrden orden) {

	    List<String> errores = new ArrayList<>();

	    if (orden == null) {
	        return "La orden no puede ser nula.";
	    }

	    // total_value
	    if (orden.getTotalValue() < 1) {
	        errores.add("El valor total de la orden (total_value) debe ser mayor o igual a 1.");
	    }

	    // vehicle_type
	    if (orden.getVehicleType() == null
	            || (!"CAR".equalsIgnoreCase(orden.getVehicleType())
	            && !"BIKE".equalsIgnoreCase(orden.getVehicleType()))) {

	        errores.add("El tipo de vehículo (vehicle_type) debe ser CAR o BIKE.");
	    }

	    // payment_method
	    if (orden.getPaymentMethod() == null
	            || (!"CASH".equalsIgnoreCase(orden.getPaymentMethod())
	            && !"ONLINE".equalsIgnoreCase(orden.getPaymentMethod()))) {

	        errores.add("El método de pago (payment_method) debe ser CASH o ONLINE.");
	    }

	    // order_id
	    if (orden.getOrderId() == null
	            || orden.getOrderId().trim().isEmpty()) {

	        errores.add("El identificador de la orden (order_id) es obligatorio.");
	    }
	    
	    if (orden.getEmail() == null || orden.getEmail().trim().isEmpty()) {
	        errores.add("El correo electrónico del cliente es obligatorio.");
	    } else if (orden.getEmail().trim().length() < 5) {
	        errores.add("El correo electrónico del cliente debe tener al menos 5 caracteres.");
	    } else if (!orden.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
	        errores.add("El correo electrónico del cliente no tiene un formato válido.");
	    }

	 // Cliente
	    if (orden.getFirstName() == null || orden.getFirstName().trim().isEmpty()) {
	        errores.add("El nombre del cliente es obligatorio.");
	    }

	    if (orden.getPhone() == null || orden.getPhone().trim().isEmpty()) {
	        errores.add("El teléfono del cliente es obligatorio.");
	    }

	    if (orden.getAddress() == null || orden.getAddress().trim().isEmpty()) {
	        errores.add("La dirección del cliente es obligatoria.");
	    }

	    // Punto de recogida
	    if (orden.getExternalPickingPointId() == null
	            || orden.getExternalPickingPointId().trim().isEmpty()) {
	        errores.add("El punto de recogida (external_picking_point_id) es obligatorio.");
	    }

	    // products del PICK_UP
	    if (orden.getProducts() == null || orden.getProducts().isEmpty()) {
	        errores.add("La orden debe contener al menos un producto para el punto de recogida.");
	    } else {

	        int i = 1;

	        for (RappiCargoProducto producto : orden.getProducts()) {

	            if (producto == null) {
	                errores.add("El producto #" + i + " es inválido.");
	                i++;
	                continue;
	            }

	            if (producto.getProductId() == null || producto.getProductId().trim().isEmpty()) {
	                errores.add("El producto #" + i + " no tiene product_id.");
	            }

	            if (producto.getProductName() == null || producto.getProductName().trim().isEmpty()) {
	                errores.add("El producto #" + i + " no tiene nombre.");
	            }

	            if (producto.getUnits() <= 0) {
	                errores.add("El producto #" + i + " debe tener al menos una unidad.");
	            }

	            if (producto.getUnitPrice() < 0) {
	                errores.add("El producto #" + i + " tiene un precio inválido.");
	            }

	            i++;
	        }
	    }

	    if (!errores.isEmpty()) {
	        return "No fue posible crear la orden en Rappi Cargo por las siguientes razones: "
	                + String.join(" ", errores);
	    }

	    return null;
	}
	


	public static boolean debeUsarRappiCargo(
	        double latitudTienda,
	        double longitudTienda,
	        double latitudCliente,
	        double longitudCliente) {

	    JSONObject validacion = validarDistanciaRappiCargo(
	            latitudTienda,
	            longitudTienda,
	            latitudCliente,
	            longitudCliente
	    );

	    return Boolean.TRUE.equals(validacion.get("respuesta"));
	}
	
	public static JSONObject validarDistanciaRappiCargo(
	        double latitudTienda,
	        double longitudTienda,
	        double latitudCliente,
	        double longitudCliente) {

	    JSONObject respuesta = new JSONObject();

	    if (!coordenadaValida(latitudTienda, longitudTienda)
	            || !coordenadaValida(latitudCliente, longitudCliente)) {

	        respuesta.put("respuesta", false);
	        respuesta.put("mensaje", "No se pudo validar Rappi Cargo porque la tienda o el cliente no tienen coordenadas validas.");
	        respuesta.put("distanciaKm", 0);

	        return respuesta;
	    }

	    double distanciaLinealKm = calcularDistanciaKm(
	            latitudTienda,
	            longitudTienda,
	            latitudCliente,
	            longitudCliente
	    );

	    double kmMinRappiCargo = obtenerParametroNumerico(
	            "KM_MIN_RAPPI_CARGO",
	            KM_MIN_RAPPI_CARGO
	    );

	    double kmMaxRappiCargo = obtenerParametroNumerico(
	            "KM_MAX_RAPPI_CARGO",
	            KM_MAX_RAPPI_CARGO
	    );

	    double factorEstimacionRuta = obtenerParametroNumerico(
	            "FACTOR_ESTIMACION_RUTA",
	            FACTOR_ESTIMACION_RUTA
	    );

	    double distanciaEstimadaRutaKm = distanciaLinealKm * factorEstimacionRuta;
	    double distanciaRedondeada = Math.round(distanciaEstimadaRutaKm * 100.0) / 100.0;

	    respuesta.put("distanciaKm", distanciaRedondeada);

	    if (distanciaEstimadaRutaKm < kmMinRappiCargo) {
	        respuesta.put("respuesta", false);
	        respuesta.put("mensaje", "El pedido esta muy cerca para Rappi Cargo. Distancia estimada: "
	                + distanciaRedondeada + " km.");
	        return respuesta;
	    }

	    if (distanciaEstimadaRutaKm > kmMaxRappiCargo) {
	        respuesta.put("respuesta", false);
	        respuesta.put("mensaje", "El pedido esta muy lejos para Rappi Cargo. Distancia estimada: "
	                + distanciaRedondeada + " km.");
	        return respuesta;
	    }

	    respuesta.put("respuesta", true);
	    respuesta.put("mensaje", "El pedido aplica para Rappi Cargo. Distancia estimada: "
	            + distanciaRedondeada + " km.");

	    return respuesta;
	}
	
	private static double obtenerParametroNumerico(String nombreParametro, double valorPorDefecto) {
	    try {
	        Parametro parametro = ParametrosDAO.obtenerParametro(nombreParametro);

	        if (parametro == null || parametro.getValorNumerico() <= 0) {
	            return valorPorDefecto;
	        }

	        return parametro.getValorNumerico();

	    } catch (Exception e) {
	        return valorPorDefecto;
	    }
	}

	public static double calcularDistanciaKm(
	        double latitudOrigen,
	        double longitudOrigen,
	        double latitudDestino,
	        double longitudDestino) {

	    if (!coordenadaValida(latitudOrigen, longitudOrigen)
	            || !coordenadaValida(latitudDestino, longitudDestino)) {
	        return -1.0;
	    }

	    double lat1Rad = Math.toRadians(latitudOrigen);
	    double lat2Rad = Math.toRadians(latitudDestino);
	    double deltaLatRad = Math.toRadians(latitudDestino - latitudOrigen);
	    double deltaLngRad = Math.toRadians(longitudDestino - longitudOrigen);

	    double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
	            + Math.cos(lat1Rad) * Math.cos(lat2Rad)
	            * Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);

	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

	    return RADIO_TIERRA_KM * c;
	}

	private static boolean coordenadaValida(double latitud, double longitud) {
	    return latitud >= -90 && latitud <= 90
	            && longitud >= -180 && longitud <= 180
	            && !(latitud == 0 && longitud == 0);
	}

	
	public static RespuestaRappiCargo notificarPedidoListo(int idPedidoTienda, int idTienda) {

	    RespuestaRappiCargo respuesta = new RespuestaRappiCargo();

	    try {

	        TercerizadoDomicilioEvento tercerizadoDomicilioEvento = 
	                TercerizadoDomicilioEventoDAO.ConsultarInfoRappiCargo(idPedidoTienda, idTienda);
	        
	        if (tercerizadoDomicilioEvento == null) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("No se encontró información de Rappi Cargo para el pedido "
	                    + idPedidoTienda + " de la tienda " + idTienda + ".");
	            return respuesta;
	        }
	        
	        String rappiOrderId = tercerizadoDomicilioEvento.getIdPedidoLogistico();
	        
	        if (rappiOrderId == null || rappiOrderId.trim().isEmpty()) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("El pedido " + idPedidoTienda + " no tiene rappi_order_id asociado.");
	            return respuesta;
	        }
	        
	        String fechacancelacion = tercerizadoDomicilioEvento.getFechaCancelacion();

	        if (fechacancelacion != null && !fechacancelacion.trim().isEmpty()) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("El pedido " + idPedidoTienda
	                    + " se registra como cancelado en la plataforma de Rappi Cargo.");
	            return respuesta;
	        }
	        
	        int canceladoInterno = tercerizadoDomicilioEvento.getCanceladoInterno();
	        
	        if (canceladoInterno == 1) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("El pedido " + idPedidoTienda + " se registra como cancelado en nuestro sistema.");
	            return respuesta;
	        }

	        String urlService = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_URL_SERVICE");
	        String token = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_TOKEN");

	        if (urlService == null || urlService.trim().isEmpty()) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("No está configurado RP_CARGO_URL_SERVICE");
	            return respuesta;
	        }

	        if (token == null || token.trim().isEmpty()) {
	            respuesta.setExito(false);
	            respuesta.setMensaje("No está configurado RP_CARGO_TOKEN");
	            return respuesta;
	        }

	        // URL exacta según la documentación oficial de Rappi (sin barra adicional al final)
	        String urlReady = urlService.endsWith("/")
	                ? urlService + "v3/order-ready/" + rappiOrderId
	                : urlService + "/v3/order-ready/" + rappiOrderId;

	        // Asegurar el protocolo HTTPS si venía como HTTP desde BD
	        if (urlReady.startsWith("http://")) {
	            urlReady = urlReady.replace("http://", "https://");
	        }

	        HttpPost request = new HttpPost(urlReady);
	        request.setHeader("user-token", token);
	        request.setHeader("Content-Type", "application/json");

	        // SOLUCIÓN AL 307: Permite al cliente HTTP seguir redirecciones en peticiones POST
	        HttpClient client = HttpClientBuilder.create()
	                .setRedirectStrategy(new org.apache.http.impl.client.LaxRedirectStrategy())
	                .build();

	        HttpResponse response = client.execute(request);

	        int status = response.getStatusLine().getStatusCode();

	        StringBuilder retorno = new StringBuilder();

	        if (response.getEntity() != null) {
	            BufferedReader rd = new BufferedReader(
	                    new InputStreamReader(response.getEntity().getContent()));
	            String line;
	            while ((line = rd.readLine()) != null) {
	                retorno.append(line);
	            }
	            rd.close();
	        }

	        respuesta.setHttpStatus(status);
	        respuesta.setBody(retorno.toString());

	        if (status >= 200 && status < 300) {

	            respuesta.setExito(true);
	            respuesta.setMensaje("Pedido notificado correctamente en Rappi Cargo.");

	        } else {

	            respuesta.setExito(false);

	            try {
	                ObjectMapper mapper = new ObjectMapper();
	                JsonNode error = mapper.readTree(retorno.toString());

	                if (error.has("message")) {
	                    respuesta.setMensaje(error.get("message").asText());
	                } else {
	                    respuesta.setMensaje("HTTP " + status);
	                }

	            } catch (Exception e) {
	                respuesta.setMensaje("HTTP " + status + " " + response.getStatusLine().getReasonPhrase());
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        respuesta.setExito(false);
	        respuesta.setMensaje("Error notificando pedido listo a Rappi Cargo: " + e.getMessage());
	    }

	    return respuesta;
	}
	
	private JSONObject marcarPedidoTercerizadoEnTienda(int idtienda, int numposheader) {

	    JSONObject json = new JSONObject();
	    json.put("exito", false);
	    json.put("mensaje", "No se pudo marcar el pedido en tienda.");

	    try {
	        TiendaCtrl tiendaCtrl = new TiendaCtrl();

	        String respuestaTienda = tiendaCtrl.obtenerUrlTienda(idtienda);

	        JSONParser parser = new JSONParser();
	        JSONArray array = (JSONArray) parser.parse(respuestaTienda);

	        if (array.isEmpty()) {
	            json.put("mensaje", "No se encontró URL de tienda para idtienda " + idtienda);
	            return json;
	        }

	        JSONObject tienda = (JSONObject) array.get(0);

	        String urlTienda = String.valueOf(tienda.get("urltienda"));

	        if (urlTienda == null || urlTienda.trim().isEmpty()) {
	            json.put("mensaje", "La tienda " + idtienda + " no tiene URL configurada.");
	            return json;
	        }

	        String separador = urlTienda.endsWith("/") ? "" : "/";

	        String urlServicio =
	                urlTienda + separador
	                + "MarcarPedidoTercerizadoTienda"
	                + "?idPedidoTienda=" + numposheader
	                + "&idTienda=" + idtienda;

	        HttpClient client = HttpClientBuilder.create().build();
	        HttpPost post = new HttpPost(urlServicio);

	        HttpResponse response = client.execute(post);

	        StringBuilder body = new StringBuilder();

	        if (response.getEntity() != null) {
	            BufferedReader rd = new BufferedReader(
	                    new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

	            String line;

	            while ((line = rd.readLine()) != null) {
	                body.append(line);
	            }

	            rd.close();
	        }

	        int status = response.getStatusLine().getStatusCode();

	        if (status >= 200 && status < 300) {
	            JSONObject respuestaLocal =
	                    (JSONObject) parser.parse(body.toString());

	            return respuestaLocal;
	        }

	        json.put("mensaje", "La tienda respondió HTTP " + status + ": " + body.toString());

	    } catch (Exception e) {
	        e.printStackTrace();
	        json.put("mensaje", "Error llamando servicio local de tienda: " + e.getMessage());
	    }

	    return json;
	}
	
	/**
	 * Desmarca el pedido como tercerizado tanto en la base principal como
	 * en la base local de la tienda, sin importar el motivo de la
	 * cancelación. Es "best effort": si falla, se deja un log pero NO se
	 * interrumpe el resto del flujo de cancelación (el pedido igual se
	 * cancela normalmente).
	 */
	public static void desmarcarTercerizado(
	        Pedido pedEvento,
	        Tienda tienda,
	        Long idEvento) {

	    long idPedido = pedEvento.getIdpedido();

	    // 1. Base principal.
	    boolean desmarcadoPrincipal =
	    		TercerizadoDomicilioEventoDAO.desmarcarPedidoRappiCargo((int) idPedido);

	    if (!desmarcadoPrincipal) {
	        System.out.println(
	                "No se pudo desmarcar domicilio_tercerizado en la base "
	                        + "principal para el pedido " + idPedido);
	    }

	    // 2. Base local de la tienda.
	    String rutaURL =
	            tienda.getUrl()
	                    + "DesmarcarPedidoTercerizadoTienda?idPedidoTienda="
	                    + pedEvento.getNumposheader()
	                    + "&idTienda=" + tienda.getIdTienda();

	    HttpClient client = HttpClientBuilder.create().build();

	    try {
	        HttpGet request = new HttpGet(rutaURL);
	        HttpResponse response = client.execute(request);
	        if (response.getEntity() != null) {
	            BufferedReader rd = new BufferedReader(
	                    new InputStreamReader(
	                            response.getEntity().getContent()));
	            StringBuilder retorno = new StringBuilder();
	            String line;
	            while ((line = rd.readLine()) != null) {
	                retorno.append(line);
	            }
	            rd.close();
	            System.out.println(
	                    "Respuesta DesmarcarPedidoTercerizadoTienda: "
	                            + retorno.toString());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        System.out.println(
	                "Error consumiendo DesmarcarPedidoTercerizadoTienda: "
	                        + e.getMessage());
	    }
	}
	


	public static boolean coordenadaValidaParaRappiCargo(double lat, double lng) {
	    // rechaza valores no numericos o infinitos
	    if (Double.isNaN(lat) || Double.isNaN(lng) || Double.isInfinite(lat) || Double.isInfinite(lng)) {
	        return false;
	    }
	    // rechaza fuera del rango geografico valido
	    if (lat < LAT_MIN || lat > LAT_MAX || lng < LNG_MIN || lng > LNG_MAX) {
	        return false;
	    }
	    // rechaza sin ubicacion real
	    if (lat == 0 && lng == 0) {
	        return false;
	    }
	    // rechaza precision insuficiente para Rappi
	    if (!tieneSuficientesDecimales(lat) || !tieneSuficientesDecimales(lng)) {
	        return false;
	    }
	    return true;
	}

	private static boolean tieneSuficientesDecimales(double valor) {
	    java.math.BigDecimal bd = java.math.BigDecimal.valueOf(valor).stripTrailingZeros();
	    int decimales = Math.max(0, bd.scale());
	    return decimales >= DECIMALES_MINIMOS_RAPPI;
	}

}
