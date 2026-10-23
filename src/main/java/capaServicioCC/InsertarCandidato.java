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
import javax.servlet.http.Part;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import capaControladorCC.CandidatoCtrl;
import capaControladorCC.VacanteCtrl;
import capaModeloCC.Candidato;
import capaModeloCC.ExperienciaCandidato;
import capaModeloCC.FormacionCandidato;

import java.util.List;
import java.util.ArrayList;

/**
 * Servicio que modifica datos de Campanas, publicidad y mercadeo: invoca
 * CandidatoCtrl.insertarCandidato.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarCandidato   Metodos: POST.
 * Tipo: Modifica datos.  Acceso: Filtro JWT del web.xml.
 * Recibe: cuerpo de la peticion.
 * Devuelve: application/json; claves: mensaje, respuesta.
 * Escribe: candidato (INSERT), experiencia_candidato (INSERT), formacion_candidato (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarCandidato.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarCandidato")
    public class InsertarCandidato extends HttpServlet {
        private CandidatoCtrl candidatoCtrl;
        private static final long serialVersionUID = 1L;
        
        @Override
        public void init() throws ServletException {
        	candidatoCtrl = new CandidatoCtrl();
        }

        protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        	   // Leer el cuerpo de la solicitud como JSON
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }
            String json = jsonBuilder.toString();

            // Convertir el JSON en objeto Candidato
            Gson gson = new Gson();
            Candidato candidato = gson.fromJson(json, Candidato.class);
            
            // Imprimir en consola para depuración
            String candidatoJson = gson.toJson(candidato);
            System.out.println("Candidato: " + candidatoJson);
            
            // Insertar el candidato en la base de datos
            String respuesta = candidatoCtrl.insertarCandidato(candidato);
            
            // Devolver la respuesta
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(respuesta);
        }
    }

    
