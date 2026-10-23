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

/**
 * Servicio que modifica datos de Direccion y cobertura (administracion): invoca
 * PedidoCtrl.insertarTmpPedidoPoligono.
 *
 * Ruta: /ProyectoPizzaAmericana/InsertarTmpPedidoPoligono   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedido (integer), idcliente (integer).
 * Devuelve: application/json; claves: resultado.
 * Escribe: tmp_pedidos_poligono (INSERT).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha InsertarTmpPedidoPoligono.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/InsertarTmpPedidoPoligono")
public class InsertarTmpPedidoPoligono extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public InsertarTmpPedidoPoligono() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros se encarga de retornar en formato JSON todos los productos parametrizados en el
	 * sistema invocando al método obtenerTodosProductos de la capa Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			int idPedido;
			int idCliente;
			try
			{
				idPedido= Integer.parseInt(request.getParameter("idpedido"));
			}catch(Exception e)
			{
				System.out.println(e.toString());
				idPedido = 0;
			}
			try
			{
				idCliente = Integer.parseInt(request.getParameter("idcliente"));
			}catch(Exception e)
			{
				System.out.println(e.toString());
				idCliente = 0;
			}
			PedidoCtrl pedCtrl = new PedidoCtrl();
			String respuesta = pedCtrl.insertarTmpPedidoPoligono(idPedido, idCliente);
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
