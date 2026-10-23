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
 * Devuelve la forma de pago con la que quedo un pedido.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerFormaPagoPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: texto; claves: descuento, idformapago, nombre, valorformapago, valortotal, virtual.
 * Lee: forma_pago, pedido, pedido_forma_pago.
 *
 * Trampas y riesgos conocidos:
 * - valortotal aqui es el valor que mando quien finalizo el pedido, no necesariamente la suma de
 *   los renglones.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerFormaPagoPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerFormaPagoPedido")
public class ObtenerFormaPagoPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerFormaPagoPedido() {
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
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.obtenerFormaPagoPedido(idpedido);
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
