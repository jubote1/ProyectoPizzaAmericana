package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CampanaNovenasCtrl;

/**
 * Servicio para buscar y filtrar clientes precargados de la Campaña de Novenas y Venta Empresarial 25 Años.
 * 
 * Ruta: /ProyectoPizzaAmericana/BuscarLeadsNovenas
 * Parámetros: filtro (string), clasificacion (string), estado (string), limite (int), idlead (opcional)
 */
@WebServlet("/BuscarLeadsNovenas")
public class BuscarLeadsNovenas extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public BuscarLeadsNovenas() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        String idleadStr = request.getParameter("idlead");
        CampanaNovenasCtrl ctrl = new CampanaNovenasCtrl();
        String jsonRespuesta;

        if (idleadStr != null && !idleadStr.trim().isEmpty()) {
            try {
                int idlead = Integer.parseInt(idleadStr.trim());
                jsonRespuesta = ctrl.obtenerLeadConBitacoraJson(idlead);
            } catch (Exception e) {
                jsonRespuesta = "{\"error\":\"ID de lead inválido\"}";
            }
        } else {
            String filtro = request.getParameter("filtro");
            String clasificacion = request.getParameter("clasificacion");
            String estado = request.getParameter("estado");
            String limiteStr = request.getParameter("limite");
            int limite = 100;
            if (limiteStr != null && !limiteStr.trim().isEmpty()) {
                try { limite = Integer.parseInt(limiteStr.trim()); } catch (Exception ignored) {}
            }
            jsonRespuesta = ctrl.buscarLeadsJson(filtro, clasificacion, estado, limite);
        }

        PrintWriter out = response.getWriter();
        out.write(jsonRespuesta);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
