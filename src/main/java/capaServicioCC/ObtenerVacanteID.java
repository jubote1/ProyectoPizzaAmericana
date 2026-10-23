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


/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca VacanteCtrl.obtenerVacantePorId.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerVacanteID   Metodos: GET.
 * Tipo: Consulta.  Acceso: Filtro JWT del web.xml.
 * Recibe (query/form): idvacante (integer).
 * Devuelve: cuerpo vacio; claves: error.
 * Lee: categoria, lugar, vacante, vacante_lugar.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerVacanteID.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerVacanteID")
public class ObtenerVacanteID extends HttpServlet {

    private VacanteCtrl vacanteCtrl;

    @Override
    public void init() throws ServletException {
        vacanteCtrl = new VacanteCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            int idVacante = Integer.parseInt(request.getParameter("idvacante"));
            Vacante contenidos = vacanteCtrl.obtenerVacantePorId(idVacante);
            // Convertir la lista de contenidos a JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(contenidos);

            // Configurar la respuesta
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Ocurrió un error al obtener los vacantes.\"}");
        }
    }
    
    

}
