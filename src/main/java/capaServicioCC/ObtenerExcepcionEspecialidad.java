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
 * Da el recargo en pesos que cuesta una especialidad cuando se pide sobre un producto (tamano)
 * dado.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerExcepcionEspecialidad   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idproducto (integer), idespecialidad (integer).
 * Devuelve: texto; claves: precio.
 * Lee: especialidad_excepcion.
 *
 * Trampas y riesgos conocidos:
 * - 0.0 significa "sin recargo" pero tambien "no encontrado" o "parametro mal escrito": no se
 *   pueden distinguir.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerExcepcionEspecialidad.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerExcepcionEspecialidad")
public class ObtenerExcepcionEspecialidad extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerExcepcionEspecialidad() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idproducto;
		int idespecialidad;
		
		try
		{
			idproducto= Integer.parseInt(request.getParameter("idproducto"));
		}catch(Exception e)
		{
			idproducto = 0;
		}
		try{
			idespecialidad = Integer.parseInt(request.getParameter("idespecialidad"));
		}catch(Exception e)
		{
			idespecialidad = 0;
		}
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
        String respuesta = PedidoCtrl.obtenerPrecioExcepcionEspecialidad(idespecialidad, idproducto);
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
