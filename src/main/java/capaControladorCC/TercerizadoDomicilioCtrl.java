package capaControladorCC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import capaDAOCC.ParametrosDAO;
import capaModeloCC.RappiCargoOrden;

public class TercerizadoDomicilioCtrl {
	

	
	
	public static String crearOrden(RappiCargoOrden orden) {
		
		try {
		String url_service = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_URL_SERVICE");
		String token = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_TOKEN");
		
		if (url_service == null || url_service.trim().isEmpty()) {
		    return "No está configurado RP_CARGO_URL_SERVICE";
		}

		if (token == null || token.trim().isEmpty()) {
		    return "No está configurado RP_CARGO_TOKEN";
		}
		String url_order_create = url_service.endsWith("/")
		        ? url_service + "v3/order-create"
		        : url_service + "/v3/order-create";
		
		System.out.println("URL: " + url_order_create);
		
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

		pickup.put("external_picking_point_id",
		        orden.getExternalPickingPointId());

		pickup.put("instructions",
		        orden.getInstructions());

		pickup.put("action_type", "PICK_UP");

		pickup.put("location_type", "STORE");
		
		ArrayNode products = mapper.createArrayNode();

		ObjectNode product = mapper.createObjectNode();

		product.put("product_id", orden.getProductId());
		product.put("name", orden.getProductName());
		product.put("description", orden.getProductDescription());
		product.put("units", orden.getUnits());
		product.put("unit_price", orden.getUnitPrice());

		products.add(product);

		pickup.set("products", products);
		
		
		ObjectNode drop = mapper.createObjectNode();

		drop.set("products", mapper.createArrayNode());

		drop.put("action_type", "DROP_OFF");
		drop.put("location_type", "CLIENT");
		
		
		actions.add(pickup);

		actions.add(drop);

		root.set("action_points", actions);
		
		
	
			
			HttpPost request = new HttpPost(url_order_create);

			request.setHeader("user-token", token);

			request.setHeader("Content-Type", "application/json");
			
			String json = mapper.writerWithDefaultPrettyPrinter()
			        .writeValueAsString(root);

			System.out.println(json);

			request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
			
			HttpClient client = HttpClientBuilder.create().build();

			HttpResponse response = client.execute(request);
			
			int status = response.getStatusLine().getStatusCode();

			BufferedReader rd = new BufferedReader(
			        new InputStreamReader(response.getEntity().getContent()));

			StringBuilder retorno = new StringBuilder();

			String line;

			while ((line = rd.readLine()) != null) {
			    retorno.append(line);
			}

			rd.close();
			
			System.out.println("HTTP Status: " + status);

			if (status >= 200 && status < 300) {
				
				System.out.println("Respuesta RappiCargo: " + retorno.toString());
				
			    return retorno.toString();
			} else {
			    return "HTTP " + status + ": " + retorno.toString();
			}
			
			
			
	      } catch (Exception e) {

	    	  e.printStackTrace();
	    	  return "ERROR: " + e.getMessage();
		}
		
		
	}

}
