package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaDAOCC.LogPedidoVirtualKunoDAO;

@WebServlet("/RappiEventos")
public class RappiEventos extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public RappiEventos() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");

        StringBuilder sb = new StringBuilder();
        String line;

        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(request.getInputStream(), "UTF-8"));

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String data = sb.toString();

            String authHeader = request.getHeader("authorization");
            if (authHeader == null) {
                authHeader = "No authorization header";
            }

            int idLog = LogPedidoVirtualKunoDAO.insertarLogRAPPI(data, authHeader );
            
            try {
                PedidoCtrl pedidoCtrl = new PedidoCtrl();
                pedidoCtrl.procesarEventoRappi(data);
            } catch (Exception e) {
                System.out.println("Error procesando evento Rappi: " + e.getMessage());
            }


            PrintWriter out = response.getWriter();
            response.setStatus(200);
            out.write("{\"status\":\"OK\",\"idLog\":" + idLog + "}");

        } catch (Exception e) {
            e.printStackTrace();


            response.setStatus(200);
            response.getWriter().write("{\"status\":\"ERROR_CONTROLADO\"}");
        }
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
