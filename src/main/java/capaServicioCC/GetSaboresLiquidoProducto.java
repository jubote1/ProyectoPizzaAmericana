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
 * Lista los sabores de bebida que vienen con un producto (por ejemplo la gaseosa incluida en un
 * combo) y el recargo de cada uno.
 *
 * Ruta: /ProyectoPizzaAmericana/GetSaboresLiquidoProducto   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idProducto (string), idtienda (integer).
 * Devuelve: application/json; claves: descripcionLiquido, descripcionSabor, idLiquido, idProducto,
 * idSaborTipoLiquido, nombreProducto, valorAdicional.
 * Lee: producto, producto_no_existente, sabor_x_tipo_liquido, tipo_liquido.
 *
 * Trampas y riesgos conocidos:
 * - idProducto ausente o no numerico: cuerpo vacio.
 * - Las claves van en camelCase, distinto del resto del sistema.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetSaboresLiquidoProducto.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetSaboresLiquidoProducto")
public class GetSaboresLiquidoProducto extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetSaboresLiquidoProducto() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El Servicio recibe como parámetro el idproducto y con base en este invocando al método ObtenerSaboresLiquidoProducto 
	 * de la capa Pedido controlador, retorna los sabores tipo liquido en formato JSON, con el objetivo de que la capa
	 * de presentación se encargue de pintarlos, según la elección del pedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			String idProducto = request.getParameter("idProducto");
			int idProdu = Integer.parseInt(idProducto);
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				idtienda = 0;
			}
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.ObtenerSaboresLiquidoProducto(idProdu, idtienda);
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
