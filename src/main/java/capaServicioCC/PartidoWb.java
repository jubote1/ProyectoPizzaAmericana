package capaServicioCC;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PartidoCtrl;

@WebServlet("/PartidoWb")
public class PartidoWb extends HttpServlet {

    private PartidoCtrl partidoCtrl;

    @Override
    public void init() throws ServletException {
        partidoCtrl = new PartidoCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        try {

            String respuesta =
                    partidoCtrl.obtenerPartidoPublicado();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(respuesta);

        } catch (Exception e) {

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().write(
                    "{\"success\":false}"
            );
        }
    }
}