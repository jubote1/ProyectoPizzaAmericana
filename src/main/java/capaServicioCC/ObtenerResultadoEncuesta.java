package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.EmpleadoCtrl;
import capaControladorCC.PedidoCtrl;

/**
 * Servicio de consulta de Campanas, publicidad y mercadeo: invoca
 * EmpleadoCtrl.obtenerResultadoEncuesta.
 *
 * Ruta: /ProyectoPizzaAmericana/ObtenerResultadoEncuesta   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Consulta.  Acceso: Sin validacion propia.
 * Recibe (query/form): idtienda (integer).
 * Devuelve: texto; claves: cantidad, nombre, promedio.
 * Lee: empleado, empleado_encuesta, empleado_encuesta_detalle.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha ObtenerResultadoEncuesta.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/ObtenerResultadoEncuesta")
public class ObtenerResultadoEncuesta extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ObtenerResultadoEncuesta() {
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
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		EmpleadoCtrl empCtrl = new EmpleadoCtrl();
        String respuesta = empCtrl.obtenerResultadoEncuesta(idTienda,4);
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
