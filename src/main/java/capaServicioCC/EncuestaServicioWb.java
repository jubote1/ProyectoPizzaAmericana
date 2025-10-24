package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import capaControladorCC.CandidatoCtrl;
import capaControladorCC.EncuestaCtrl;
import capaControladorCC.VacanteCtrl;
import capaModeloCC.Candidato;
import capaModeloCC.EncuestaServicio;
import capaModeloCC.ExperienciaCandidato;
import capaModeloCC.FormacionCandidato;

import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;

@WebServlet("/EncuestaServicioWb")
public class EncuestaServicioWb extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private EncuestaCtrl encuestaCtrl;
    private static final Logger logger = Logger.getLogger(EncuestaServicioWb.class.getName());

    @Override
    public void init() throws ServletException {
        encuestaCtrl = new EncuestaCtrl();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Leer cuerpo JSON
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }
            String json = jsonBuilder.toString();

            if (json == null || json.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"JSON vacío o inválido.\"}");
                return;
            }

            Gson gson = new Gson();
            EncuestaServicio encuesta = gson.fromJson(json, EncuestaServicio.class);

            JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
            String accion = jsonObj.has("type") ? jsonObj.get("type").getAsString() : "survey";

            logger.info("📩 Encuesta recibida: " + gson.toJson(encuesta) + " | Acción: " + accion);

            String respuesta;
            switch (accion) {
                case "roulette":
                    respuesta = encuestaCtrl.resultadoRuleta(encuesta);
                    break;
                default:
                    respuesta = encuestaCtrl.insertarEncuestaServicioWb(encuesta);
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(respuesta);
            }

        } catch (Exception e) {
            logger.severe("❌ Error procesando la solicitud: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno del servidor.\"}");
        }
    }
}


    
