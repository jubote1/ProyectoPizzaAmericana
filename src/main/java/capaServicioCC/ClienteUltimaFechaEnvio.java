package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import capaControladorCC.SegmentacionClienteCtrl;
import capaDAOCC.SegmentacionClienteDAO;

@WebServlet("/ClienteUltimaFechaEnvio")
public class ClienteUltimaFechaEnvio extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            // Obtener los par�metros desde el JSON
            JsonArray idsClientesArray = jsonObject.has("idsClientes") ? jsonObject.getAsJsonArray("idsClientes") : null;

            // Validar que los IDs no sean null ni vac�os
            if (idsClientesArray == null || idsClientesArray.size() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print("{\"error\": \"Faltan parametros obligatorios o valores invalidos\"}");
                return;
            }

            // Convertir el array de idsClientes a un arreglo de enteros (int)
            List<Integer> idsClientes = new ArrayList<>();
            for (JsonElement idElement : idsClientesArray) {
                idsClientes.add(idElement.getAsInt());
            }

            Map<String, Object> resultado = SegmentacionClienteDAO.actualizarFechaUltimaPublicidad(idsClientes);

            boolean success = (boolean) resultado.get("success");
            String mensaje = (String) resultado.get("message");

            // Devolver la respuesta
            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("{\"success\": true, \"message\": \"" + mensaje + "\"}");
            } else {

                List<Integer> clientesFallidos = (List<Integer>) resultado.get("clientesFallidos");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print(
                    "{\"success\": false, \"message\": \"" + mensaje +
                    "\", \"clientesFallidos\": \"" + clientesFallidos + "\"}"
                );
            }

        } catch (JsonSyntaxException e) {
            System.err.println("Error de JSON: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Formato JSON invalido\"}");
        } catch (Exception e) {
            e.printStackTrace(); // <-- MUY IMPORTANTE
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}

