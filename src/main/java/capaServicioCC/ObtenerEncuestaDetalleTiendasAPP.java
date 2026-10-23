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
 * EncuestaCtrl.obtenerEncLaboralDetalle.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerEncuestaDetalleTiendasAPP   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idencuesta (integer).
 * Devuelve: texto; claves: alertar, dependencia, descripcion, idencuesta, idencuestadetalle,
 * obligatorio, orden, porcentaje, tiporespuesta, valoralertar, valordefecto, valorescala, ....
 * Lee: encuesta_laboral, encuesta_laboral_detalle.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerEncuestaDetalleTiendasAPP.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerEncuestaDetalleTiendasAPP")
public class ObtenerEncuestaDetalleTiendasAPP extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerEncuestaDetalleTiendasAPP() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		EncuestaCtrl encCtrl = new EncuestaCtrl();
		int idencuesta = Integer.parseInt(request.getParameter("idencuesta"));
        String respuesta = encCtrl.obtenerEncLaboralDetalle(idencuesta);

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
