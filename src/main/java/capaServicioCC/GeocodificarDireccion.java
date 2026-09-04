package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import capaControladorCC.UbicacionCtrl;
import capaModeloCC.Ubicacion;

@WebServlet("/GeocodificarDireccion")
public class GeocodificarDireccion extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String direccion = request.getParameter("direccion");
        String municipio = request.getParameter("municipio");
        String barrio = request.getParameter("barrio");

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Ubicacion ubicacion = new UbicacionCtrl().ubicarDireccionEnTiendaBatch(
                    direccion == null ? "" : direccion,
                    municipio == null ? "" : municipio,
                    barrio == null ? "" : barrio
            );
            out.print(String.format(
                    "{\"latitud\":%s,\"longitud\":%s}",
                    ubicacion.getLatitud(), ubicacion.getLongitud()
            ));
        } catch (Exception e) {
            out.print("{\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}");
        }
        out.flush();
    }
}