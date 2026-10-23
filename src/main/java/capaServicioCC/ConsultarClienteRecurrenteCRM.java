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
 * Dice si el telefono del lead corresponde a un cliente conocido y lo escribe en el lead.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarClienteRecurrenteCRM   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe: cuerpo de la peticion y cabecera authorization.
 * Devuelve: text/plain;charset=UTF-8; claves: mail.smtp.host, mail.smtp.ssl.protocols.
 * Lee: cliente, integracion_crm, municipio, nomenclatura_direccion, parametros, pedido,
 * pedido_anulado, tienda, tienda_bloqueada.
 * Escribe: cliente (UPDATE), log_pedido_crmbot (INSERT/UPDATE).
 * Sistemas externos: ArcGIS, Correo SMTP, GloriaFood/Kuno, Gmail, Kommo (CRM).
 *
 * Trampas y riesgos conocidos:
 * - No valida la cabecera Authorization (ver la entrada comun).
 * - Responde OK aunque el trabajo posterior falle: el error queda solo en el log del servidor.
 * - Comparte un pool fijo de 5 hilos con sus propias llamadas.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarClienteRecurrenteCRM.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarClienteRecurrenteCRM")
public class ConsultarClienteRecurrenteCRM extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public ConsultarClienteRecurrenteCRM() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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

            final String data = sb.toString();

            String authHeader = request.getHeader("authorization");
            if (authHeader == null) {
                authHeader = "HTTP Authorization header: No authorization header";
            }

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
                        pedidoCtrl.consultarClienteRecurrenteCRMBOT(data, authHeaderFinal);
                    } catch (Exception e) {
                        System.out.println("Error procesando ConsultarClienteRecurrenteCRM: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            System.out.println("Error recibiendo webhook ConsultarClienteRecurrenteCRM: " + e.getMessage());
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