package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.TiendaCtrl;

/**
 * Calcula, en el servidor, el descuento que le corresponde a un pedido segun los descuentos
 * generales diarios del canal contact center.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerDescuentoDiarioPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: texto; claves: descuento, motivodescuento.
 * Lee: adicion_detalle_pedido, descuentos_generales, detalle_pedido.
 *
 * Trampas y riesgos conocidos:
 * - Es uno de los pocos calculos de dinero que si viven en el servidor. El bot lo usa con el
 *   mismo motivo.
 * - La pantalla no deja aplicarlo si el pedido ya tiene ofertas asignadas.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerDescuentoDiarioPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerDescuentoDiarioPedido")
public class ObtenerDescuentoDiarioPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerDescuentoDiarioPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros y se encarga de retornar en formato JSON todas las formas de pago
	 * parametrizadas en el sistema, invocando al método obtenerFormaPagoPedido de la capa Parametros controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idPedido = Integer.parseInt(request.getParameter("idpedido"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.obtenerDescuentoDiarioPedido(idPedido);
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
