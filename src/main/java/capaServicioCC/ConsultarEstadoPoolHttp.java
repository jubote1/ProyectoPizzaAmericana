package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utilidadesCC.ClientesHttp;

/**
 * Endpoint de observabilidad en tiempo real para el pool de conexiones HTTP.
 * Permite monitorear desde navegador, Postman o curl la salud del pool:
 * - Conexiones en uso (leased)
 * - Conexiones disponibles (available)
 * - Peticiones en cola de espera (pending)
 * - Detalle por host/tienda activo
 */
@WebServlet("/EstadoPoolHttp")
public class ConsultarEstadoPoolHttp extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public ConsultarEstadoPoolHttp() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        try (PrintWriter out = response.getWriter()) {
            String jsonRespuesta = ClientesHttp.obtenerEstadoPoolsJSON();
            out.write(jsonRespuesta);
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
