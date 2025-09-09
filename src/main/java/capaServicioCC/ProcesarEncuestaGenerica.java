package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import capaControladorCC.PedidoCtrl;

@WebServlet("/ProcesarEncuestaGenerica")
public class ProcesarEncuestaGenerica extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ProcesarEncuestaGenerica() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*"); // Si necesitas CORS
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            PrintWriter out = response.getWriter();
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String data = sb.toString();

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.trim().isEmpty()) {
                authHeader = "No Authorization header";
            }

            PedidoCtrl pedidoCtrl = new PedidoCtrl();
            String respuesta = pedidoCtrl.procesarEncuestaGenerica(data, authHeader);

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(respuesta);

        } catch (Exception e) {
            e.printStackTrace(); // Para el log del servidor

            try (PrintWriter out = response.getWriter()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"success\": false, \"message\": \"Error interno: " + e.getMessage().replace("\"", "'") + "\"}");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response); // delegar POST a GET es válido aquí
    }
}
