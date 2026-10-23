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
 * EmpleadoCtrl.obtenerResulEncuestaOperacionDetalle.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerResultadoEncuestaOperacionDet   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idempleadoencuesta (string).
 * Devuelve: texto; claves: item, observacion, orden, porcentaje, respuesta, respuesta_no,
 * respuesta_si, tipo_respuesta, valor_escala, valor_final, valor_inicial.
 * Lee: empleado_encuesta_detalle, encuesta_laboral_detalle.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerResultadoEncuestaOperacionDet.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerResultadoEncuestaOperacionDet")
public class ObtenerResultadoEncuestaOperacionDet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerResultadoEncuestaOperacionDet() {
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
		response.addHeader("Access-Control-Allow-Origin", "*");
		
		String paramEncuesta = request.getParameter("idempleadoencuesta");
		int idempleadoencuesta = 0;
		if(paramEncuesta != null) {
			idempleadoencuesta = Integer.parseInt(paramEncuesta);
			
		}
		EmpleadoCtrl empCtrl = new EmpleadoCtrl();
        String respuesta = empCtrl.obtenerResulEncuestaOperacionDetalle(idempleadoencuesta);
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