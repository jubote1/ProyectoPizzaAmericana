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
 * Dice, por tienda, que sabores de bebida estan homologados (equivalentes) con un producto de la
 * tienda, para saber cuales se entregan como "gaseosa incluida".
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerHomologaGaseosaIncluida   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: idsabortipoliquido, idtienda.
 * Lee: homologacion_producto.
 *
 * Trampas y riesgos conocidos:
 * - Devuelve las homologaciones de todas las tiendas juntas; la pantalla filtra despues.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerHomologaGaseosaIncluida.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerHomologaGaseosaIncluida")
public class ObtenerHomologaGaseosaIncluida extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerHomologaGaseosaIncluida() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			PedidoCtrl PedCtrl = new PedidoCtrl();
			String respuesta = PedCtrl.obtenerHomologacionGaseosaIncluida();
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
