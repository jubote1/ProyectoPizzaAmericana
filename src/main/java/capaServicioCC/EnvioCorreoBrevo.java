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

@WebServlet("/EnvioCorreoBrevo")
public class EnvioCorreoBrevo extends HttpServlet {
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
            String asunto = jsonData.has("asunto") ? jsonData.get("asunto").getAsString() : "";

              
            int idplantilla = jsonData.has("idplantilla") ? jsonData.get("idplantilla").getAsInt() : 0;
            JsonArray paramsDefaultArray = jsonData.has("paramsDefault") ? jsonData.getAsJsonArray("paramsDefault") : new JsonArray();

            if (correosArray == null || correosArray.size() == 0) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "No se recibieron correos.");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                // Convertir JsonArray a List<JsonObject>
                List<JsonObject> correos = StreamSupport.stream(correosArray.spliterator(), false)
                        .map(element -> element.getAsJsonObject()).collect(Collectors.toList());
                List<JsonObject> paramsDefault = StreamSupport.stream(paramsDefaultArray.spliterator(), false)
                        .map(element -> element.getAsJsonObject()).collect(Collectors.toList());

                // Llamar al controlador para enviar los correos
                SegmentacionClienteCtrl SegCtrl = new SegmentacionClienteCtrl();
                jsonResponse = SegCtrl.envioCorreoBrevo(correos, asunto, idplantilla, paramsDefault);
                response.setStatus(HttpServletResponse.SC_OK);
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Método GET no permitido.");
    }
}