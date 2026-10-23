package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.TiendaCtrl;

/**
 * Da la direccion del servicio local de una tienda, su nombre de origen de datos y si tiene el POS
 * nuevo.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerUrlTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer).
 * Devuelve: texto; claves: dsnodbc, pos, urltienda.
 * Lee: tienda.
 *
 * Trampas y riesgos conocidos:
 * - El servlet no tiene try/catch: un idtienda ausente o no numerico lanza una excepcion y
 *   Tomcat responde HTTP 500 con su pagina de error.
 * - Si la tienda no existe devuelve valores nulos, no un error.
 * - Expone las URLs internas de las tiendas a cualquiera que llame.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerUrlTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerUrlTienda")
public class ObtenerUrlTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerUrlTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como parámetro el idtienda y con base en este se retorna la URL tienda que será la base, para 
	 * invocar los servicios para la inserción del pedido en la tienda.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
				response.addHeader("Access-Control-Allow-Origin", "*");
				int idtienda = Integer.parseInt(request.getParameter("idtienda"));
				TiendaCtrl TiendaCtrl = new TiendaCtrl();
		        String respuesta = TiendaCtrl.obtenerUrlTienda(idtienda);
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
