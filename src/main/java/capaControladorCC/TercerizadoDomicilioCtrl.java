package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.TercerizadoDomicilioEventoDAO;
import capaModeloCC.DetallePedido;
import capaModeloCC.RappiCargoOrden;
import capaModeloCC.RappiCargoOrden.RappiCargoProducto;
import capaModeloCC.RespuestaRappiCargo;
public class TercerizadoDomicilioCtrl {
	

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
	
	public String crearOrdenRappiCargo(int idpedido) {

	    JSONArray listJSON = new JSONArray();
	    JSONObject respuestaJSON = new JSONObject();

	    try {

	        RappiCargoOrden orden =
	                TercerizadoDomicilioEventoDAO.ConsultarPedidoRappiCargo(idpedido);


	        if (orden == null) {

	            respuestaJSON.put("resultado", false);
	            respuestaJSON.put("mensaje",
	                    "No fue posible construir la orden Rappi Cargo.");

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
	                    new BigInteger(cargoNode.asText());



	            TercerizadoDomicilioEventoDAO.actualizarPedidoRappiCargo(
	                    idpedido,
	                    cargoOrderId);



	            respuestaJSON.put("resultado", true);

	            respuestaJSON.put("mensaje",
	                    "Orden creada correctamente.");

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
}
