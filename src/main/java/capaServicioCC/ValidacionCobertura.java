package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.CoberturaRequest;

@WebServlet("/ValidacionCobertura")
public class ValidacionCobertura extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ValidacionCobertura() {
        super();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        configurarRespuesta(response);

        try {
        	String data = leerBody(request);

        	if (data == null || data.trim().isEmpty()) {
        	    responderError(response, 400, "Debe enviar la direccion.");
        	    return;
        	}

        	Gson gson = new Gson();
        	CoberturaRequest coberturaRequest = gson.fromJson(data, CoberturaRequest.class);

        	if (coberturaRequest == null || coberturaRequest.getDireccion().isBlank()) {
        	    responderError(response, 400, "La direccion es obligatoria.");
        	    return;
        	}

        	PedidoCtrl pedidoCtrl = new PedidoCtrl();

        	String respuesta = pedidoCtrl.validarCobertura(coberturaRequest);
            response.setStatus(HttpServletResponse.SC_OK);

            PrintWriter out = response.getWriter();
            out.write(respuesta);
            out.flush();

        } catch (com.google.gson.JsonSyntaxException e) {
            responderError(response, 400, "El JSON enviado no es valido.");
        } catch (Exception e) {
            System.out.println("Error en ValidacionCobertura: " + e.getMessage());
            responderError(response, 500, "Error interno al validar cobertura.");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        configurarRespuesta(response);
        responderError(response, 405, "Este servicio debe consumirse por POST.");
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        configurarRespuesta(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void configurarRespuesta(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private String leerBody(HttpServletRequest request) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(request.getInputStream(), "UTF-8")
        );

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }


    private void responderError(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);

        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("resultado", mensaje);

        PrintWriter out = response.getWriter();
        out.write(error.toString());
        out.flush();
    }
}