package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.SolicitudPQRSImagenes;


/**
 * Servicio que modifica datos de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.eliminarSolicitudPQRSImagen.
 *
 * Ruta: /ProyectoPizzaAmericana/EliminarSolicitudPQRSImagenes   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idimagen (string).
 * Devuelve: text/plain; claves: respuesta.
 * Lee: solicitudpqrs_imagenes.
 * Escribe: solicitudpqrs_imagenes (DELETE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha EliminarSolicitudPQRSImagenes.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/EliminarSolicitudPQRSImagenes")
public class EliminarSolicitudPQRSImagenes extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public EliminarSolicitudPQRSImagenes() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/plain");

        Logger logger = Logger.getLogger("log_file");
        PrintWriter out = response.getWriter();

        String respuesta;
        int idimagen = 0;

        try {
            String idimagenParam = request.getParameter("idimagen");
            idimagen = Integer.parseInt(idimagenParam);
        } catch (Exception e) {
            logger.error("Error al parsear idimagen: " + e.toString());
            // Continúa con idimagen = 0
        }

        SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
        respuesta = solicitudCtrl.eliminarSolicitudPQRSImagen(idimagen);

        out.write(respuesta);
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
