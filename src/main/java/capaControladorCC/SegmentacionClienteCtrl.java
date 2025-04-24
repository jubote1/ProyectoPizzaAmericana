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
import capaDAOCC.ParametrosDAO;
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
import com.google.gson.JsonElement;

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
				jsonObject.put("categoria", plantilla.getCategoria());
				jsonArray.put(jsonObject);
			}
		}

		return jsonArray.toString();
	}

	public static String obtenerClientesFiltrados(String fechaInicio, String fechaMaxima, int minPedidos,
			List<Integer> excepciones, List<Integer> idTiendas, int diasMinimosSinPublicidad) {
		SegmentacionClienteDAO segmentacionClienteDAO = new SegmentacionClienteDAO();
		List<ClienteSegmento> clientes = segmentacionClienteDAO.obtenerClientesFiltrados(fechaInicio, fechaMaxima,
				minPedidos, excepciones, idTiendas, diasMinimosSinPublicidad);

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

	public static String obtenerClientesClub(String estadoMiembro, String correoMiembro, int puntosMiembro,
			String fechaMaxima, String fechaInicio, List<Integer> tiendas) {
		SegmentacionClienteDAO segmentacionClienteDAO = new SegmentacionClienteDAO();
		List<ClienteClub> clientes = segmentacionClienteDAO.obtenerClientesClub(estadoMiembro, correoMiembro,
				puntosMiembro, fechaMaxima, fechaInicio, tiendas);

		JSONArray jsonArray = new JSONArray();
		for (ClienteClub cliente : clientes) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("idcliente", cliente.getIdcliente());
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
					.addHeader("accept", "application/json").addHeader("api-key", apiKey)
					.addHeader("content-type", "application/json").build();

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

	public static boolean actualizarFechaUltimaPublicidad(List<Integer> idsclientes) {
		SegmentacionClienteDAO segmentacionClienteDAO = new SegmentacionClienteDAO();
		boolean respuesta = segmentacionClienteDAO.actualizarFechaUltimaPublicidad(idsclientes);
		return respuesta;
	}

	public JsonObject envioWhatsappBrevo(List<JsonObject> telefonos, String mensaje, int idplantilla, List<JsonObject> paramsDefault) throws IOException {
	    JsonObject jsonResponse = new JsonObject();
	    JsonArray errores = new JsonArray();
	    OkHttpClient client = new OkHttpClient();

	    try {
	        IntegracionCRM brevo = IntegracionCRMDAO.obtenerInformacionIntegracion("BREVO");
	        String apiKey = brevo.getAccessToken();
	        String senderNumber = ParametrosDAO.retornarValorAlfanumerico("NUMEROWHATSAPPBREVO");

	        boolean requiereEnvioIndividual = telefonos.stream().anyMatch(t -> t.has("params") || t.has("nombre"));

	        if ((mensaje == null || mensaje.trim().isEmpty()) && idplantilla <= 0) {
	            jsonResponse.addProperty("success", false);
	            jsonResponse.addProperty("message", "El mensaje de texto no puede estar vacío si no se usa plantilla.");
	            return jsonResponse;
	        }

	        if (requiereEnvioIndividual) {
	            for (JsonObject contacto : telefonos) {
	                if (telefonoInvalido(contacto)) {
	                    JsonObject error = new JsonObject();
	                    error.addProperty("error", "Falta el campo 'telefono' en uno de los contactos.");
	                    errores.add(error);
	                    continue;
	                }

	                JsonObject requestBody = construirRequestBody(contacto, senderNumber, mensaje, idplantilla, paramsDefault);
	                ejecutarEnvio(client, requestBody, apiKey, contacto.get("telefono").getAsString(), errores);
	            }
	        } else {
	            JsonObject contactoGlobal = new JsonObject();
	            JsonArray contactNumbers = new JsonArray();

	            for (JsonObject contacto : telefonos) {
	                if (telefonoInvalido(contacto)) {
	                    JsonObject error = new JsonObject();
	                    error.addProperty("error", "Falta el campo 'telefono' en uno de los contactos.");
	                    errores.add(error);
	                    continue;
	                }
	                contactNumbers.add("+57" + contacto.get("telefono").getAsString());
	            }

	            if (contactNumbers.size() == 0) {
	                throw new Exception("No hay teléfonos válidos para enviar el mensaje.");
	            }

	            contactoGlobal.add("telefono", contactNumbers.get(0)); // solo para conservar compatibilidad de tipo
	            JsonObject requestBody = construirRequestBody(contactoGlobal, senderNumber, mensaje, idplantilla, paramsDefault);
	            requestBody.add("contactNumbers", contactNumbers);

	            ejecutarEnvio(client, requestBody, apiKey, null, errores);
	        }

	        jsonResponse.addProperty("success", errores.size() == 0);
	        jsonResponse.addProperty("message", errores.size() == 0 ? "Todos los mensajes fueron enviados correctamente." : "Algunos mensajes no pudieron ser enviados.");
	        if (errores.size() > 0) jsonResponse.add("errores", errores);

	    } catch (Exception e) {
	        jsonResponse.addProperty("success", false);
	        jsonResponse.addProperty("message", "Error interno: " + e.getMessage());
	    }

	    return jsonResponse;
	}


	private JsonObject construirParams(List<JsonObject> paramsDefault, JsonObject contacto) {
		JsonObject paramsFinal = new JsonObject();
		// Validar e insertar "nombre"
		if (contacto != null && contacto.has("nombre") && !contacto.get("nombre").isJsonNull()) {
			String nombre = contacto.get("nombre").getAsString();
		
			paramsFinal.addProperty("nombre", nombre); // Esto reemplaza o inserta automáticamente
		}else {
			paramsFinal.addProperty("titulo", "Pizza Americana");
		}

		if (paramsDefault != null) {
			for (JsonObject def : paramsDefault) {
				if (def.has("clave") && def.has("valor") && !def.get("clave").isJsonNull()
						&& !def.get("valor").isJsonNull()) {
					paramsFinal.addProperty(def.get("clave").getAsString(), def.get("valor").getAsString());
				}
			}
		}

		if (contacto != null && contacto.has("params") && contacto.get("params").isJsonArray()
				&& !contacto.get("params").isJsonNull()) {
			for (JsonElement el : contacto.getAsJsonArray("params")) {
				if (el.isJsonObject()) {
					JsonObject p = el.getAsJsonObject();
					if (p.has("clave") && p.has("valor") && !p.get("clave").isJsonNull()
							&& !p.get("valor").isJsonNull()) {
						paramsFinal.addProperty(p.get("clave").getAsString(), p.get("valor").getAsString());
					}
				}
			}

		}
		return paramsFinal;
		
	}

	private boolean telefonoInvalido(JsonObject contacto) {
	    return !contacto.has("telefono") || contacto.get("telefono").isJsonNull();
	}
	
	private JsonObject construirRequestBody(JsonObject contacto, String senderNumber, String mensaje, int idplantilla, List<JsonObject> paramsDefault) {
	    JsonObject requestBody = new JsonObject();
	    JsonArray contactNumbers = new JsonArray();
	    contactNumbers.add("+57" + contacto.get("telefono").getAsString());

	    requestBody.addProperty("senderNumber", senderNumber);
	    requestBody.add("contactNumbers", contactNumbers);

	    if (idplantilla > 0) {
	        requestBody.addProperty("templateId", idplantilla);
	        requestBody.add("params", construirParams(paramsDefault, contacto));
	    } else {
	        requestBody.addProperty("text", mensaje);
	        JsonObject param = new JsonObject();
	        if (contacto.has("nombre") && !contacto.get("nombre").isJsonNull()) {
	            param.addProperty("nombre", contacto.get("nombre").getAsString());
	        } else {
	            param.addProperty("titulo", "Pizza Americana");
	        }
	        requestBody.add("params", param);
	    }

	    return requestBody;
	}
	
	private void ejecutarEnvio(OkHttpClient client, JsonObject requestBody, String apiKey, String telefono, JsonArray errores) throws IOException {
	    Request request = new Request.Builder()
	            .url("https://api.brevo.com/v3/whatsapp/sendMessage")
	            .addHeader("accept", "application/json")
	            .addHeader("api-key", apiKey)
	            .addHeader("content-type", "application/json")
	            .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
	            .build();

	    try (Response response = client.newCall(request).execute()) {
	        if (!response.isSuccessful()) {
	            JsonObject error = new JsonObject();
	            if (telefono != null) error.addProperty("telefono", telefono);
	            error.addProperty("error", response.body().string());
	            errores.add(error);
	        }
	    }
	}




}
