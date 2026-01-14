package capaServicioCC;


import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import capaControladorCC.PedidoCtrl;
import capaModeloCC.AlertaEntregaDom;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/RegistroAlertaEntregaDom")
public class RegistroAlertaEntregaDom extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🔧 IMPORTANTE: Configurar correctamente la codificación de entrada
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        // Leer parámetros del request
        int idPedido = Integer.parseInt(request.getParameter("idpedido"));
        String clave_dom = request.getParameter("clave_dom");
        int idTienda = Integer.parseInt(request.getParameter("idtienda"));
        String descripcion = request.getParameter("descripcion");

        boolean error = Boolean.parseBoolean(request.getParameter("error"));
        Double lat_dom = Double.parseDouble(request.getParameter("latitud_dom"));
        Double long_dom = Double.parseDouble(request.getParameter("longitud_dom"));
        Double lat_cli = Double.parseDouble(request.getParameter("latitud_cli"));
        Double long_cli = Double.parseDouble(request.getParameter("longitud_cli"));


        String param_novedad = request.getParameter("con_novedad");

        boolean con_novedad;

        if (param_novedad == null) {
            // No enviaron el parámetro
            con_novedad = true;
        } else {
            con_novedad = Boolean.parseBoolean(param_novedad);
        }

        // Lógica de guardado
        PedidoCtrl PedidoCtrl = new PedidoCtrl();
        AlertaEntregaDom alertaEntregaDom = new AlertaEntregaDom(idPedido, clave_dom, descripcion, error, lat_dom, long_dom, lat_cli, long_cli, idTienda,con_novedad);
        String respuesta = PedidoCtrl.registrarAlertaEntregaDom(alertaEntregaDom);

        // Enviar respuesta
        PrintWriter out = response.getWriter();
        out.print(respuesta);
        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET no soportado. Use POST con JSON.");
    }
}
