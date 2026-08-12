package capaServicioCC;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import capaControladorCC.TercerizadoDomicilioCtrl;
import capaModeloCC.RespuestaRappiCargo;

@WebServlet("/NotificarPedidoListoRappiCargo")
public class NotificarPedidoListoRappiCargo extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idPedidoParam = request.getParameter("idPedido");

        if (idPedidoParam == null || idPedidoParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"ok\":false,\"message\":\"idPedido es obligatorio\"}");
            return;
        }

        try {

            long idPedido = Long.parseLong(idPedidoParam);

            RespuestaRappiCargo respuestaRappi =
                    TercerizadoDomicilioCtrl.notificarPedidoListo(idPedido);

            JSONObject json = new JSONObject();

            json.put("ok", respuestaRappi.isExito());
            json.put("httpStatus", respuestaRappi.getHttpStatus());
            json.put("mensaje", respuestaRappi.getMensaje());
            json.put("respuestaRappi", respuestaRappi.getBody());

            response.setStatus(
                    respuestaRappi.isExito()
                            ? HttpServletResponse.SC_OK
                            : HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().write(json.toJSONString());

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"ok\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
