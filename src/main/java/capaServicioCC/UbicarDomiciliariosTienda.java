package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import capaDAOCC.DomiciliarioPedidoDAO;


/**
 * Servicio de consulta de Domiciliarios y enrutamiento: invoca DomiciliarioPedidoDAO.obtenerPorId.
 *
 * Ruta: /ProyectoPizzaAmericana/UbicarDomiciliariosTienda   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): t (integer), bool (string).
 * Devuelve: text/plain; claves: clave_dom, fecha, idtienda, latitud, longitud, nombre_largo.
 * Lee: general.empleado, tienda, ubicacion_domiciliario.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha UbicarDomiciliariosTienda.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/UbicarDomiciliariosTienda")
public class UbicarDomiciliariosTienda extends HttpServlet {
	private static final long serialVersionUID = 1L;
	DomiciliarioPedidoDAO dom;
       
	
	public void init() {

		try {

			dom = new DomiciliarioPedidoDAO();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UbicarDomiciliariosTienda() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		try {
			ArrayList<JSONObject>  ubicacion = dom.obtenerPorId(Integer.parseInt(request.getParameter("t")),Boolean.valueOf(request.getParameter("bool")));

			JSONArray jArray = new JSONArray();
			jArray.add(Integer.parseInt(request.getParameter("t")));
			jArray.add(ubicacion);
			response.getWriter().write(jArray.toString());
		} catch (NumberFormatException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	


	
	}

	/**y
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	private void listar(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
		 System.out.print(Integer.parseInt(request.getParameter("t")));
		RequestDispatcher dispatcher = request.getRequestDispatcher("index.jsp");
		dispatcher.forward(request, response);
	}

}



