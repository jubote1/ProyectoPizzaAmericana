package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import capaModeloCC.ComentarioPqrs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import capaControladorCC.SolicitudPQRSCtrl;
import com.google.gson.reflect.TypeToken;

/**
 * Servicio que modifica datos de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.insertarSolicitudPQRS.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarSolicitudPQRS   Metodos: GET, POST.
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): listaComentarios (string), fechasolicitud (string), tiposolicitud (string),
 * nombre (string), apellido (string), telefono (string), direccion (string), zona (string), tipo
 * (string), arearesponsable (string), descuentoRedimido (boolean), ccVinculado (boolean), correo
 * (string).
 * Devuelve: cuerpo vacio; claves: idSolicitudPQRS, mail.smtp.auth, mail.smtp.host, mail.smtp.port,
 * mail.smtp.ssl.protocols, mail.smtp.starttls.enable, message, registroComentarios, success.
 * Lee: parametros, pqrs_comentario.
 * Escribe: pqrs_comentario (DELETE/INSERT/UPDATE), solicitudpqrs (INSERT).
 * Sistemas externos: Correo SMTP.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarSolicitudPQRS.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarSolicitudPQRS")
public class InsertarSolicitudPQRS extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setContentType("application/json; charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        Type listType = new TypeToken<List<ComentarioPqrs>>() {}.getType();
        List<ComentarioPqrs> listaComentarios = new Gson().fromJson(req.getParameter("listaComentarios"), listType);

        String respuesta = new SolicitudPQRSCtrl().insertarSolicitudPQRS(
            req.getParameter("fechasolicitud"),
            req.getParameter("tiposolicitud"),
            parseInt(req, "idcliente"),
            parseInt(req, "idtienda"),
            req.getParameter("nombre"),
            req.getParameter("apellido"),
            req.getParameter("telefono"),
            req.getParameter("direccion"),
            req.getParameter("zona"),
            parseInt(req, "idmunicipio"),
            "", // comentario vacío
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
            req.getParameter("correo")
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

