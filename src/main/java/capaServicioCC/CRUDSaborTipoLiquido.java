package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.ParametrosCtrl;

/**
 * Servlet implementation class CRUDSaborTipoLiquido
 * Servicio que se encarga de implementar el CRUD para la entidad Sabor Tipo Liquido.
 */
@WebServlet("/CRUDSaborTipoLiquido")
public class CRUDSaborTipoLiquido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDSaborTipoLiquido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Se recibe como parámetro principal el idoperación, con base en los siguientes valores 1 insertar 2 editar 3 Eliminar  4 Consultar
	 * se toman el resto de parámetros y se invoca el método correspondiente en la capa Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
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
			String descripcion = request.getParameter("descripcion");
			int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
			respuesta = ParametrosCtrl.insertarSaborLiquido(descripcion, idtipoliquido);
		}else if (operacion ==2)
		{
			int idsaborxtipoliquido= Integer.parseInt(request.getParameter("idsabortipoliquido"));
			String descripcion = request.getParameter("descripcion");
			int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
			respuesta = ParametrosCtrl.editarSaborLiquido(idsaborxtipoliquido, descripcion, idtipoliquido);
		}else if (operacion ==3 )
		{
			int idsaborxtipoliquido = Integer.parseInt(request.getParameter("idsabortipoliquido"));
			respuesta = ParametrosCtrl.eliminarSaborLiquido(idsaborxtipoliquido);
		}else if (operacion == 4)
		{
			int idsaborcon = Integer.parseInt(request.getParameter("idsabortipoliquido"));
			respuesta = ParametrosCtrl.retornarSaborLiquido(idsaborcon);
		}else if(operacion == 5)
		{
			respuesta = ParametrosCtrl.retornarSaborLiquidos();
		}else if(operacion == 6)
		{
			respuesta = ParametrosCtrl.retornarSaborLiquidosGrid();
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
