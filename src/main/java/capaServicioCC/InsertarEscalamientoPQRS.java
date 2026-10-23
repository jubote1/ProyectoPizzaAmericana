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
 * SolicitudPQRSCtrl.insertarEscalamientoPQRS.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarEscalamientoPQRS   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): arearesponsable (string), idsolicitudpqrs (integer).
 * Devuelve: texto; claves: mail.smtp.host, mail.smtp.ssl.protocols, resultado.
 * Lee: area_escalamiento_pqrs, parametros.
 * Escribe: escalamiento_pqrs (INSERT).
 * Sistemas externos: Correo SMTP, Gmail, Wompi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarEscalamientoPQRS.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarEscalamientoPQRS")
public class InsertarEscalamientoPQRS extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarEscalamientoPQRS() {
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
		String areaResponsable = request.getParameter("arearesponsable");
		JSONObject resultadoJSON = new JSONObject();
		int idSolicitudPQRS = 0;
		try {
			idSolicitudPQRS = Integer.parseInt(request.getParameter("idsolicitudpqrs"));
		}catch(Exception e)
		{
			idSolicitudPQRS = 0;
		}
		if(idSolicitudPQRS != 0)
		{
			SolicitudPQRSCtrl solicitudPQRSCtrl = new SolicitudPQRSCtrl();
			EscalamientoPQRS escalamiento = new EscalamientoPQRS(idSolicitudPQRS,areaResponsable);
			solicitudPQRSCtrl.insertarEscalamientoPQRS(escalamiento);
			resultadoJSON.put("resultado", "OK");
		}else
		{
			resultadoJSON.put("resultado", "NOK");
		}
        PrintWriter out = response.getWriter();
		out.write(resultadoJSON.toJSONString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
