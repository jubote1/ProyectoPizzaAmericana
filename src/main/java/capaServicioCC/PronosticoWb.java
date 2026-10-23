package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import capaControladorCC.PartidoCtrl;
import capaModeloCC.Pronostico;

/**
 * Servicio de consulta de Sin clasificar (revisar a mano): invoca PartidoCtrl.guardarPronostico.
 *
 * Ruta: /ProyectoPizzaAmericana/PronosticoWb   Metodos: POST.
 * Tipo: Consulta.  Acceso: Filtro JWT del web.xml.
 * Recibe: cuerpo de la peticion.
 * Devuelve: cuerpo vacio; claves: message, success.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha PronosticoWb.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/PronosticoWb")
public class PronosticoWb extends HttpServlet {

    private PartidoCtrl partidoCtrl;

    @Override
    public void init() throws ServletException {
    	partidoCtrl = new PartidoCtrl();
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        try {

            StringBuilder jsonBuilder =
                    new StringBuilder();

            String line;

            try (BufferedReader reader =
                    request.getReader()) {

                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            Gson gson = new Gson();

            Pronostico pronostico =
                    gson.fromJson(
                            jsonBuilder.toString(),
                            Pronostico.class
                    );

            String respuesta =
            		partidoCtrl.guardarPronostico(
                            pronostico
                    );

            response.setContentType("application/json");
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