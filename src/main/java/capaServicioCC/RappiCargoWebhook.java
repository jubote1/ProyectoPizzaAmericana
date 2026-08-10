package capaServicioCC;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaControladorCC.PedidoCtrl;
import capaDAOCC.ParametrosDAO;
import capaDAOCC.PedidoDAO;
import capaDAOCC.TercerizadoDomicilioEventoDAO;
import capaDAOCC.TiendaDAO;
import capaModeloCC.Notificaciones;
import capaModeloCC.Pedido;
import capaModeloCC.Tienda;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

@WebServlet("/api/rappi-cargo/webhook/status")
public class RappiCargoWebhook extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String secretHeader = request.getHeader("x-rappi-cargo-secret");

        String webhookSecret = ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_SECRET");
        webhookSecret = webhookSecret != null ? webhookSecret : "";

        if (webhookSecret.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"ok\":false,\"message\":\"Webhook secret not configured\"}");
            return;
        }

        if (secretHeader == null || !secretHeader.equals(webhookSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"ok\":false,\"message\":\"Unauthorized\"}");
            return;
        }

        StringBuilder bodyBuilder = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }

        String body = bodyBuilder.toString();
        JsonNode json = mapper.readTree(body);

        String externalOrderId = getText(json, "external_order_id");
        String rappiOrderId = getText(json, "rappi_order_id");
        String cargoOrderId = getText(json, "cargo_order_id");
        String state = getText(json, "state");
        String trackingUrl = getText(json, "tracking_url");
        String deliveredAt = getText(json, "delivered_at");
        String cancelledAt = getText(json, "cancelled_at");
        long idPedido = 0;
        String processResult = null;

        try {

            idPedido = Long.parseLong(externalOrderId);

        } catch (NumberFormatException e) {

            try {

                idPedido = Long.parseLong(externalOrderId.split(":")[0]);

            } catch (Exception ex) {

                System.out.println("El external_order_id no tiene formato numérico: " + externalOrderId);

                processResult = "El external_order_id no tiene formato numérico: " + externalOrderId;
            }
        }

        Long idEvento = guardarEventoRappi(
                idPedido,
                rappiOrderId,
                cargoOrderId,
                state,
                trackingUrl,
                deliveredAt,
                cancelledAt,
                body,
                processResult
        );
        
     // Solo se procesa el pedido si fue posible obtener un id interno válido.
        if (idPedido != 0) {
            try {
                actualizarPedidoSegunEstado(idPedido, state, cargoOrderId,idEvento);
            } catch (Exception e) {
                e.printStackTrace();

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        idPedido,
                        false,
                        "Error procesando webhook: " + e.getMessage(),idEvento);
            }
        }
        
 
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"ok\":true}");
    }

    private static String getText(JsonNode json, String field) {
        return json.has(field) && !json.get(field).isNull()
                ? json.get(field).asText()
                : null;
    }

    private Long  guardarEventoRappi(
            long idPedido,
            String rappiOrderId,
            String cargoOrderId,
            String state,
            String trackingUrl,
            String deliveredAt,
            String cancelledAt,
            String payloadJson,
            String processResult) {

    	
    	Long idEvento = null;
        try {
        

          

        idEvento = TercerizadoDomicilioEventoDAO.guardarEvento(
                    idPedido,
                    "RAPPICARGO",
                    cargoOrderId,
                    rappiOrderId,
                    state,
                    trackingUrl,
                    deliveredAt,
                    cancelledAt,
                    payloadJson,
                    processResult);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return  idEvento;
    }

    private void actualizarPedidoSegunEstado(long externalOrderId, String state, String cargoOrderId,Long idEvento) {

        if (state == null) {
            TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                    externalOrderId,
                    false,
                    "El webhook llegó sin estado.",idEvento);
            return;
        }

        BigInteger idOrdenComercio = new BigInteger(cargoOrderId);

        Pedido pedEvento = PedidoDAO.ConsultaPedidoXOrden(idOrdenComercio);

        System.out.println(pedEvento);
        if (pedEvento == null || pedEvento.getIdpedido() == 0) {

            String mensaje = "No existe un pedido asociado al cargo_order_id: " + idOrdenComercio;

            System.out.println(mensaje);

            TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                    externalOrderId,
                    false,
                    mensaje,
                    idEvento);

            return;
        }

        if (externalOrderId != pedEvento.getIdpedido()) {

            String mensaje =
                    "Inconsistencia entre external_order_id (" +
                    externalOrderId +
                    ") e id_pedido encontrado (" +
                    pedEvento.getIdpedido() +
                    ")";

            System.out.println(mensaje);

            TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                    externalOrderId,
                    false,
                    mensaje,
                    idEvento);

            return;
        }

        Tienda tienda = TiendaDAO.obtenerTienda(pedEvento.getTienda().getIdTienda());

        if (tienda == null) {

            TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                    externalOrderId,
                    false,
                    "No se encontró la tienda asociada al pedido.",
                    idEvento);

            return;
        }

        HttpClient client = HttpClientBuilder.create().build();

        String rutaURL = "";
        String respuesta = "";

        switch (state) {

            case "created":
            case "scheduled":
            case "finding_courier":
            case "in_progress":

                Pedido pedido = PedidoDAO.ConsultaPedido((int) externalOrderId);

                if (pedido == null || pedido.getIdpedido() == 0) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "No existe el pedido interno.",
                            idEvento);

                    break;
                }

                long idActual = pedido.getIdOrdenComercio();
                long idCargo = idOrdenComercio.longValue();

                if (idActual == 0L) {

                    TercerizadoDomicilioEventoDAO.actualizarPedidoRappiCargo(
                            pedido.getIdpedido(),
                            idOrdenComercio);

                } else if (idActual != idCargo) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "El pedido ya tiene asociado otro idOrdenComercio. Actual: "
                                    + idActual + ", recibido: " + idCargo,
                                    idEvento);

                    break;
                }

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "OK",
                        idEvento);

                break;
                
            case "in_store":
            case "arrive":
            case "pending_review":
            case "return_in_store":

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "OK",
                        idEvento);

                break;

            case "on_the_route":

                rutaURL = tienda.getUrl()
                        + "DarSalidaDomicilioPlataforma?idpedido="
                        + pedEvento.getNumposheader()
                        + "&idusuario=180&usuario=Caja";

                try {

                    HttpGet request = new HttpGet(rutaURL);

                    HttpResponse response = client.execute(request);

                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    StringBuilder retorno = new StringBuilder();

                    String line;

                    while ((line = rd.readLine()) != null) {
                        retorno.append(line);
                    }

                    respuesta = retorno.toString();

                    PedidoDAO.marcarDomiciliarioPlataforma(idOrdenComercio);

                } catch (Exception e) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "Error consumiendo DarSalidaDomicilioPlataforma: " + e.getMessage(),
                            idEvento);

                    break;
                }

                if (respuesta.isEmpty()) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "El servicio DarSalidaDomicilioPlataforma respondió vacío.",
                            idEvento);

                    break;
                }

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "OK",
                        idEvento);

                break;

            case "delivered_to_user":

                rutaURL = tienda.getUrl()
                        + "DarEntregaDomicilio?idpedidotienda="
                        + pedEvento.getNumposheader()
                        + "&claveusuario=2&idtienda="
                        + tienda.getIdTienda()
                        + "&observacion=PedidoEntregadoPorRappi";

                try {

                    HttpGet request = new HttpGet(rutaURL);

                    HttpResponse response = client.execute(request);

                    BufferedReader rd = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent()));

                    StringBuilder retorno = new StringBuilder();

                    String line;

                    while ((line = rd.readLine()) != null) {
                        retorno.append(line);
                    }

                    respuesta = retorno.toString();

                    PedidoDAO.marcarEntregadoPlataforma(idOrdenComercio);

                } catch (Exception e) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "Error consumiendo DarEntregaDomicilio: " + e.getMessage(),
                            idEvento);

                    break;
                }

                if (respuesta.isEmpty()) {

                    TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                            externalOrderId,
                            false,
                            "El servicio DarEntregaDomicilio respondió vacío.",
                            idEvento);

                    break;
                }

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "OK",
                        idEvento);

                break;

            case "canceled":

                boolean ordenCancelada = PedidoDAO.validarCancelacionPlataforma(idOrdenComercio);

                if (!ordenCancelada) {

                    PedidoDAO.marcarCancelacionPlataforma(idOrdenComercio);

                    String mensaje = ParametrosDAO.retornarValorAlfanumerico("MSG_CANCELACION");

                    mensaje = mensaje.replace("{nombreOrigen}", "TERCERO");
                    mensaje = mensaje.replace("{orderComercio}", cargoOrderId);
                    mensaje = mensaje.replace("{orderInterno}", String.valueOf(pedEvento.getIdpedido()));

                    Notificaciones notificacion = new Notificaciones(mensaje);

                    notificacion.setOrigen("TERCERO");
                    notificacion.setIdpedido(pedEvento.getIdpedido());

                    int idNotificacion = PedidoDAO.insertarNotificacion(notificacion);

                    if (idNotificacion > 0) {

                        notificacion.setId(idNotificacion);

                        notificacion.setFechaHora(
                                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        .format(new java.util.Date()));

                        NotificacionSocket.enviarATodos(notificacion);
                    }

                    notificacion.setIdpedido(pedEvento.getNumposheader());

                    PedidoCtrl.registrarNotificacionATiendaAsync(
                            pedEvento.getIdtienda(),
                            notificacion);
                }

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "OK",
                        idEvento);

                break;

            case "failed_delivery":
            case "returned":
            case "failed_return":

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        true,
                        "Webhook procesado correctamente. El pedido finalizó con un estado excepcional: " + state,
                        idEvento);

                break;

            default:

                TercerizadoDomicilioEventoDAO.actualizarResultadoProcesoPorPedido(
                        externalOrderId,
                        false,
                        "Estado no soportado recibido desde RappiCargo: " + state,
                        idEvento);

                break;
        }
    }
}