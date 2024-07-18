package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;
/**
 * Servlet implementation class CRUDEspecialidad
 * Servicio que encarga de implementar los servicios CRUD para la entidad Especialidad.
 */
@WebServlet("/CRUDEspecialidad")
public class CRUDEspecialidad extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDEspecialidad() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe un idoperación que puede ser 1 insertar 2 editar 3 Eliminar  4 Consultar
	 * dependiendo el valor de idoperacion, se recibirán los diferentes parámetros de la entidad especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");	
		Logger logger = Logger.getLogger("log_file");
			HttpSession sesion = request.getSession();
			response.addHeader("Access-Control-Allow-Origin", "*");
			String idoperacion = request.getParameter("idoperacion");
			ParametrosCtrl ParametrosCtrl = new ParametrosCtrl();
			int operacion;
			String respuesta="";
			try
			{
				operacion = Integer.parseInt(idoperacion);
			}catch(Exception e){
				operacion = 0;
			}
			if (operacion ==1)
			{
				String nombre = request.getParameter("nombre");
				String abreviatura = request.getParameter("abreviatura");
				logger.info("insertar especialidad con parametros nombre "+ nombre + " abreviatura " + abreviatura);
				respuesta = ParametrosCtrl.insertarEspecialidad(nombre, abreviatura);
			}else if (operacion ==2)
			{
				int idespedit= Integer.parseInt(request.getParameter("idespecialidad"));
				String nombedit= request.getParameter("nombre");
				String abreedit = request.getParameter("abreviatura");
				logger.info("editar especialiad con parámetros idespecialidad  " + idespedit + " nombre " + nombedit  + " abreviatura " + abreedit);
				respuesta = ParametrosCtrl.editarEspecialidad(idespedit, nombedit, abreedit);
			}else if (operacion ==3 )
			{
				int idespeli = Integer.parseInt(request.getParameter("idespecialidad"));
				respuesta = ParametrosCtrl.eliminarEspecialidad(idespeli);
			}else if (operacion == 4)
			{
				int idespcon = Integer.parseInt(request.getParameter("idespecialidad"));
				respuesta = ParametrosCtrl.retornarEspecialidad(idespcon);
			}
			System.out.println(respuesta);
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
