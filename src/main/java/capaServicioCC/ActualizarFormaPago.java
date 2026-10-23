package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;;

/**
 * Servicio que modifica datos de Sin clasificar (revisar a mano): invoca
 * PedidoCtrl.actualizarFormaPago.
 *
 * Ruta: /ProyectoPizzaAmericana/ActualizarFormaPago   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idformapagonueva (integer), idpedido (integer), valorformapagonueva
 * (number), valortotal (number), virtualorigen (boolean), virtualdestino (boolean).
 * Devuelve: application/json;charset=UTF-8; claves: resultado.
 * Lee: pedido_forma_pago.
 * Escribe: pedido (UPDATE), pedido_forma_pago (DELETE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ActualizarFormaPago.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ActualizarFormaPago")
public class ActualizarFormaPago extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ActualizarFormaPago() {
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
		response.setContentType("application/json;charset=UTF-8");
		try{
			request.setCharacterEncoding("UTF-8");
			int idFormaPagoNueva = Integer.parseInt(request.getParameter("idformapagonueva"));
			int idPedido = Integer.parseInt(request.getParameter("idpedido"));
			double valorFormaPagoNueva = Double.parseDouble(request.getParameter("valorformapagonueva"));
			double valorTotal = Double.parseDouble(request.getParameter("valortotal"));
			boolean virtualOrigen = Boolean.parseBoolean(request.getParameter("virtualorigen"));
			boolean virtualDestino = Boolean.parseBoolean(request.getParameter("virtualdestino"));
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.actualizarFormaPago(idPedido, idFormaPagoNueva, valorFormaPagoNueva, valorTotal, virtualOrigen, virtualDestino);
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
