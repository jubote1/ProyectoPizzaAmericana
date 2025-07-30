package capaServicioCC;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import capaControladorCC.SolicitudPQRSCtrl;
import capaControladorCC.VacanteCtrl;
import capaModeloCC.Vacante;



@WebServlet("/PendientePqrs")
public class PendientePqrs extends HttpServlet {

    private SolicitudPQRSCtrl solicitudCtrl;

    @Override
    public void init() throws ServletException {
    	solicitudCtrl = new SolicitudPQRSCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
        	response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
            String fecha = request.getParameter("fecha");
            String respuesta = solicitudCtrl.obtenerPendientesPqrs(fecha);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(respuesta);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Ocurrió un error al obtener las solicitudes.\"}");
        }
    }

}
