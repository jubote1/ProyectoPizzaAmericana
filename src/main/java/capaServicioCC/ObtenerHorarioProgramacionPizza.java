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
 * Devuelve las horas a las que se puede programar un pedido para HOY, en franjas de media hora,
 * segun el horario de apertura y cierre del dia.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerHorarioProgramacionPizza   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): adm (string).
 * Devuelve: texto; claves: hora.
 * Lee: horario_dia.
 *
 * Trampas y riesgos conocidos:
 * - No dice si la tienda esta abierta ahora ni si esta bloqueada.
 * - Si falta la fila del dia en horario_dia, devuelve [] sin avisar.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerHorarioProgramacionPizza.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerHorarioProgramacionPizza")
public class ObtenerHorarioProgramacionPizza extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerHorarioProgramacionPizza() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		String adm = request.getParameter("adm");
		ParametrosCtrl parCtrl = new ParametrosCtrl();
        String respuesta = parCtrl.obtenerHorarioProgramacionPizza();
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
