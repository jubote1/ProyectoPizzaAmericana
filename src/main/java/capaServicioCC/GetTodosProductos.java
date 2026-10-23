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
 * Devuelve todos los productos habilitados de la empresa, sin considerar la tienda.
 *
 * Ruta: /ProyectoPizzaAmericana/GetTodosProductos   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: descripcion, idproducto, idreceta, idtipo_liquido, impuesto,
 * incluye_liquido, manejacantidad, nombre, preciogeneral, preciopuntos, productoasociaadicion,
 * tipo.
 * Lee: producto.
 *
 * Trampas y riesgos conocidos:
 * - Las claves mezclan estilos (incluye_liquido con guion bajo, productoasociaadicion sin el).
 * - La pantalla lo usa solo cuando no hay tienda escogida; con tienda debe usar
 *   GetProductosTienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetTodosProductos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetTodosProductos")
public class GetTodosProductos extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetTodosProductos() {
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
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.obtenerTodosProductos();
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
