package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Catalogo y productos (administracion): invoca
 * PedidoCtrl.obtenerCantidadProductoVendidoFecha.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerCantidadProductosVendidos   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): adm (string), idproducto (integer).
 * Devuelve: texto; claves: cantidad.
 * Lee: detalle_pedido, pedido.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerCantidadProductosVendidos.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerCantidadProductosVendidos")
public class ObtenerCantidadProductosVendidos extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerCantidadProductosVendidos() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		String adm = request.getParameter("adm");
		int idProducto = 0;
		try
		{
			idProducto = Integer.parseInt(request.getParameter("idproducto"));
		}catch(Exception e)
		{
			idProducto = 0;
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
        String respuesta = pedCtrl.obtenerCantidadProductoVendidoFecha(idProducto);
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
