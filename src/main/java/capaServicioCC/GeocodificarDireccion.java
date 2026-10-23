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

/**
 * Solo geocodifica: devuelve latitud, longitud y la direccion normalizada, sin decir si hay
 * cobertura.
 *
 * Ruta: /ProyectoPizzaAmericana/GeocodificarDireccion   Metodos: GET.
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): direccion (string), municipio (string), barrio (string).
 * Devuelve: application/json; charset=UTF-8; claves: error.
 * Lee: parametros.
 * Sistemas externos: ArcGIS, Google Maps.
 *
 * Trampas y riesgos conocidos:
 * - Los errores salen con HTTP 200: hay que mirar la clave error.
 * - Un 0,0 significa "no encontrado", no es una coordenada valida.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GeocodificarDireccion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
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

    	if (direccion == null || direccion.trim().isEmpty()) {
    	    out.print("{\"error\":\"La dirección es obligatoria\"}");
    	    out.flush();
    	    return;
    	}

    	try {
    	    Ubicacion ubicacion = new UbicacionCtrl().ubicarDireccionEnTiendaBatch(
    	            direccion,
    	            municipio == null ? "" : municipio,
    	            barrio == null ? "" : barrio
    	    );

    	    String dirSalida = ubicacion.getDireccion();
    	    if (dirSalida == null || dirSalida.trim().isEmpty()) {
    	        dirSalida = direccion.trim();
    	    }

    	    com.google.gson.JsonObject jsonResp = new com.google.gson.JsonObject();
    	    jsonResp.addProperty("latitud", ubicacion.getLatitud());
    	    jsonResp.addProperty("longitud", ubicacion.getLongitud());
    	    jsonResp.addProperty("direccion", dirSalida);

    	    out.print(jsonResp.toString());

    	} catch (Exception e) {
    	    out.print("{\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}");
    	}

    	out.flush();
    }
}