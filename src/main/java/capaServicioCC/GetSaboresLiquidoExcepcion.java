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
 * Igual que el anterior, pero para la bebida que incluye una promocion.
 *
 * Ruta: /ProyectoPizzaAmericana/GetSaboresLiquidoExcepcion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idExcepcion (string), idtienda (integer).
 * Devuelve: application/json; claves: descripcionExcepcion, descripcionLiquido, descripcionSabor,
 * idExcepcion, idLiquido, idSaborTipoLiquido, valorAdicional.
 * Lee: excepcion_precio, producto_no_existente, sabor_x_tipo_liquido, tipo_liquido.
 *
 * Trampas y riesgos conocidos:
 * - Mismos de GetSaboresLiquidoProducto.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetSaboresLiquidoExcepcion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetSaboresLiquidoExcepcion")
public class GetSaboresLiquidoExcepcion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetSaboresLiquidoExcepcion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro idExcepcion, con base en este invocando al método ObtenerSaboresLiquidoExcepcion(idExcep);
	 * de la capa Pedido controlador, retorna en formato JSON los tipos liquidos asociados a la excepción de precio, esto
	 * con el fin de pintarlo en la capa de presentación.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			String idExcepcion = request.getParameter("idExcepcion");
			int idExcep = Integer.parseInt(idExcepcion);
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				idtienda = 0;
			}
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.ObtenerSaboresLiquidoExcepcion(idExcep, idtienda);
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
