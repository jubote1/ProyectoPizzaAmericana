package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

@WebServlet("/InsertarPedidoCRMBOT")
public class InsertarPedidoCRMBOT extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public InsertarPedidoCRMBOT() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String data = "";
        String authHeader = "";

        try {
            response.addHeader("Access-Control-Allow-Origin", "*");

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(request.getInputStream(), "UTF-8")
            );

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            authHeader = request.getHeader("authorization");

            if (authHeader == null) {
                authHeader = "HTTP Authorization header: No authorization header";
            }

            final String dataFinal = data;
            final String authHeaderFinal = authHeader;

            // Respuesta inmediata para que Kommo no reintente por timeout.
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("OK");
            response.getWriter().flush();

            // Procesamiento en segundo plano.
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        PedidoCtrl pedidoCtrl = new PedidoCtrl();
                        pedidoCtrl.insertarPedidoCRMBOT(dataFinal, authHeaderFinal);
                    } catch (Exception e) {
                        System.out.println("Error procesando InsertarPedidoCRMBOT: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            System.out.println("Error recibiendo webhook InsertarPedidoCRMBOT: " + e.getMessage());
            e.printStackTrace();

            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("OK");
                response.getWriter().flush();
            }
        }
    }

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