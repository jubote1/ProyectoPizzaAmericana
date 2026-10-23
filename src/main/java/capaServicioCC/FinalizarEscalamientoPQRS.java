package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EscalamientoPQRS;;

/**
 * Servicio que modifica datos de PQRS y escalamiento: invoca
 * SolicitudPQRSCtrl.finalizarEscalamientoPQRS.
 *
 * Ruta: /ProyectoPizzaAmericana/FinalizarEscalamientoPQRS   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idescalamiento (integer).
 * Devuelve: texto; claves: respuesta.
 * Escribe: escalamiento_pqrs (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha FinalizarEscalamientoPQRS.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/FinalizarEscalamientoPQRS")
public class FinalizarEscalamientoPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FinalizarEscalamientoPQRS() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los par�metros para llamar al m�todo insertarEspecialidad de la capa Parametros controlador,
	 * e insertar la especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		JSONObject resultadoJSON = new JSONObject();
		int idEscalamiento = 0;
		try {
			idEscalamiento = Integer.parseInt(request.getParameter("idescalamiento"));
		}catch(Exception e)
		{
			idEscalamiento = 0;
		}
		SolicitudPQRSCtrl solicitudPQRSCtrl = new SolicitudPQRSCtrl();
		String respuesta = solicitudPQRSCtrl.finalizarEscalamientoPQRS(idEscalamiento);
        PrintWriter out = response.getWriter();
		out.write(respuesta);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
