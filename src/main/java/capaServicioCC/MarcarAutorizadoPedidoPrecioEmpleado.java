package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ClienteCtrl;
import capaControladorCC.PedidoCtrl;
import capaModeloCC.PedidoPrecioEmpleado;
import capaModeloCC.Usuario;

/**
 * Servicio que modifica datos de Tiendas y operacion: invoca
 * PedidoCtrl.marcarAutorizadoPedidoPrecioEmpleado.
 *
 * Ruta: /ProyectoPizzaAmericana/MarcarAutorizadoPedidoPrecioEmpleado   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): codigo (string).
 * Devuelve: texto.
 * Escribe: pedido_precio_empleado (UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha MarcarAutorizadoPedidoPrecioEmpleado.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/MarcarAutorizadoPedidoPrecioEmpleado")
public class MarcarAutorizadoPedidoPrecioEmpleado extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MarcarAutorizadoPedidoPrecioEmpleado() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro la información más básica de un pedido como lo son, tinda, idcliente, 
	 * fechapedido, con estos tres hace una inserción del encabezado pedido y retorna en formato JSON el idpedido que se
	 * manejará para el pedido. Esta labor se realiza invocando el método InsertarEncabezadoPedido de la capa Pedido
	 * Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		String codigo = request.getParameter("codigo");		
		PedidoCtrl pedCtrl = new PedidoCtrl();
		pedCtrl.marcarAutorizadoPedidoPrecioEmpleado(codigo);
        System.out.println("");
        PrintWriter out = response.getWriter();
		out.write("");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
