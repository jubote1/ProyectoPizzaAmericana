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
 * Devuelve los productos que una tienda si tiene (quita los marcados como no existentes en esa
 * tienda).
 *
 * Ruta: /ProyectoPizzaAmericana/GetProductosTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer), plataforma (string).
 * Devuelve: application/json; claves: controlaespecialidades, descripcion, idproducto, idreceta,
 * idtipo_liquido, impuesto, incluye_liquido, manejacantidad, nombre, preciogeneral, preciopuntos,
 * productoasociaadicion, ....
 * Lee: producto, producto_no_existente.
 *
 * Trampas y riesgos conocidos:
 * - Lo que una tienda no tiene se maneja en producto_no_existente (pantalla
 *   MarcarProductoNoExistente.html); este servicio solo lo lee.
 * - El precio es el general, no uno por tienda.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetProductosTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetProductosTienda")
public class GetProductosTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetProductosTienda() {
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
			int idtienda;
			try
			{
				idtienda = Integer.parseInt(request.getParameter("idtienda"));
			}catch(Exception e)
			{
				System.out.println(e.toString());
				idtienda = 0;
			}
			String plataforma = request.getParameter("plataforma");
			if(plataforma == null )
			{
				plataforma = "N";
			}
			if(plataforma.equals(new String("")))
			{
				plataforma = "N";
			}
			PedidoCtrl PedidoCtrl = new PedidoCtrl();
			String respuesta = PedidoCtrl.GetProductosTiendaPlat(idtienda, plataforma);
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
