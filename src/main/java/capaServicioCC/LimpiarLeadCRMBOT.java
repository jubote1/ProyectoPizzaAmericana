package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

@WebServlet("/LimpiarLeadCRMBOT")
public class LimpiarLeadCRMBOT extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Pool de hilos para procesar las solicitudes en segundo plano
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public LimpiarLeadCRMBOT() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {

            response.addHeader("Access-Control-Allow-Origin", "*");
            response.setContentType("application/json;charset=UTF-8");

            // Leer el body
            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            final String data = sb.toString();

            // Obtener Authorization
            String authHeader = request.getHeader("authorization");
            if (authHeader == null) {
                authHeader = "";
            }

            final String auth = authHeader;

            // Responder inmediatamente
            response.setStatus(HttpServletResponse.SC_OK);

            PrintWriter out = response.getWriter();
            out.write("{}");
            out.flush();

            // Procesar en segundo plano
            executor.submit(() -> {
                try {
                    PedidoCtrl pedidoCtrl = new PedidoCtrl();
                    String respuesta = pedidoCtrl.limpiarLeadCRMBOT(data, auth);

                    System.out.println("Proceso terminado: " + respuesta);

                } catch (Exception e) {
                    System.err.println("Error procesando limpiarLeadCRMBOT:");
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {

            e.printStackTrace();

            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{}");
                response.getWriter().flush();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        doGet(request, response);
    }

    @Override
    public void destroy() {
        executor.shutdown();
        super.destroy();
    }
}