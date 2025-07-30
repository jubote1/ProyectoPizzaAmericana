package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.SolicitudPQRSCtrl;
import capaModeloCC.EscalamientoPQRS;;

/**
 * Servlet implementation class InsertarEspecialidad
 * M�todo que se encarga recibir los par�metros para la inserci�n de una especialidad.
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
		HttpSession sesion = request.getSession();
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
