package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.CampanaNovenasCtrl;

/**
 * Servicio para registrar notas de contacto / bitácora de llamadas en la Campaña de Novenas 25 Años.
 * 
 * Ruta: /ProyectoPizzaAmericana/GuardarBitacoraNovenas
 * Parámetros POST: idlead, idcliente, usuario_asesor, estado_contacto, fecha_tentativa, pizzas_estimadas, observacion
 */
@WebServlet("/GuardarBitacoraNovenas")
public class GuardarBitacoraNovenas extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public GuardarBitacoraNovenas() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");

        String idleadStr = request.getParameter("idlead");
        String idclienteStr = request.getParameter("idcliente");
        String usuarioAsesor = request.getParameter("usuario_asesor");
        String estadoContacto = request.getParameter("estado_contacto");
        String fechaTentativa = request.getParameter("fecha_tentativa");
        String pizzasEstimadasStr = request.getParameter("pizzas_estimadas");
        String observacion = request.getParameter("observacion");

        CampanaNovenasCtrl ctrl = new CampanaNovenasCtrl();
        String jsonRespuesta;

        try {
            int idlead = Integer.parseInt(idleadStr.trim());
            int idcliente = Integer.parseInt(idclienteStr.trim());
            int pizzasEstimadas = 0;
            if (pizzasEstimadasStr != null && !pizzasEstimadasStr.trim().isEmpty()) {
                pizzasEstimadas = Integer.parseInt(pizzasEstimadasStr.trim());
            }

            if (usuarioAsesor == null || usuarioAsesor.trim().isEmpty()) {
                usuarioAsesor = "asesor_cc";
            }
            if (estadoContacto == null || estadoContacto.trim().isEmpty()) {
                estadoContacto = "LLAMADO";
            }
            if (observacion == null || observacion.trim().isEmpty()) {
                observacion = "Contacto realizado.";
            }

            jsonRespuesta = ctrl.guardarBitacoraJson(idlead, idcliente, usuarioAsesor.trim(), estadoContacto.trim(), fechaTentativa, pizzasEstimadas, observacion.trim());
        } catch (Exception e) {
            jsonRespuesta = "{\"resultado\":\"ERROR\",\"mensaje\":\"Parámetros inválidos: " + e.getMessage() + "\"}";
        }

        PrintWriter out = response.getWriter();
        out.write(jsonRespuesta);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
