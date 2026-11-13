package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EventoEspecialCtrl;

/**
 * Mueve un evento especial de estado: de TENTATIVO a APARTADO cuando el cliente
 * abona, a CONFIRMADO cuando la tienda lo cierra, a ENTREGADO el dia del evento,
 * o a CANCELADO si se cae. No borra nada.
 *
 * Ruta: /ProyectoPizzaAmericana/CambiarEstadoEventoEspecial
 * Solo POST. Parametros: idevento, estado, usuario
 */
@WebServlet("/CambiarEstadoEventoEspecial")
public class CambiarEstadoEventoEspecial extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final List<String> ESTADOS =
        Arrays.asList("TENTATIVO", "APARTADO", "CONFIRMADO", "ENTREGADO", "CANCELADO");

    public CambiarEstadoEventoEspecial() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();

        String idStr = request.getParameter("idevento");
        String estado = request.getParameter("estado");
        String usuario = request.getParameter("usuario");

        int idevento;
        try {
            idevento = Integer.parseInt(idStr == null ? "" : idStr.trim());
        } catch (NumberFormatException e) {
            out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Falta el id del evento.\"}");
            return;
        }

        if (estado == null || !ESTADOS.contains(estado.trim().toUpperCase())) {
            out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Estado invalido. Los validos son: " + ESTADOS + ".\"}");
            return;
        }

        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = "asesor_cc";
        }

        EventoEspecialCtrl ctrl = new EventoEspecialCtrl();
        out.write(ctrl.cambiarEstadoJson(idevento, estado.trim(), usuario.trim()));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("{\"resultado\":\"ERROR\",\"mensaje\":\"Este servicio solo acepta POST.\"}");
    }
}
