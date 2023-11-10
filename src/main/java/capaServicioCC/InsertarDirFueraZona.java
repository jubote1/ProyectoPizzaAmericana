package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;;

/**
 * Servlet implementation class InsertarEspecialidad
 * Método que se encarga recibir los parámetros para la inserción de una especialidad.
 */
@WebServlet("/InsertarDirFueraZona")
public class InsertarDirFueraZona extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarDirFueraZona() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Servicio que recibe los parámetros para llamar al método insertarEspecialidad de la capa Parametros controlador,
	 * e insertar la especialidad.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		HttpSession sesion = request.getSession();
		String direccion = request.getParameter("direccion");
		String municipio = request.getParameter("municipio");
		String strIdCliente = request.getParameter("idcliente");
		String strLatitud = request.getParameter("latitud");
		String strLongitud = request.getParameter("longitud");
		String telefono= request.getParameter("telefono");
		String nombre = request.getParameter("nombre");
		String apellido = request.getParameter("apellido");
		double latitud = 0;
		double longitud = 0;
		int idCliente = 0;
		try{
			idCliente = Integer.parseInt(strIdCliente);
		}catch(Exception e)
		{
			idCliente = 0;
		}
		try{
			latitud = Double.parseDouble(strLatitud);
		}catch(Exception e)
		{
			latitud = 0;
		}
		try{
			longitud = Double.parseDouble(strLongitud);
		}catch(Exception e)
		{
			longitud = 0;
		}
		ParametrosCtrl Parametros = new ParametrosCtrl();
		String respuesta = Parametros.insertarDirFueraZona(direccion, municipio, idCliente, latitud, longitud, telefono, nombre, apellido);
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
