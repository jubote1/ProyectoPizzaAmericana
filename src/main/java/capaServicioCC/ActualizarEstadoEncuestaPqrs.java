
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

@WebServlet("/ActualizarEstadoEncuestaPqrs")
public class ActualizarEstadoEncuestaPqrs extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setContentType("application/json; charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        int idsolicitudpqrs = parseIntSafe(req.getParameter("idsolicitudpqrs"));
        boolean envio_encuesta = Boolean.parseBoolean(req.getParameter("envio_encuesta"));

        String respuesta = new SolicitudPQRSCtrl()
                .actualizarEstadoEncPqrs(idsolicitudpqrs, envio_encuesta);

        resp.getWriter().write(respuesta);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

