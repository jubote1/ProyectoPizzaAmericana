package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import capaControladorCC.SegmentacionClienteCtrl;

@WebServlet("/EnvioBrevo")
public class EnvioBrevo extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	request.setCharacterEncoding("UTF-8");
    	response.setContentType("application/json; charset=UTF-8");
    	response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        	String requestBody = reader.lines().collect(Collectors.joining());

            // Parsear el JSON recibido
            JsonObject jsonData = gson.fromJson(requestBody, JsonObject.class);

            // Obtener los parámetros de la solicitud
            JsonArray correosArray = jsonData.getAsJsonArray("correos");
            JsonArray telefonosArray = jsonData.getAsJsonArray("telefonos");
            String asunto = jsonData.has("asunto") ? jsonData.get("asunto").getAsString() : "";

            Integer medio = jsonData.has("medio") ? jsonData.get("medio").getAsInt() : null;
            int idplantilla = (jsonData.has("idplantilla") && !jsonData.get("idplantilla").isJsonNull() && !jsonData.get("idplantilla").getAsString().isEmpty())
            	    ? jsonData.get("idplantilla").getAsInt()
            	    : 0;

            JsonArray paramsDefaultArray = jsonData.has("paramsDefault") ? jsonData.getAsJsonArray("paramsDefault") : new JsonArray();
            List<JsonObject> paramsDefault = StreamSupport.stream(paramsDefaultArray.spliterator(), false)
                    .map(element -> element.getAsJsonObject()).collect(Collectors.toList());
            
            SegmentacionClienteCtrl SegCtrl = new SegmentacionClienteCtrl();
            
            if(medio == null) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "No se recibio ningun medio para accionar.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }else {
            	
            	if(medio == 0) {

                    if (correosArray == null || correosArray.size() == 0) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "No se recibieron correos.");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    } else {
                        // Convertir JsonArray a List<JsonObject>
                        List<JsonObject> correos = StreamSupport.stream(correosArray.spliterator(), false)
                                .map(element -> element.getAsJsonObject()).collect(Collectors.toList());
               
                        jsonResponse = SegCtrl.envioCorreoBrevo(correos, asunto, idplantilla, paramsDefault);
                        registrarEnvio("C", correos, "email", idplantilla, asunto, request, jsonResponse);
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
            	}else {
            	    if (telefonosArray == null || telefonosArray.size() == 0) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "No se recibieron numeros celulares.");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }else {
                        List<JsonObject> telefonos = StreamSupport.stream(telefonosArray.spliterator(), false)
                                .map(element -> element.getAsJsonObject()).collect(Collectors.toList());
                        jsonResponse = SegCtrl.envioWhatsappBrevo(telefonos, asunto, idplantilla, paramsDefault);
                        registrarEnvio("W", telefonos, "telefono", idplantilla, asunto, request, jsonResponse);
                        response.setStatus(HttpServletResponse.SC_OK);
                    	
                    }
            		
            	}
            	
            }
            
  
        } catch (JsonSyntaxException e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error en el formato JSON.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } finally {
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }

    /**
     * Deja constancia de a quien se le acabo de enviar.
     *
     * Va DESPUES del envio y no antes: lo que interesa registrar es lo que de
     * verdad salio, y de paso se puede guardar si Brevo lo acepto o no.
     *
     * Se traga cualquier error. Perder la evidencia de un envio es malo; que
     * falle el envio a tres mil personas porque el registro reviento es mucho
     * peor, y ademas a esta altura Brevo ya lo mando.
     *
     * @param campo "email" o "telefono", que es como viene el destinatario
     */
    private void registrarEnvio(String canal, java.util.List<JsonObject> destinatarios,
            String campo, int idplantilla, String asunto, HttpServletRequest request,
            JsonObject respuestaBrevo) {
        try {
            java.util.ArrayList<capaDAOCC.EnvioBrevoDAO.Destino> lista =
                    new java.util.ArrayList<capaDAOCC.EnvioBrevoDAO.Destino>();
            for (JsonObject o : destinatarios) {
                if (o == null || !o.has(campo) || o.get(campo).isJsonNull()) {
                    continue;
                }
                capaDAOCC.EnvioBrevoDAO.Destino d = new capaDAOCC.EnvioBrevoDAO.Destino();
                d.destino = o.get(campo).getAsString();
                d.nombre = (o.has("name") && !o.get("name").isJsonNull())
                        ? o.get("name").getAsString() : "";
                lista.add(d);
            }
            boolean bien = respuestaBrevo != null && respuestaBrevo.has("success")
                    && respuestaBrevo.get("success").getAsBoolean();
            String detalle = null;
            if (!bien && respuestaBrevo != null && respuestaBrevo.has("message")) {
                detalle = respuestaBrevo.get("message").getAsString();
            }
            capaDAOCC.EnvioBrevoDAO.registrar(canal, lista, idplantilla, asunto,
                    utilidadesCC.AccesoCRM.usuarioEnSesion(request),
                    bien ? "OK" : "ERROR", detalle);
        } catch (Exception e) {
            org.apache.log4j.Logger.getLogger("log_file").error(
                    "EnvioBrevo: no se pudo registrar el envio, " + e.toString());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Método GET no permitido.");
    }
}