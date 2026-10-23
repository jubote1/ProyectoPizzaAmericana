package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EncuestaCtrl;
import capaControladorCC.ParametrosCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * EncuestaCtrl.obtenerEncuestaLaboral.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerEncuestaTiendasAPP   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idencuesta (integer), documento (string).
 * Devuelve: texto; claves: Encabezado, codigo, dependencia, descripcion, idempleado, idencuesta,
 * nombreencuesta, version.
 * Lee: empleado, encuesta_laboral.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerEncuestaTiendasAPP.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerEncuestaTiendasAPP")
public class ObtenerEncuestaTiendasAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerEncuestaTiendasAPP() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		int idencuesta = Integer.parseInt(request.getParameter("idencuesta"));
		String  documento = request.getParameter("documento");
		EncuestaCtrl encCtrl = new EncuestaCtrl();
        String respuesta = encCtrl.obtenerEncuestaLaboral(idencuesta,documento);
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
