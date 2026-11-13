package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EventoEspecialCtrl;

/**
 * Devuelve los eventos especiales planillados dentro de un rango de fechas, con
 * el resumen (eventos, pizzas, valor y anticipos) que la pantalla muestra arriba.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarEventosEspeciales
 * Parametros: fecha_desde, fecha_hasta (aaaa-mm-dd, obligatorios),
 *             estado, idtienda, tipo, filtro (opcionales)
 */
@WebServlet("/ConsultarEventosEspeciales")
public class ConsultarEventosEspeciales extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ConsultarEventosEspeciales() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        String fechaDesde = request.getParameter("fecha_desde");
        String fechaHasta = request.getParameter("fecha_hasta");
        String estado = request.getParameter("estado");
        String tipo = request.getParameter("tipo");
        String filtro = request.getParameter("filtro");
        String idtiendaStr = request.getParameter("idtienda");

        PrintWriter out = response.getWriter();

        if (fechaDesde == null || fechaDesde.trim().isEmpty() || fechaHasta == null || fechaHasta.trim().isEmpty()) {
            out.write("{\"resultado\":\"ERROR\",\"mensaje\":\"Faltan las fechas del rango (fecha_desde y fecha_hasta).\"}");
            return;
        }

        int idtienda = 0;
        try {
            if (idtiendaStr != null && !idtiendaStr.trim().isEmpty()) {
                idtienda = Integer.parseInt(idtiendaStr.trim());
            }
        } catch (NumberFormatException e) {
            idtienda = 0;
        }

        EventoEspecialCtrl ctrl = new EventoEspecialCtrl();
        out.write(ctrl.listarJson(fechaDesde.trim(), fechaHasta.trim(), estado, idtienda, tipo, filtro));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
