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
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import capaControladorCC.EncuestaCtrl;
import capaModeloCC.EmpleadoEncuesta;
import capaModeloCC.EmpleadoEncuestaDetalle;

@WebServlet("/InsertarDetalleEncuestaTiendasAPP")
public class InsertarDetalleEncuestaTiendasAPP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public InsertarDetalleEncuestaTiendasAPP() {
        super();
    }

    /**
     * Método POST que recibe un JSON con los campos:
     * idempleadoencuesta, idencuestadetalle, valor, observacionadi
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");

        StringBuilder jsonBuffer = new StringBuilder();
        String line;

        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error leyendo el JSON: " + e.getMessage());
            return;
        }
            try {
                JSONArray jsonArray = new JSONArray(jsonBuffer.toString());
                EncuestaCtrl encCtrl = new EncuestaCtrl();


                List<JSONObject>  listaEncuesta  = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
         
                    listaEncuesta.add(jsonObject);
                           
                }
                String respuesta = encCtrl.insertarEmpleadoEncuestaDetalle(listaEncuesta);
                PrintWriter out = response.getWriter();
                out.write(respuesta);


        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error procesando JSON: " + e.getMessage());
        }
    }


    /**
     * Elimina el método GET porque ya no se usa para esta operación.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET no soportado. Use POST con JSON.");
    }
}