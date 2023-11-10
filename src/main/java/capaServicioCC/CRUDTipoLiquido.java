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
 * Servlet implementation class CRUDTipoLiquido
 * Servicio implementado para soportar las operaciones CRUD de la entidad tipo liquido.
 */
@WebServlet("/CRUDTipoLiquido")
public class CRUDTipoLiquido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDTipoLiquido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este método se encarga de solicitar principalmente el idoperacion de acuerdo a lo siguiente:
	 * 1 insertar 2 editar 3 Eliminar  4 Consultar, con base en lo anterior se solicitarán los parametros y 
	 * posteriormente se invocará el método correspondiente en la capa controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		HttpSession sesion = request.getSession();
		response.addHeader("Access-Control-Allow-Origin", "*");
		String idoperacion = request.getParameter("idoperacion");
		ParametrosCtrl ParametrosCtrl = new ParametrosCtrl();
		int operacion;
		String respuesta="";
		System.out.println("operacion " + idoperacion) ;
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			String nombre = request.getParameter("nombre");
			String capacidad = request.getParameter("capacidad");
			respuesta = ParametrosCtrl.insertarTipoLiquido(nombre, capacidad);
		}else if (operacion ==2)
		{
			int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
			String nombre = request.getParameter("nombre");
			String capacidad = request.getParameter("capacidad");
			respuesta = ParametrosCtrl.editarTipoLiquido(idtipoliquido, nombre, capacidad);
		}else if (operacion ==3 )
		{
			int idtipoliquido = Integer.parseInt(request.getParameter("idtipoliquido"));
			respuesta = ParametrosCtrl.eliminarTipodLiquido(idtipoliquido);
		}else if (operacion == 4)
		{
			int idtipcon = Integer.parseInt(request.getParameter("idtipoliquido"));
			respuesta = ParametrosCtrl.retornarTipoLiquido(idtipcon);
		}else if(operacion == 5)
		{
			respuesta = ParametrosCtrl.retornarTiposLiquidos();
		}else if(operacion == 6)
		{
			respuesta = ParametrosCtrl.retornarTiposLiquidos();
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
