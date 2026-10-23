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
 * Servicio que modifica datos de Pedidos: invoca PedidoCtrl.cancelarPedido.
 *
 * Ruta: /ProyectoPizzaAmericana/CancelarPedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer).
 * Devuelve: application/json;charset=UTF-8; claves: resultado.
 * Escribe: pedido (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha CancelarPedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/CancelarPedido")
public class CancelarPedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CancelarPedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * M�todo GET del Servicio que recibe como par�metroel idpedido del sistema Contact Center y el numPedidoPixel el cual es el 
	 * n�mero de pedido Pixel que retorno la inserci�n, se invoca en al capa controlador el m�todo actualizarEstadoNumeroPedidoPixel.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				response.setContentType("application/json;charset=UTF-8");
				request.setCharacterEncoding("UTF-8");
				int idpedido = Integer.parseInt(request.getParameter("idpedido"));
				PedidoCtrl PedidoCtrl = new PedidoCtrl();
		        String respuesta = PedidoCtrl.cancelarPedido(idpedido);
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
