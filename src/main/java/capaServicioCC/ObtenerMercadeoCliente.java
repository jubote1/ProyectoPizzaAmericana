package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;;

/**
 * Resume el historial comercial de un telefono: ultimo pedido, cuantos pedidos en 30, 90 y 180
 * dias, PQRS radicadas y sus especialidades preferidas.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerMercadeoCliente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): telefono (string).
 * Devuelve: application/json; claves: cantidadpedidos180, cantidadpedidos30, cantidadpedidos90,
 * cantidadpqrs, especialidades, fechaultimopedido.
 * Lee: cliente, detalle_pedido, especialidad, pedido, solicitudpqrs.
 *
 * Trampas y riesgos conocidos:
 * - fechaultimopedido sale vacia si nunca ha pedido.
 * - Cuatro consultas mas una de PQRS por llamada, todas con SQL concatenado.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerMercadeoCliente.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerMercadeoCliente")
public class ObtenerMercadeoCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerMercadeoCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro del tel�fono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el tel�fono indicado.
	 * Lo anterior invocando el m�todo obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			String tel = request.getParameter("telefono");
			System.out.println(tel);
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			String respuesta = ClienCtrl.obtenerMercadeoCliente(tel);
			PrintWriter out = response.getWriter();
			out.write(respuesta);
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
