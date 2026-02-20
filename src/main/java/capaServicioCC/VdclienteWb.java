package capaServicioCC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import capaControladorCC.CandidatoCtrl;
import capaControladorCC.EncuestaCtrl;
import capaControladorCC.PedidoCtrl;
import capaControladorCC.VacanteCtrl;
import capaModeloCC.Candidato;
import capaModeloCC.EncuestaServicio;
import capaModeloCC.ExperienciaCandidato;
import capaModeloCC.FormacionCandidato;

import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;

@WebServlet("/VdclienteWb")
public class VdclienteWb extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private EncuestaCtrl encuestaCtrl;
    private static final Logger logger = Logger.getLogger(VdclienteWb.class.getName());


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            PedidoCtrl pedCtrl = new PedidoCtrl();
        	HttpSession sesion = request.getSession();
			int idPedido = Integer.parseInt(request.getParameter("idpedido"));
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
            String respuesta = pedCtrl.validarPedidoEncuesta(idPedido, idTienda);
            System.out.println("llamado con " + idPedido + " idtienda " + idTienda);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(respuesta);
            }

        } catch (Exception e) {
            logger.severe("❌ Error procesando la solicitud: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno del servidor.\"}");
        }
    }
}


    
