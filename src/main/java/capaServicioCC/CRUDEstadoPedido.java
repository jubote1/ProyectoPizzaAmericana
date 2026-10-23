package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;

/**
 * Servicio con 5 operaciones que se escogen con el parametro operacion.
 *
 * Ruta: /ProyectoPizzaAmericana/CRUDEstadoPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Mixto (consulta y modifica).  Acceso: Sin validacion propia.
 * Recibe (query/form): idoperacion (string), descripcion (string), idestadopedido (integer).
 * Operaciones (operacion): 1, 2, 3, 4, 5.
 * Devuelve: texto; claves: descripcion, idestadopedido, resultado.
 * Lee: estado_pedido.
 * Escribe: estado_pedido (DELETE/INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CRUDEstadoPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CRUDEstadoPedido")
public class CRUDEstadoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CRUDEstadoPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro principal un idoperación que significa lo siguiente: 1 insertar 2 editar 3 Eliminar  4 Consultar
	 * con base en el idoperación se pedirán los parámetros necesario para cada operación del CRUD en la entidad estado pedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//Operación idoperacion 1 insertar 2 editar 3 Eliminar  4 Consultar
		response.addHeader("Access-Control-Allow-Origin", "*");
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
			respuesta = ParametrosCtrl.insertarEstadoPedido(descripcion);
		}else if (operacion ==2)
		{
			int idestedit= Integer.parseInt(request.getParameter("idestadopedido"));
			String descedit= request.getParameter("descripcion");
			respuesta = ParametrosCtrl.editarEstadoPedido(idestedit, descedit);
		}else if (operacion ==3 )
		{
			int idesteli = Integer.parseInt(request.getParameter("idestadopedido"));
			respuesta = ParametrosCtrl.eliminarEstadoPedido(idesteli);
		}else if (operacion == 4)
		{
			int idestcon = Integer.parseInt(request.getParameter("idestadopedido"));
			respuesta = ParametrosCtrl.retornarEstadoPedido(idestcon);
		}else if(operacion == 5)
		{
			respuesta = ParametrosCtrl.retornarEstadosPedido();
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
