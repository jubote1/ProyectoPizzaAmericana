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
 * Suma el valor de todos los renglones de un pedido.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerTotalPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: texto; claves: valortotal.
 * Lee: detalle_pedido.
 *
 * Trampas y riesgos conocidos:
 * - Es la suma de lo que se guardo; si un precio fue manipulado, el total tambien.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerTotalPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerTotalPedido")
public class ObtenerTotalPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerTotalPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el idpedido correspondiente y se encarga de retornar el total del pedido,
	 * sumando los detalles de pedidos asociados al pedido, esto invocando al método obtenerTotalPedido de la capa
	 * Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idpedido = Integer.parseInt(request.getParameter("idpedido"));
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.obtenerTotalPedido(idpedido);
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
