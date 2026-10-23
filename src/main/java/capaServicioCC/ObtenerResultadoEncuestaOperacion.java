package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EmpleadoCtrl;


/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * EmpleadoCtrl.obtenerResultadoEncuestaOperacion.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerResultadoEncuestaOperacion   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer), fecha1 (string), fecha2 (string), idencuesta (string).
 * Devuelve: texto; claves: descripcion_encuesta, fecha_hora, idempleadoencuesta, idtienda, item,
 * nombre_empleado, observacion, orden, porcentaje, porcentaje_total, respuesta, respuesta_no, ....
 * Lee: empleado, empleado_encuesta, empleado_encuesta_detalle, encuesta_laboral,
 * encuesta_laboral_detalle.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerResultadoEncuestaOperacion.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerResultadoEncuestaOperacion")
public class ObtenerResultadoEncuestaOperacion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerResultadoEncuestaOperacion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * Este servicio recibe como par�metro el idpedido correspondiente y se encarga de retornar el total del pedido,
	 * sumando los detalles de pedidos asociados al pedido, esto invocando al m�todo obtenerTotalPedido de la capa
	 * Pedido Controlador.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			response.addHeader("Access-Control-Allow-Origin", "*");
			int idTienda = Integer.parseInt(request.getParameter("idtienda"));
			String fecha1 = request.getParameter("fecha1");
			String fecha2 = request.getParameter("fecha2");
			String paramEncuesta = request.getParameter("idencuesta");
			int idencuesta = 0;
			if(paramEncuesta != null) {
				idencuesta = Integer.parseInt(paramEncuesta);
				
			}
			EmpleadoCtrl empCtrl = new EmpleadoCtrl();
	        String respuesta = empCtrl.obtenerResultadoEncuestaOperacion(idTienda,idencuesta,fecha1,fecha2);
	        PrintWriter out = response.getWriter();
			out.write(respuesta);
		}catch(Exception e) {
			System.out.println(e);
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


