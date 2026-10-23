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
 * Lista las tiendas que hoy toman domicilios (funcional = 'S').
 *
 * Ruta: /ProyectoPizzaAmericana/GetTiendasFuncionales   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Devuelve: application/json; claves: alertarpedidos, destino, id, latitud, longitud, manejazonas,
 * nombre, pos, urltienda.
 * Lee: tienda.
 *
 * Trampas y riesgos conocidos:
 * - Si alertarpedidos es nulo en alguna tienda, esa fila lanza una excepcion que el DAO captura:
 *   la lista se corta ahi y el servicio devuelve solo las tiendas anteriores, sin avisar.
 * - Devuelve tambien la URL interna de cada tienda.
 * - Que una tienda sea funcional NO significa que este abierta ni que acepte pedidos ahora: eso
 *   lo dicen horario_dia y tienda_bloqueada.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha GetTiendasFuncionales.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/GetTiendasFuncionales")
public class GetTiendasFuncionales extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetTiendasFuncionales() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio no recibe parámetros dado que no filtra la información, simplemente retorna en formato JSON 
	 * las tiendas o puntos de venta parametrizados en el sistema, invocando el método obtenerTiendas de la capa Tienda Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try{
			response.addHeader("Access-Control-Allow-Origin", "*");
			response.setContentType("application/json");
			TiendaCtrl TienCtrl = new TiendaCtrl();
			String respuesta = TienCtrl.obtenerTiendasFuncionales();
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
