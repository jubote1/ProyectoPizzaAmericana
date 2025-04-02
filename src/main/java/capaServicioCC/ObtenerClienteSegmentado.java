package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import capaControladorCC.SegmentacionClienteCtrl;

@WebServlet("/ObtenerClienteSegmentado")
public class ObtenerClienteSegmentado extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);


            String fechaInicio = jsonObject.has("fechaInicio") ? jsonObject.get("fechaInicio").getAsString().trim() : null;
            String fechaMaxima = jsonObject.has("fechaMaxima") ? jsonObject.get("fechaMaxima").getAsString().trim() : null;
            Integer minPedidos = jsonObject.has("minPedidos") ? jsonObject.get("minPedidos").getAsInt() : null;


            // Validar que los valores no sean null ni vacíos, y que minPedidos sea mayor que 0
            if (fechaInicio == null || fechaInicio.isEmpty() ||
                fechaMaxima == null || fechaMaxima.isEmpty() ||
                minPedidos == null) {

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\": \"Faltan parámetros obligatorios o valores inválidos\"}");
                return;
            }


            List<Integer> excepciones = new ArrayList<>();
            if (jsonObject.has("excepciones") && jsonObject.get("excepciones").isJsonArray()) {
                for (JsonElement elem : jsonObject.get("excepciones").getAsJsonArray()) {
                    excepciones.add(elem.getAsInt());
                }
            }
  

            List<Integer> tiendas = new ArrayList<>();
            if (jsonObject.has("tiendas") && jsonObject.get("tiendas").isJsonArray()) {
                for (JsonElement elem : jsonObject.get("tiendas").getAsJsonArray()) {
                	tiendas.add(elem.getAsInt());
                }
            }

            // Llamar al controlador
            String respuesta = SegmentacionClienteCtrl.obtenerClientesFiltrados(fechaInicio, fechaMaxima, minPedidos, excepciones, tiendas);
            
 

            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
    		out.write(respuesta);

        } catch (JsonSyntaxException e) {
            System.err.println("Error de JSON: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Formato JSON inválido\"}");
        } catch (Exception e) {
            System.err.println("Error interno: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\": \"Error interno del servidor\"}");
        }
    }

    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
}

