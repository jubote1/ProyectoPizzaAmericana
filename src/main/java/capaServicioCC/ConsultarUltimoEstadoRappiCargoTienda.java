package capaServicioCC;

import com.fasterxml.jackson.databind.ObjectMapper;
import capaDAOCC.TercerizadoDomicilioEventoDAO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de consulta de Plataformas (Rappi, DiDi, Kuno): invoca
 * TercerizadoDomicilioEventoDAO.consultarUltimoEstadoPorPedidoTienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarUltimoEstadoRappiCargoTienda   Metodos: GET.
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer).
 * Devuelve: cuerpo vacio; claves: error, estado.
 * Lee: pedido, tercerizado_domicilio_evento.
 * Sistemas externos: Rappi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarUltimoEstadoRappiCargoTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarUltimoEstadoRappiCargoTienda")
public class ConsultarUltimoEstadoRappiCargoTienda extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int idPedidoTienda;
        int idTienda;

        try {
            idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
            idTienda = Integer.parseInt(request.getParameter("idtienda"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"estado\":null,\"error\":\"Parámetros inválidos\"}");
            return;
        }

        String estado = TercerizadoDomicilioEventoDAO
                .consultarUltimoEstadoPorPedidoTienda(idPedidoTienda, idTienda);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("estado", estado);

        response.getWriter().write(mapper.writeValueAsString(resultado));
    }
}