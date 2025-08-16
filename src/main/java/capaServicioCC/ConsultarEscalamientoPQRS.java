package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EscalamientoPQRS;;

/**
 * Servlet implementation class InsertarEspecialidad
 * M�todo que se encarga recibir los par�metros para la inserci�n de una especialidad.
 */
@WebServlet("/ConsultarEscalamientoPQRS")
public class ConsultarEscalamientoPQRS extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ConsultarEscalamientoPQRS() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        SolicitudPQRSCtrl solicitudPQRSCtrl = new SolicitudPQRSCtrl();
        String respuestaJSON = "[]"; // Por defecto, JSON vacío

        try {
            String idsParam = request.getParameter("idsolicitudes"); // varios IDs
            String idParam = request.getParameter("idsolicitudpqrs"); // un solo ID

            if (idsParam != null && !idsParam.trim().isEmpty()) {
                // IDs separados por coma → varios registros
                String[] idsArray = idsParam.split(",");
                int[] ids = new int[idsArray.length];
                for (int i = 0; i < idsArray.length; i++) {
                    ids[i] = Integer.parseInt(idsArray[i].trim());
                }
                // Llamamos al método nuevo que recibe int[]
                respuestaJSON = solicitudPQRSCtrl.consultarEscalamientoPQRS(ids);
            } else if (idParam != null && !idParam.trim().isEmpty()) {
                // Solo un ID → llamamos al método original
                int id = Integer.parseInt(idParam.trim());
                respuestaJSON = solicitudPQRSCtrl.consultarEscalamientoPQRS(id);
            }

        } catch (Exception e) {
            e.printStackTrace();
            respuestaJSON = "[]";
        }

        out.write(respuestaJSON);
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
