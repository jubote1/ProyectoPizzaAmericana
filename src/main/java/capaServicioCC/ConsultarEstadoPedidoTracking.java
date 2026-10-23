package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;

/**
 * A pesar de su nombre, NO consulta: avanza el estado de entrega de un pedido en la tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/ConsultarEstadoPedidoTracking   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer).
 * Devuelve: texto; claves: resultado, tipo_error.
 * Lee: tienda.
 *
 * Trampas y riesgos conocidos:
 * - GRAVE: una llamada de lectura que cambia el estado de un pedido de la tienda. Un agente que
 *   lo use para "ver el estado" lo estaria marcando como entregado.
 * - Ninguna pagina ni sistema del repositorio lo llama: se usa desde fuera (posiblemente una
 *   pagina de seguimiento para el cliente). No retirar sin confirmar.
 * - Sin autenticacion: cualquiera puede avanzar el estado de un pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ConsultarEstadoPedidoTracking.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ConsultarEstadoPedidoTracking")
public class ConsultarEstadoPedidoTracking extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConsultarEstadoPedidoTracking() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocará el método de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.avanzarEstadoPedidoDomicilioLog(idPedidoTienda, idTienda, "", "");
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
