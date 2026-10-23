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
 * Servicio de consulta de Reportes y estadisticas: invoca ClientesHttp.obtenerEstadoPoolsJSON.
 *
 * Ruta: /ProyectoPizzaAmericana/EstadoPoolHttp   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: apache_general, apache_redirecciones, conexiones_totales,
 * disponibles, disponibles_ociosas, en_cola, en_cola_esperando, en_uso, en_uso_activas, estado,
 * host, max_por_host, ....
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarEstadoPoolHttp.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
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
