package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import capaModeloCC.ComentarioPqrs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import capaControladorCC.SolicitudPQRSCtrl;
import com.google.gson.reflect.TypeToken;

/**
 * Servlet implementation class InseratarSolicitudPQRS Este servicio se encarga
 * de insertar en el sistema la informaci�n de una solicitud PQRS, este servlet
 * hace las veces de front y se encarga de la invocaci�n a la clase en la capa
 * Controladora.
 */
@WebServlet("/InsertarPqrsWeb")
public class InsertarPqrsWeb extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public InsertarPqrsWeb() {
		super();
	}

	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException {
	    	
	    	
	    	response.setContentType("application/json");
	         response.setCharacterEncoding("UTF-8");
	        request.setCharacterEncoding("UTF-8");
	        // Definir variables
	        String fechasolicitud = "";
	        String tiposolicitud = "";
	        String nombres = "";
	        String apellidos = "";
	        String telefono = "";
	        String direccion = "";
	        String email = "";
	        int idtienda = 0;
	        int idmunicipio = 0;
	        String politicaDatos = "";
	        String comentario = "";

	        try {
	            if ("application/json".equalsIgnoreCase(request.getContentType())) {
	                // Leer JSON desde el cuerpo
	                StringBuilder sb = new StringBuilder();
	                try (BufferedReader reader = request.getReader()) {
	                    String line;
	                    while ((line = reader.readLine()) != null) {
	                        sb.append(line);
	                    }
	                }

	                String jsonBody = sb.toString();
	                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
	                Type listType = new TypeToken<List<ComentarioPqrs>>() {}.getType();
	                JSONObject json = new JSONObject(jsonBody);

	                fechasolicitud = json.optString("fechasolicitud");
	                tiposolicitud = json.optString("tiposolicitud");
	                nombres = json.optString("nombre");
	                apellidos = json.optString("apellido");
	                telefono = json.optString("telefono");
	                direccion = json.optString("direccion");
	                email = json.optString("email");
	                idtienda = json.optInt("idtienda", 0);
	                idmunicipio = json.optInt("idmunicipio", 0);
	                politicaDatos = json.optString("politicaDatos");
	                comentario = json.optString("comentario");

	            } else {
	                // Procesar como formulario normal
	                fechasolicitud = request.getParameter("fechasolicitud");
	                tiposolicitud = request.getParameter("tiposolicitud");
	                nombres = request.getParameter("nombre");
	                apellidos = request.getParameter("apellido");
	                telefono = request.getParameter("telefono");
	                direccion = request.getParameter("direccion");
	                email = request.getParameter("email");
	                idtienda = parseIntSafe(request.getParameter("idtienda"));
	                idmunicipio = parseIntSafe(request.getParameter("idmunicipio"));
	                politicaDatos = request.getParameter("politicaDatos");
	                comentario = request.getParameter("comentario");
	            }

	            // Validación básica
	            if (telefono == null || telefono.trim().isEmpty()) {
	                throw new IllegalArgumentException("El teléfono es obligatorio.");
	            }

	            // Llamar al controlador de negocio
	            SolicitudPQRSCtrl solicitudCtrl = new SolicitudPQRSCtrl();
	            String resultadoJson = solicitudCtrl.insertarPqrsWeb(
	                    fechasolicitud, tiposolicitud, idtienda, nombres, apellidos,
	                    telefono, direccion, idmunicipio, comentario,
	                    politicaDatos, email
	            );

	            // Responder
	            
	            PrintWriter out = response.getWriter();
	            out.write(resultadoJson);

	        } catch (IllegalArgumentException e) {
	            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
	        } catch (Exception e) {
	            e.printStackTrace();
	            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error inesperado del servidor.");
	        }
	    }

	    private int parseIntSafe(String param) {
	        try {
	            return Integer.parseInt(param);
	        } catch (Exception e) {
	            return 0;
	        }
	    }
	}
	

