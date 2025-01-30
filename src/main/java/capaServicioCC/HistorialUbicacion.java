package capaServicioCC;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.DomiciliarioPedidoDAO;

@WebServlet("/HistorialUbicacion")
public class HistorialUbicacion extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }

    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int tiendaId = parseTiendaId(request.getParameter("tiendaId"));
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String claveRapida = request.getParameter("claveRapida");
        String action = request.getParameter("action");

        
        System.out.println("tiendaId: " + tiendaId);
        System.out.println("startDate: " + startDate);
        System.out.println("endDate: " + endDate);
        System.out.println("claveRapida: " + claveRapida);
        System.out.println("action: " + action);
        
        
        try {
            JSONArray jsonResponse = new JSONArray();
            if ("rastreo".equals(action)) {
                List<JSONObject> rastreo = DomiciliarioPedidoDAO.HistorialActualUbicacion(tiendaId);
                jsonResponse.addAll(rastreo);
            } else if ("historial".equals(action)) {
                List<JSONObject> historial = DomiciliarioPedidoDAO.HistorialUsuariosPorFecha(tiendaId, startDate, endDate);
                jsonResponse.addAll(historial);
            } else if ("detalle".equals(action)) {
            	 List<JSONObject> detalle_historial = DomiciliarioPedidoDAO.DetalleHistorialUsuariosPorFecha(tiendaId, startDate, claveRapida);
                 jsonResponse.addAll(detalle_historial);
            }

            response.setContentType("application/json");
            response.getWriter().write(jsonResponse.toJSONString());
        } catch (SQLException e) {
            handleError(response, "Error al obtener el historial de ubicaciones.");
        }
    }

    private int parseTiendaId(String tiendaIdParam) {
        try {
            return (tiendaIdParam != null && !tiendaIdParam.isEmpty()) ? Integer.parseInt(tiendaIdParam) : 0;
        } catch (NumberFormatException e) {
            return 0; // Valor predeterminado
        }
    }

    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write(errorMessage);
    }
}
