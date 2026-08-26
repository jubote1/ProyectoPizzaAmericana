package capaServicioCC;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.TercerizadoDomicilioCtrl;
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
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        /*
         * ============================================================
         * 1. VALIDAR SECRET DEL WEBHOOK
         * ============================================================
         */

        String secretHeader =
                request.getHeader("x-rappi-cargo-secret");

        String webhookSecret =
                ParametrosDAO.retornarValorAlfanumerico("RP_CARGO_SECRET");

        webhookSecret = webhookSecret != null
                ? webhookSecret.trim()
                : "";

        if (webhookSecret.isEmpty()) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().write(
                    "{\"ok\":false,\"message\":\"Webhook secret not configured\"}");

            return;
        }

        if (secretHeader == null
                || !secretHeader.equals(webhookSecret)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);

            response.getWriter().write(
                    "{\"ok\":false,\"message\":\"Unauthorized\"}");

            return;
        }

        /*
         * ============================================================
         * 2. LEER BODY
         * ============================================================
         */

        StringBuilder bodyBuilder = new StringBuilder();

        try (BufferedReader reader = request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }

        String body = bodyBuilder.toString();

        if (body.trim().isEmpty()) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().write(
                    "{\"ok\":false,\"message\":\"Webhook body vacío\"}");

            return;
        }

        JsonNode json;

        try {

            json = mapper.readTree(body);

        } catch (Exception e) {

            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().write(
                    "{\"ok\":false,\"message\":\"JSON inválido\"}");

            return;
        }

        /*
         * ============================================================
         * 3. OBTENER INFORMACIÓN DEL WEBHOOK
         * ============================================================
         */

        String externalOrderId =
                getText(json, "external_order_id");

        String rappiOrderId =
                getText(json, "rappi_order_id");

        String cargoOrderId =
                getText(json, "cargo_order_id");

        String state =
                getText(json, "state");

        String trackingUrl =
                getText(json, "tracking_url");

        String deliveredAt =
                getText(json, "delivered_at");

        String cancelledAt =
                getText(json, "cancelled_at");

        System.out.println("==========================================");
        System.out.println("WEBHOOK RAPPICARGO");
        System.out.println("external_order_id: " + externalOrderId);
        System.out.println("rappi_order_id: " + rappiOrderId);
        System.out.println("cargo_order_id: " + cargoOrderId);
        System.out.println("state: " + state);
        System.out.println("tracking_url: " + trackingUrl);
        System.out.println("delivered_at: " + deliveredAt);
        System.out.println("cancelled_at: " + cancelledAt);
        System.out.println("==========================================");

        /*
         * ============================================================
         * 4. OBTENER ID DEL PEDIDO INTERNO
         *
         * Rappi puede enviar:
         *
         * 12345
         *
         * o incluso:
         *
         * 12345:cargo:duplicate:0
         *
         * Por eso usamos obtenerIdPedido().
         * ============================================================
         */

        long idPedido = obtenerIdPedido(externalOrderId);

        String processResult = null;

        if (idPedido == 0) {

            processResult =
                    "El external_order_id no tiene formato numérico válido: "
                    + externalOrderId;
        }

        /*
         * ============================================================
         * 5. GUARDAR SIEMPRE EL EVENTO
         * ============================================================
         */

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

        /*
         * ============================================================
         * 6. SI NO PUDIMOS IDENTIFICAR EL PEDIDO, NO CONTINUAMOS
         * ============================================================
         */

        if (idPedido == 0) {

            response.setStatus(
                    HttpServletResponse.SC_OK);

            response.getWriter().write(
                    "{\"ok\":true}");

            return;
        }

        /*
         * ============================================================
         * 7. PROCESAR WEBHOOK
         * ============================================================
         */

        try {

            actualizarPedidoSegunEstado(
                    idPedido,
                    state,
                    cargoOrderId,
                    idEvento
            );

        } catch (Exception e) {

            e.printStackTrace();

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            "Error procesando webhook: "
                                    + e.getMessage(),
                            idEvento
                    );
        }

        /*
         * ============================================================
         * 8. RESPONDER A RAPPI
         * ============================================================
         *
         * Importante:
         * El webhook fue recibido correctamente aunque nuestro
         * procesamiento interno haya tenido algún problema.
         *
         * Por eso respondemos HTTP 200.
         * ============================================================
         */

        response.setStatus(
                HttpServletResponse.SC_OK);

        response.getWriter().write(
                "{\"ok\":true}");
    }

    /**
     * Obtiene un campo String del JSON.
     */
    private static String getText(
            JsonNode json,
            String field) {

        if (json == null) {
            return null;
        }

        if (!json.has(field)) {
            return null;
        }

        if (json.get(field).isNull()) {
            return null;
        }

        String value =
                json.get(field).asText();

        return value != null
                ? value.trim()
                : null;
    }

    /**
     * Convierte external_order_id a idpedido.
     *
     * Soporta:
     *
     * 12345
     *
     * y:
     *
     * 12345:cargo:duplicate:0
     */
    private static long obtenerIdPedido(
            String externalOrderId) {

        if (externalOrderId == null
                || externalOrderId.trim().isEmpty()) {

            return 0;
        }

        String valor =
                externalOrderId.trim();

        try {

            return Long.parseLong(valor);

        } catch (NumberFormatException e) {

            /*
             * Ejemplo:
             *
             * 2346828:cargo:duplicate:0
             */

            try {

                String[] partes =
                        valor.split(":");

                if (partes.length > 0) {

                    return Long.parseLong(
                            partes[0].trim());
                }

            } catch (Exception ex) {

                System.out.println(
                        "No fue posible convertir external_order_id: "
                                + externalOrderId);
            }
        }

        return 0;
    }

    /**
     * Guarda el evento recibido desde Rappi Cargo.
     */
    private Long guardarEventoRappi(
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

            idEvento =
                    TercerizadoDomicilioEventoDAO.guardarEvento(
                            idPedido,
                            "RAPPICARGO",
                            cargoOrderId,
                            rappiOrderId,
                            state,
                            trackingUrl,
                            deliveredAt,
                            cancelledAt,
                            payloadJson,
                            processResult
                    );

        } catch (Exception e) {

            e.printStackTrace();
        }

        return idEvento;
    }

    /**
     * Procesa el estado recibido desde Rappi Cargo.
     *
     * IMPORTANTE:
     *
     * Primero se busca el pedido por external_order_id.
     *
     * Después se asocia cargo_order_id con idOrdenComercio.
     *
     * Ya NO buscamos primero el pedido mediante cargo_order_id.
     *
     * Esto evita la condición de carrera entre:
     *
     * crearOrdenRappiCargo()
     *
     * y
     *
     * el webhook de Rappi.
     */
    private void actualizarPedidoSegunEstado(
            long idPedido,
            String state,
            String cargoOrderId,
            Long idEvento) {

        /*
         * ============================================================
         * 1. VALIDAR ESTADO
         * ============================================================
         */

        if (state == null
                || state.trim().isEmpty()) {

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            "El webhook llegó sin estado.",
                            idEvento
                    );

            return;
        }

        /*
         * ============================================================
         * 2. VALIDAR CARGO_ORDER_ID
         * ============================================================
         */

        if (cargoOrderId == null
                || cargoOrderId.trim().isEmpty()) {

            String mensaje =
                    "El webhook llegó sin cargo_order_id.";

            System.out.println(mensaje);

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            mensaje,
                            idEvento
                    );

            return;
        }

        /*
         * ============================================================
         * 3. CONVERTIR CARGO_ORDER_ID
         * ============================================================
         */

        BigInteger idOrdenComercio;

        try {

            idOrdenComercio =
                    new BigInteger(cargoOrderId.trim());

        } catch (NumberFormatException e) {

            String mensaje =
                    "El cargo_order_id no tiene formato numérico válido: "
                            + cargoOrderId;

            System.out.println(mensaje);

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            mensaje,
                            idEvento
                    );

            return;
        }

        /*
         * ============================================================
         * 4. BUSCAR PEDIDO POR IDPEDIDO
         *
         * ESTA ES LA CORRECCIÓN PRINCIPAL.
         *
         * Antes:
         *
         * ConsultaPedidoXOrden(cargo_order_id)
         *
         * Ahora:
         *
         * ConsultaPedido(idPedido)
         *
         * porque external_order_id es el vínculo original con
         * nuestro pedido.
         * ============================================================
         */

        Pedido pedEvento =
                PedidoDAO.ConsultaPedido(
                        (int) idPedido
                );

        if (pedEvento == null
                || pedEvento.getIdpedido() == 0) {

            String mensaje =
                    "No existe un pedido asociado al external_order_id: "
                            + idPedido;

            System.out.println(mensaje);

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            mensaje,
                            idEvento
                    );

            return;
        }

        long idCargo =
                idOrdenComercio.longValue();

  
        /*
         * ============================================================
         * 7. OBTENER TIENDA
         * ============================================================
         */

        int idTiendaPedido = pedEvento.getIdtienda();

        if (idTiendaPedido <= 0 && pedEvento.getTienda() != null) {
            idTiendaPedido = pedEvento.getTienda().getIdTienda();
        }

        if (idTiendaPedido <= 0) {

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            "El pedido no tiene tienda asociada.",
                            idEvento
                    );

            return;
        }

        Tienda tienda =
                TiendaDAO.obtenerTienda(idTiendaPedido);
        
        
        if (tienda == null) {

            TercerizadoDomicilioEventoDAO
                    .actualizarResultadoProcesoPorPedido(
                            idPedido,
                            false,
                            "No se encontró la tienda asociada al pedido.",
                            idEvento
                    );

            return;
        }

        /*
         * ============================================================
         * 8. PROCESAR ESTADO
         * ============================================================
         */

        HttpClient client =
                HttpClientBuilder
                        .create()
                        .build();

        String rutaURL = "";
        String respuesta = "";

        switch (state) {

            /*
             * ========================================================
             * PEDIDO CREADO / BUSCANDO DOMICILIARIO
             * ========================================================
             */

            case "created":
            case "scheduled":
            case "finding_courier":
            case "in_progress":

                /*
                 * Ya tenemos pedEvento.
                 *
                 * NO hacemos nuevamente:
                 *
                 * PedidoDAO.ConsultaPedido(...)
                 */

                if (pedEvento == null
                        || pedEvento.getIdpedido() == 0) {

                    TercerizadoDomicilioEventoDAO
                            .actualizarResultadoProcesoPorPedido(
                                    idPedido,
                                    false,
                                    "No existe el pedido interno.",
                                    idEvento
                            );

                    break;
                }

                /*
                 * En este punto idOrdenComercio ya debe estar asociado.
                 */

                System.out.println(
                        "Webhook "
                                + state
                                + " procesado correctamente."
                                + " Pedido: "
                                + pedEvento.getIdpedido()
                                + ", cargo_order_id: "
                                + idCargo);

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                true,
                                "OK",
                                idEvento
                        );

                break;

            /*
             * ========================================================
             * ESTADOS QUE NO REQUIEREN ACCIÓN LOCAL
             * ========================================================
             */

            case "in_store":
            case "arrive":
            case "pending_review":
            case "return_in_store":

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                true,
                                "OK",
                                idEvento
                        );

                break;

            /*
             * ========================================================
             * DOMICILIARIO EN RUTA
             * ========================================================
             */

            case "on_the_route":

                rutaURL =
                        tienda.getUrl()
                                + "DarSalidaDomicilioPlataforma?idpedido="
                                + pedEvento.getNumposheader()
                                + "&idusuario=180&usuario=Caja";

                try {

                    HttpGet request =
                            new HttpGet(rutaURL);

                    HttpResponse response =
                            client.execute(request);

                    if (response.getEntity() != null) {

                        BufferedReader rd =
                                new BufferedReader(
                                        new InputStreamReader(
                                                response.getEntity()
                                                        .getContent()
                                        )
                                );

                        StringBuilder retorno =
                                new StringBuilder();

                        String line;

                        while ((line = rd.readLine()) != null) {
                            retorno.append(line);
                        }

                        rd.close();

                        respuesta =
                                retorno.toString();
                    }

                    PedidoDAO.marcarDomiciliarioPlataforma(
                            idOrdenComercio
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                    TercerizadoDomicilioEventoDAO
                            .actualizarResultadoProcesoPorPedido(
                                    idPedido,
                                    false,
                                    "Error consumiendo "
                                            + "DarSalidaDomicilioPlataforma: "
                                            + e.getMessage(),
                                    idEvento
                            );

                    break;
                }

                if (respuesta.isEmpty()) {

                    TercerizadoDomicilioEventoDAO
                            .actualizarResultadoProcesoPorPedido(
                                    idPedido,
                                    false,
                                    "El servicio "
                                            + "DarSalidaDomicilioPlataforma "
                                            + "respondió vacío.",
                                    idEvento
                            );

                    break;
                }

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                true,
                                "OK",
                                idEvento
                        );

                break;

            /*
             * ========================================================
             * PEDIDO ENTREGADO
             * ========================================================
             */

            case "delivered_to_user":

                rutaURL =
                        tienda.getUrl()
                                + "DarEntregaDomicilio?idpedidotienda="
                                + pedEvento.getNumposheader()
                                + "&claveusuario=2&idtienda="
                                + tienda.getIdTienda()
                                + "&observacion=PedidoEntregadoPorRappi";

                try {

                    HttpGet request =
                            new HttpGet(rutaURL);

                    HttpResponse response =
                            client.execute(request);

                    if (response.getEntity() != null) {

                        BufferedReader rd =
                                new BufferedReader(
                                        new InputStreamReader(
                                                response.getEntity()
                                                        .getContent()
                                        )
                                );

                        StringBuilder retorno =
                                new StringBuilder();

                        String line;

                        while ((line = rd.readLine()) != null) {
                            retorno.append(line);
                        }

                        rd.close();

                        respuesta =
                                retorno.toString();
                    }

                    PedidoDAO.marcarEntregadoPlataforma(
                            idOrdenComercio
                    );

                } catch (Exception e) {

                    e.printStackTrace();

                    TercerizadoDomicilioEventoDAO
                            .actualizarResultadoProcesoPorPedido(
                                    idPedido,
                                    false,
                                    "Error consumiendo "
                                            + "DarEntregaDomicilio: "
                                            + e.getMessage(),
                                    idEvento
                            );

                    break;
                }

                if (respuesta.isEmpty()) {

                    TercerizadoDomicilioEventoDAO
                            .actualizarResultadoProcesoPorPedido(
                                    idPedido,
                                    false,
                                    "El servicio "
                                            + "DarEntregaDomicilio "
                                            + "respondió vacío.",
                                    idEvento
                            );

                    break;
                }

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                true,
                                "OK",
                                idEvento
                        );

                break;

            /*
             * ========================================================
             * CANCELADO
             * ========================================================
             */

            case "canceled":
                /*
                 * Sin importar el motivo, toda cancelación de RappiCargo desmarca
                 * el pedido como tercerizado (principal y tienda). El resto del
                 * flujo de cancelación sigue igual que hasta ahora.
                 */
            	TercerizadoDomicilioCtrl.desmarcarTercerizado(pedEvento, tienda, idEvento);

                boolean ordenCancelada =
                        PedidoDAO.validarCancelacionPlataforma(idOrdenComercio);
                if (!ordenCancelada) {
                    PedidoDAO.marcarCancelacionPlataforma(idOrdenComercio);
                    String mensaje =
                            ParametrosDAO.retornarValorAlfanumerico("MSG_CANCELACION");
                    if (mensaje == null) {
                        mensaje = "Pedido cancelado por tercero.";
                    }
                    mensaje = mensaje.replace("{nombreOrigen}", "TERCERO");
                    mensaje = mensaje.replace("{orderComercio}", cargoOrderId);
                    mensaje = mensaje.replace(
                            "{orderInterno}", String.valueOf(pedEvento.getIdpedido()));

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
                            pedEvento.getIdtienda(), notificacion);
                }
                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido, true, "OK", idEvento);
                break;

            /*
             * ========================================================
             * ESTADOS EXCEPCIONALES
             * ========================================================
             */

            case "failed_delivery":
            case "returned":
            case "failed_return":

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                true,
                                "Webhook procesado correctamente. "
                                        + "El pedido finalizó con un estado "
                                        + "excepcional: "
                                        + state,
                                idEvento
                        );

                break;

            /*
             * ========================================================
             * ESTADO NO SOPORTADO
             * ========================================================
             */

            default:

                TercerizadoDomicilioEventoDAO
                        .actualizarResultadoProcesoPorPedido(
                                idPedido,
                                false,
                                "Estado no soportado recibido desde "
                                        + "RappiCargo: "
                                        + state,
                                idEvento
                        );

                break;
        }
    }
}