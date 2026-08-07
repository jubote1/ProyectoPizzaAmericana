package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;

@WebServlet("/CambiarFormaPagoPedidoApp")
public class CambiarFormaPagoPedidoApp extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public CambiarFormaPagoPedidoApp() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json;charset=UTF-8");
        request.setCharacterEncoding("UTF-8");
        int idPedidoTienda = parseIntSafe(request.getParameter("idpedidotienda"));
        int idTienda = parseIntSafe(request.getParameter("idtienda"));
        String claveUsuario = getParam(request, "claveusuario", "");
        String observacion = getParam(request, "observacion_dom", "");
        String idFormaPagoStr = getParam(request, "idformapago", "1");
        int idFormaPago = parseIntSafe(idFormaPagoStr);

        // Decodificar observación
        try {
            observacion = URLDecoder.decode(observacion, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {}

        // ⛔ Si forma de pago viene en 0 → devolver error directo
        if (idFormaPago == 0) {
            writeJson(response, "error:idformapago_invalido");
            return;
        }

        PedidoCtrl pedidoCtrl = new PedidoCtrl();

        String respuesta = pedidoCtrl.cambiarFormaPagoPedidoApp(
                idPedidoTienda,
                idTienda,
                claveUsuario,
                observacion,
                idFormaPago
        );

        writeJson(response, respuesta);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    // ----------------- Utils -----------------

    private String getParam(HttpServletRequest req, String name, String defecto) {
        String val = req.getParameter(name);
        return (val != null) ? val : defecto;
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeJson(HttpServletResponse response, String text) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(text);
        out.flush();
    }
}
