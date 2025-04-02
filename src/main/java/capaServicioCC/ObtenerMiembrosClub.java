package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
import capaModeloCC.Cliente; // Asegúrate de tener una clase Cliente para representar los datos

@WebServlet("/ObtenerMiembrosClub")
public class ObtenerMiembrosClub extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            // Extraer parámetros
            String estadoMiembro = jsonObject.has("estadoMiembro") ? jsonObject.get("estadoMiembro").getAsString() : null;
            String correoMiembro = jsonObject.has("correoMiembro") ? jsonObject.get("correoMiembro").getAsString() : null;
            int puntosMiembro = jsonObject.has("puntosMiembro") ? jsonObject.get("puntosMiembro").getAsInt() : 0;
            String fechaInicio = jsonObject.has("fechaInicio") ? jsonObject.get("fechaInicio").getAsString().trim() : null;
            String fechaMaxima = jsonObject.has("fechaMaxima") ? jsonObject.get("fechaMaxima").getAsString().trim() : null;
            List<Integer> tiendas = new ArrayList<>();
            if (jsonObject.has("tiendas") && jsonObject.get("tiendas").isJsonArray()) {
                for (JsonElement elem : jsonObject.get("tiendas").getAsJsonArray()) {
                	tiendas.add(elem.getAsInt());
                }
            }
            // Llamar al controlador para obtener la lista de clientes
            SegmentacionClienteCtrl controlador = new SegmentacionClienteCtrl();
            String listaClientes = controlador.obtenerClientesClub(estadoMiembro, correoMiembro, puntosMiembro,fechaMaxima,fechaInicio,tiendas);
            
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.write(listaClientes);
            out.flush();

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().print("{\"error\": \"Método no permitido\"}");
    }
}
