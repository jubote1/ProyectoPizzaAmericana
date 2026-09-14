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