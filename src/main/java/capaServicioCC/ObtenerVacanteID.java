package capaServicioCC;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import capaControladorCC.VacanteCtrl;
import capaModeloCC.Vacante;


@WebServlet("/ObtenerVacanteID")
public class ObtenerVacanteID extends HttpServlet {

    private VacanteCtrl vacanteCtrl;

    @Override
    public void init() throws ServletException {
        vacanteCtrl = new VacanteCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int idVacante = Integer.parseInt(request.getParameter("idvacante"));
        Vacante contenidos = vacanteCtrl.obtenerVacantePorId(idVacante);
        // Convertir la lista de contenidos a JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(contenidos);

        // Configurar la respuesta
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
