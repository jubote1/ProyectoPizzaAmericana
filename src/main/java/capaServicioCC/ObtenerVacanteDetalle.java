package capaServicioCC;

import capaControladorCC.VacanteCtrl;
import capaModeloCC.Vacante;

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
 * VacanteCtrl.obtenerVacanteDetalle.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerVacanteDetalle   Metodos: GET.
 * Tipo: Consulta.  Acceso: Filtro JWT del web.xml.
 * Recibe (query/form): idvacante (integer).
 * Devuelve: cuerpo vacio.
 * Lee: beneficio, categoria, contrato, horario, lugar, vacante, vacante_beneficio,
 * vacante_horario, vacante_lugar.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerVacanteDetalle.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerVacanteDetalle")
public class ObtenerVacanteDetalle extends HttpServlet {

    private VacanteCtrl vacanteCtrl;

    @Override
    public void init() throws ServletException {
        vacanteCtrl = new VacanteCtrl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int idVacante = Integer.parseInt(request.getParameter("idvacante"));
        Vacante contenidos = vacanteCtrl.obtenerVacanteDetalle(idVacante);
        // Convertir la lista de contenidos a JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(contenidos);

        // Configurar la respuesta
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
