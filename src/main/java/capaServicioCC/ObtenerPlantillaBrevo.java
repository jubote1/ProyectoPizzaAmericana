package capaServicioCC;



import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import capaControladorCC.SegmentacionClienteCtrl;
import capaDAOCC.SegmentacionClienteDAO;
import capaModeloCC.PlantillaBrevo;

/**
 * Servicio de consulta de Bot y CRM (Kommo): invoca SegmentacionClienteCtrl.obtenerPlantillas.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerPlantillaBrevo   Metodos: GET.
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: categoria, error, idplantilla, nombre.
 * Lee: plantilla_brevo.
 * Sistemas externos: Brevo.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerPlantillaBrevo.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerPlantillaBrevo")
public class ObtenerPlantillaBrevo extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        try {
            SegmentacionClienteCtrl SegCtrl = new SegmentacionClienteCtrl();
            String respuesta = SegCtrl.obtenerPlantillas();

            if (respuesta == null || respuesta.isEmpty()) {
                respuesta = "[]"; // Evita que se devuelva una respuesta vacía
            }

            out.print(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Error al obtener datos\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
