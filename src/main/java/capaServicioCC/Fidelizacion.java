package capaServicioCC;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import com.google.gson.Gson;

import capaControladorCC.FidelizacionCtrl;
import capaModeloCC.Candidato;

/**
 * Servlet implementation class CRUDTienda
 * Servicio que implementa los m�todos para soportar el CRUD de la entidad Tienda.
 */
@WebServlet("/Fidelizacion")
public class Fidelizacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Fidelizacion() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
    	
        int idoperacion = 0;
        String correo = "", nombre = "", apellidos = "", telefono = "", fechaNacimiento = "", politicaDatos = "";
        int idtienda = 0;

        try {
            if ("application/json".equals(request.getContentType())) {
                // Leer JSON
                BufferedReader reader = request.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String requestBody = sb.toString();

                Gson gson = new Gson();
                JSONObject jsonObject = gson.fromJson(requestBody, JSONObject.class);

                idoperacion = jsonObject.get("idoperacion") != null ? Integer.parseInt(jsonObject.get("idoperacion").toString()) : 0;
                correo = jsonObject.get("correo") != null ? jsonObject.get("correo").toString() : "";
                nombre = jsonObject.get("nombre") != null ? jsonObject.get("nombre").toString() : "";
                apellidos = jsonObject.get("apellidos") != null ? jsonObject.get("apellidos").toString() : "";
                telefono = jsonObject.get("telefono") != null ? jsonObject.get("telefono").toString() : "";
                fechaNacimiento = jsonObject.get("fechaNacimiento") != null ? jsonObject.get("fechaNacimiento").toString() : "";
                politicaDatos = jsonObject.get("politicaDatos") != null ? jsonObject.get("politicaDatos").toString() : "";
                idtienda = jsonObject.get("idtienda") != null ? Integer.parseInt(jsonObject.get("idtienda").toString()) : 0;

            } else {
                // Leer parámetros de formulario
                idoperacion = request.getParameter("idoperacion") != null ? Integer.parseInt(request.getParameter("idoperacion")) : 0;
                correo = request.getParameter("correo") != null ? request.getParameter("correo") : "";
                nombre = request.getParameter("nombre") != null ? request.getParameter("nombre") : "";
                apellidos = request.getParameter("apellidos") != null ? request.getParameter("apellidos") : "";
                telefono = request.getParameter("telefono") != null ? request.getParameter("telefono") : "";
                fechaNacimiento = request.getParameter("fechaNacimiento") != null ? request.getParameter("fechaNacimiento") : "";
                politicaDatos = request.getParameter("politicaDatos") != null ? request.getParameter("politicaDatos") : "";
                idtienda = request.getParameter("idtienda") != null ? Integer.parseInt(request.getParameter("idtienda")) : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error procesando la solicitud.");
            return;
        }

        System.out.println("idoperacion recibido: " + idoperacion);

        FidelizacionCtrl fideCtrl = new FidelizacionCtrl();
        String respuesta;

        switch (idoperacion) {
            case 1:
                respuesta = fideCtrl.RegistrarClienteFidelizacionWb(correo, nombre, telefono, fechaNacimiento, apellidos, idtienda, politicaDatos);
                break;
            case 2:
                respuesta = fideCtrl.ConsultarClienteFidelizacionWb(correo);
                break;
            case 3:
                respuesta = fideCtrl.activarClienteFidelizacion(correo);
                break;
            default:
                JSONObject respuestoObject = new JSONObject();
                respuestoObject.put("respuesta", false);
                respuestoObject.put("mensaje", "Parámetros inválidos o vacíos");
                respuesta = respuestoObject.toJSONString();
                break;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(respuesta);
    }


    
	


}
