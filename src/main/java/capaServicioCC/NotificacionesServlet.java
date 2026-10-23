package capaServicioCC;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaDAOCC.PedidoDAO;
import capaModeloCC.Notificaciones;

/**
 * Servicio que modifica datos de Sin clasificar (revisar a mano): invoca
 * PedidoDAO.listarNotificaciones, PedidoDAO.marcarNotificacionesVistas.
 *
 * Ruta: /ProyectoPizzaAmericana/NotificacionesServlet   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): accion (string).
 * Devuelve: cuerpo vacio; claves: fecha_hora, id, idpedido, mensaje, notificado, ok, origen.
 * Lee: notificaciones.
 * Escribe: notificaciones (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha NotificacionesServlet.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/NotificacionesServlet")
public class NotificacionesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = request.getParameter("accion");

        response.setContentType("application/json;charset=UTF-8");

        if ("listar".equalsIgnoreCase(accion)) {
            List<Notificaciones> lista = PedidoDAO.listarNotificaciones();

            response.getWriter().write(convertirListaJson(lista));
            return;
        }

        response.getWriter().write("[]");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accion = request.getParameter("accion");

        response.setContentType("application/json;charset=UTF-8");
        
        if ("marcarTodasVistas".equalsIgnoreCase(accion)) {
            boolean ok = PedidoDAO.marcarNotificacionesVistas();

            response.getWriter().write("{\"ok\":" + ok + "}");
            return;
        }


        response.getWriter().write("{\"ok\":false}");
    }

    private String convertirListaJson(List<Notificaciones> lista) {

        StringBuilder json = new StringBuilder();
        json.append("[");

        for (int i = 0; i < lista.size(); i++) {
            Notificaciones n = lista.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{");
            json.append("\"id\":").append(n.getId()).append(",");
            json.append("\"mensaje\":\"").append(escaparJson(n.getMensaje())).append("\",");
            json.append("\"origen\":\"").append(escaparJson(n.getOrigen())).append("\",");
            json.append("\"idpedido\":").append(n.getIdpedido()).append(",");
            json.append("\"notificado\":\"").append(escaparJson(n.getNotificado())).append("\",");
            json.append("\"fecha_hora\":\"").append(escaparJson(n.getFechaHora())).append("\"");

            json.append("}");
        }

        json.append("]");

        return json.toString();
    }

    private String escaparJson(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", " ");
    }
}
