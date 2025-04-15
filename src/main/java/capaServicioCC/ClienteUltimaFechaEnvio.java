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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import capaControladorCC.SegmentacionClienteCtrl;

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

            // Obtener los parámetros desde el JSON
            JsonArray idsClientesArray = jsonObject.has("idsClientes") ? jsonObject.getAsJsonArray("idsClientes") : null;

            // Validar que los IDs no sean null ni vacíos
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

            // Llamar al controlador para actualizar la fecha de la última publicidad
            boolean resultado = SegmentacionClienteCtrl.actualizarFechaUltimaPublicidad(idsClientes);

            // Devolver la respuesta
            if (resultado) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print("{\"success\": true, \"message\": \"Fecha de ultima publicidad actualizada\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print("{\"error\": \"Hubo un error al actualizar la fecha de ultima publicidad\"}");
            }

        } catch (JsonSyntaxException e) {
            System.err.println("Error de JSON: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().print("{\"error\": \"Formato JSON invalido\"}");
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

