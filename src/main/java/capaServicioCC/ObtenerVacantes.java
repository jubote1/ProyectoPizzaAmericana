package capaServicioCC;


import capaModeloCC.Vacante;
import capaControladorCC.VacanteCtrl;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * VacanteCtrl.obtenerVacantesActivas.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerVacantes   Metodos: GET.
 * Tipo: Consulta.  Acceso: Filtro JWT del web.xml.
 * Devuelve: cuerpo vacio.
 * Lee: categoria, lugar, vacante, vacante_lugar.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerVacantes.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerVacantes")
public class ObtenerVacantes extends HttpServlet {

    private VacanteCtrl vacanteCtrl;

    @Override
    public void init() throws ServletException {
        vacanteCtrl = new VacanteCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Vacante> vacantes = vacanteCtrl.obtenerVacantesActivas();

        // Convertir la lista de vacantes a JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(vacantes);

        // Configurar la respuesta
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
