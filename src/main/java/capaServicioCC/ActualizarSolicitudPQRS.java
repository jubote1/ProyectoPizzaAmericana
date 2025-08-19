package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.ComentarioPqrs;

@WebServlet("/ActualizarSolicitudPQRS")
public class ActualizarSolicitudPQRS extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setContentType("application/json; charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();

        String listaComentariosStr = req.getParameter("listaComentarios");
        Type listType = new TypeToken<List<ComentarioPqrs>>() {}.getType();
        List<ComentarioPqrs> listaComentarios = new Gson().fromJson(listaComentariosStr, listType);

        String respuesta = solicitudCtrl.actualizarSolicitudPQRS(
            parseInt(req, "idsolicitudpqrs"),
            req.getParameter("fechasolicitud"),
            req.getParameter("tiposolicitud"),
            parseInt(req, "idcliente"),
            parseInt(req, "idtienda"),
            req.getParameter("nombres"),
            req.getParameter("apellidos"),
            req.getParameter("telefono"),
            req.getParameter("direccion"),
            req.getParameter("zona"),
            parseInt(req, "idmunicipio"),
            "", // Comentario vacío
            parseInt(req, "idorigen"),
            parseInt(req, "idfoco"),
            req.getParameter("tipo"),
            req.getParameter("arearesponsable"),
            parseInt(req, "idpedidotienda"),
            parseDouble(req, "valorPedido"),
            parseDouble(req, "valorDescuento"),
            parseInt(req, "porcentajeDescuento"),
            Boolean.parseBoolean(req.getParameter("descuentoRedimido")),
            parseInt(req, "idpedidoredencion"),
            listaComentarios,
            parseInt(req, "idusuarioRegistro"),
            parseInt(req, "idusuarioRedencion"),
            parseInt(req, "idestado"),
            parseInt(req, "idprioridad"),
            parseInt(req, "idmotivo"),
            Boolean.parseBoolean(req.getParameter("ccVinculado")),
            req.getParameter("correo"),
            Boolean.parseBoolean(req.getParameter("envio_encuesta"))
        );

        resp.getWriter().write(respuesta);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    private int parseInt(HttpServletRequest req, String name) {
        try { return Integer.parseInt(req.getParameter(name)); } catch (Exception e) { return 0; }
    }

    private double parseDouble(HttpServletRequest req, String name) {
        try { return Double.parseDouble(req.getParameter(name)); } catch (Exception e) { return 0.0; }
    }
}
