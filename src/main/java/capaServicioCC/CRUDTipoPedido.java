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
@WebServlet("/CRUDTipoPedido")
public class CRUDTipoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDTipoPedido() {
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
		try
		{
			operacion = Integer.parseInt(idoperacion);
		}catch(Exception e){
			operacion = 0;
		}
		if (operacion ==1)
		{
			String nombre = request.getParameter("nombre");
			String tipoPago = request.getParameter("tipopago");
			//respuesta = ParametrosCtrl.insertarTipoLiquido(nombre, capacidad);
		}else if (operacion ==2)
		{
			int idtipoPedido = Integer.parseInt(request.getParameter("idtipopedido"));
			String nombre = request.getParameter("nombre");
			String tipoPago = request.getParameter("tipopago");
			//respuesta = ParametrosCtrl.editarTipoLiquido(idtipoliquido, nombre, capacidad);
		}else if (operacion ==3 )
		{
			int idtipoPedido = Integer.parseInt(request.getParameter("idtipopedido"));
			//respuesta = ParametrosCtrl.eliminarTipodLiquido(idtipoliquido);
		}else if (operacion == 4)
		{
			
		}else if(operacion == 5)
		{
			respuesta = ParametrosCtrl.obtenerTiposPedido();
		}else if(operacion == 6)
		{
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
