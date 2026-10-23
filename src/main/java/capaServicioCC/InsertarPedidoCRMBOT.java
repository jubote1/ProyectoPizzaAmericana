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

/**
 * Crea el pedido completo a partir del lead, lo finaliza y, segun la forma de pago, genera el link
 * de pago o lo envia a la tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarPedidoCRMBOT   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: text/plain;charset=UTF-8; claves: amount_in_cents, collect_shipping, currency,
 * custom_fields_values, description, expires_at, field_id, id, mail.smtp.host,
 * mail.smtp.ssl.protocols, mensaje, name, ....
 * Lee: cliente, detalle_pedido, especialidad, especialidad_excepcion, excepcion_precio,
 * forma_pago, homologacion_forpag_pedidovirtual, homologacion_producto_pedidovirtual,
 * integracion_crm, integracion_crm_homologa_asesor.
 * Escribe: cliente (INSERT/UPDATE), detalle_pedido (INSERT), log_pedido_crmbot (INSERT/UPDATE),
 * pedido (INSERT/UPDATE).
 * Sistemas externos: ArcGIS, Correo SMTP, GloriaFood/Kuno, Gmail, Kommo (CRM), WhatsApp, Wompi.
 *
 * Trampas y riesgos conocidos:
 * - Sin validacion del origen de la llamada (ver la entrada comun).
 * - El pool de 5 hilos es por servicio y la cola no tiene tope: con varios pedidos simultaneos,
 *   el trabajo (Kommo, ArcGIS, Wompi) se encola y el cliente espera mas de lo previsto.
 * - Cualquier error interno devuelve false y solo deja un mensaje en el log del servidor; el
 *   cliente no recibe nada.
 * - Si falla el link de pago, el pedido se cancela: el cliente podria no enterarse.
 * - Hay direcciones de correo fijas en el codigo (alertas).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarPedidoCRMBOT.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
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