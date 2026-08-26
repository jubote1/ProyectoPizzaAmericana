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

        String idPedidoParam = request.getParameter("idPedidoTienda");
        String idTiendaParam = request.getParameter("idTienda");

        if (idPedidoParam == null || idPedidoParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"exito\":false,\"message\":\"idPedido es obligatorio\"}");
            return;
        }
        
        
        if (idTiendaParam == null || idTiendaParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"exito\":false,\"message\":\"idTienda es obligatorio\"}");
            return;
        }

        try {

            int idPedidoTienda = Integer.parseInt(idPedidoParam);
            int idTienda = Integer.parseInt(idTiendaParam);
            
            System.out.println("idPedidoTienda: " + idPedidoTienda);
            System.out.println("idTienda: " + idTienda);


            RespuestaRappiCargo respuestaRappi =
                    TercerizadoDomicilioCtrl.notificarPedidoListo(idPedidoTienda, idTienda);

            JSONObject json = new JSONObject();

            json.put("exito", respuestaRappi.isExito());
            json.put("httpStatus", respuestaRappi.getHttpStatus());
            json.put("mensaje", respuestaRappi.getMensaje());
            json.put("respuestaRappi", respuestaRappi.getBody());

            response.setStatus(
                    respuestaRappi.isExito()
                            ? HttpServletResponse.SC_OK
                            : HttpServletResponse.SC_BAD_REQUEST);

            response.getWriter().write(json.toJSONString());

        } catch (NumberFormatException e) {

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                "{\"exito\":false,"
                + "\"message\":\"idPedidoTienda='" + idPedidoParam
                + "', idTienda='" + idTiendaParam
                + "' no son valores numéricos válidos.\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"exito\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
