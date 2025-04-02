package capaControladorCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import capaDAOCC.IntegracionCRMDAO;
import capaDAOCC.SegmentacionClienteDAO;
import capaModeloCC.ClienteClub;
import capaModeloCC.ClienteSegmento;
import capaModeloCC.IntegracionCRM;
import capaModeloCC.PlantillaBrevo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SegmentacionClienteCtrl {

	private static final Gson gson = new Gson();
	private static final String API_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

	public static String obtenerPlantillas() {
		SegmentacionClienteDAO segmentacionCliente = new SegmentacionClienteDAO();
		List<PlantillaBrevo> plantillas = segmentacionCliente.obtenerPlantillas();

		JSONArray jsonArray = new JSONArray();

		if (plantillas != null) { // Evita `NullPointerException`
			for (PlantillaBrevo plantilla : plantillas) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("idplantilla", plantilla.getIdPlantilla());
				jsonObject.put("nombre", plantilla.getNombre());
				jsonArray.put(jsonObject);
			}
		}

		return jsonArray.toString();
	}

	public static String obtenerClientesFiltrados(String fechaInicio, String fechaMaxima, int minPedidos,
			List<Integer> excepciones, List<Integer> idTienda) {
		SegmentacionClienteDAO segmentacionClienteDAO = new SegmentacionClienteDAO();
		List<ClienteSegmento> clientes = segmentacionClienteDAO.obtenerClientesFiltrados(fechaInicio, fechaMaxima,
				minPedidos, excepciones, idTienda);

		JSONArray jsonArray = new JSONArray();
		for (ClienteSegmento cliente : clientes) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("idcliente", cliente.getIdCliente());
			jsonObject.put("nombre", cliente.getNombre());
			jsonObject.put("nombreComp", cliente.getNombreComp());
			jsonObject.put("telefono", cliente.getTelefono());
			jsonObject.put("nombretienda", cliente.getNombreTienda());
			jsonObject.put("numeropedidos", cliente.getNumeroPedidos());
			jsonObject.put("fechamaxima", cliente.getFechaMaxima().toString());
			jsonObject.put("email", cliente.getEmail());
			jsonArray.put(jsonObject);
		}
		return jsonArray.toString();
	}
	
	
	public static String obtenerClientesClub(String estadoMiembro, String correoMiembro, int puntosMiembro, String fechaMaxima, String fechaInicio, List<Integer> tiendas) {
	    SegmentacionClienteDAO segmentacionClienteDAO = new SegmentacionClienteDAO();
	    List<ClienteClub> clientes = segmentacionClienteDAO.obtenerClientesClub(estadoMiembro, correoMiembro, puntosMiembro, fechaMaxima, fechaInicio, tiendas);

	    JSONArray jsonArray = new JSONArray();
	    for (ClienteClub cliente : clientes) {
	        JSONObject jsonObject = new JSONObject();
	        jsonObject.put("idCliente", cliente.getIdcliente());
	        jsonObject.put("nombre", cliente.getNombre());
	        jsonObject.put("nombreCompania", cliente.getNombrecompania());
	        jsonObject.put("telefono", cliente.getTelefono());
	        jsonObject.put("correo", cliente.getCorreo());
	        jsonObject.put("fechaVinculacion", cliente.getFechaVinculado().toString()); // Asegurar formato adecuado
	        jsonObject.put("activo", cliente.getActivo()); // Si es boolean, lo tomará como true/false
	        jsonObject.put("puntosVigentes", cliente.getPuntos());
	        jsonObject.put("nombreTienda", cliente.getNombreTienda());

	        jsonArray.put(jsonObject);
	    }
	    return jsonArray.toString();
	}


	public JsonObject envioCorreoBrevo(List<JsonObject> correos, String asunto, int idplantilla,
	        List<JsonObject> paramsDefault) throws IOException {

	    JsonObject jsonResponse = new JsonObject();

	    try {
	        // Recuperar los datos de integración con Brevo
	        IntegracionCRM brevo = IntegracionCRMDAO.obtenerInformacionIntegracion("BREVO");

	        // Configuración global
	        String apiKey = brevo.getAccessToken();
	        JsonObject requestBody = new JsonObject();
	        JsonArray messageVersions = new JsonArray();
	        JsonObject globalParams = new JsonObject();

	        // Agregar parámetros globales
	        for (JsonObject param : paramsDefault) {
	            globalParams.addProperty(param.get("clave").getAsString(), param.get("valor").getAsString());
	        }

	        // Procesar cada correo
	        for (JsonObject correo : correos) {
	            JsonObject message = new JsonObject();
	            JsonArray toArray = new JsonArray();

	            JsonObject recipient = new JsonObject();
	            recipient.addProperty("name", correo.get("name").getAsString());
	            recipient.addProperty("email", correo.get("email").getAsString());
	            toArray.add(recipient);

	            if (correo.has("subject") || correo.has("params")) {
	                if (correo.has("subject")) {
	                    message.addProperty("subject", correo.get("subject").getAsString());
	                }
	                if (correo.has("params")) {
	                    message.add("params", correo.get("params"));
	                }
	            }

	            message.add("to", toArray);
	            messageVersions.add(message);
	        }

	        // Construir la solicitud final
	        requestBody.add("messageVersions", messageVersions);
	        if (!asunto.isEmpty()) {
	            requestBody.addProperty("subject", asunto);
	        }
	        requestBody.addProperty("templateId", idplantilla);

	        if (globalParams.size() > 0) {
	            requestBody.add("params", globalParams);
	        }

	        JsonObject sender = new JsonObject();
	        sender.addProperty("name", "Pizza Americana");
	        sender.addProperty("email", "mercadeo@pizzaamericana.com.co");
	        requestBody.add("sender", sender);
	        
	 

	        // Enviar la solicitud con OkHttp
	        OkHttpClient client = new OkHttpClient();
	        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toString());
	        Request request = new Request.Builder().url(API_EMAIL_URL).post(body)
	                .addHeader("accept", "application/json")
	                .addHeader("api-key", apiKey)
	                .addHeader("content-type", "application/json")
	                .build();

	        Response response = client.newCall(request).execute();

	        // Manejar la respuesta
	        if (!response.isSuccessful()) {
	            jsonResponse.addProperty("success", false);
	            jsonResponse.addProperty("message", "Error en la API de Brevo: " + response.body().string());
	        } else {
	            jsonResponse.addProperty("success", true);
	            jsonResponse.addProperty("message", "Correos enviados correctamente.");
	        }

	        // Cerrar la respuesta después de procesarla
	        response.close();

	    } catch (IOException e) {
	        jsonResponse.addProperty("success", false);
	        jsonResponse.addProperty("message", "Error interno: " + e.getMessage());
	    }

	    return jsonResponse;
	}

	
	
}
