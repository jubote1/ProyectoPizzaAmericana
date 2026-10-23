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
 * Da los avisos de un cliente por su idcliente: PQRS que ha radicado y su ultimo pedido.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerNotificacionesCliente   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idcliente (integer).
 * Devuelve: application/json; claves: formapago, idpedido, pqrs, tiempopedido, ultimopedido.
 * Lee: forma_pago, pedido, pedido_forma_pago, solicitudpqrs.
 *
 * Trampas y riesgos conocidos:
 * - El texto pqrs es una frase armada en el servidor ("Ha radicado PQRS ,101,205"), no una
 *   lista.
 * - idcliente no numerico: respuesta vacia.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerNotificacionesCliente.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerNotificacionesCliente")
public class ObtenerNotificacionesCliente extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerNotificacionesCliente() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro del teléfono el cual es manejado como un String, con base en esto se retorna
	 * en formato JSON todos los registros que tiene asociado el cliente en la tabla de clientes con el teléfono indicado.
	 * Lo anterior invocando el método obtenerCliente(tel) de la capa controlador cliente.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		try{
			int idCliente = Integer.parseInt(request.getParameter("idcliente"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			ClienteCtrl ClienCtrl = new ClienteCtrl();
			//En este punto realizaremos el llamado al método para retornar la información
			String respuesta = ClienCtrl.obtenerNotificacionesCliente(idCliente);
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
