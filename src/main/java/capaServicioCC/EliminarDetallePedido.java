package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import capaControladorCC.PedidoCtrl;

/**
 * Borra un renglon del pedido y todo lo que le cuelga (sus adiciones, modificadores y renglones
 * asociados).
 *
 * Ruta: /ProyectoPizzaAmericana/EliminarDetallePedido   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): iddetallepedido (integer).
 * Devuelve: texto; claves: iddetallepedido.
 * Lee: adicion_detalle_pedido, detalle_pedido, modificador_detalle_pedido.
 * Escribe: adicion_detalle_pedido (DELETE), detalle_pedido (DELETE), modificador_detalle_pedido
 * (DELETE).
 *
 * Trampas y riesgos conocidos:
 * - Borra fisicamente, sin comprobar de que pedido es el renglon ni en que estado esta ese
 *   pedido. Se puede vaciar un pedido ya finalizado.
 * - Sin sesion y sin transaccion.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha EliminarDetallePedido.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/EliminarDetallePedido")
public class EliminarDetallePedido extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public EliminarDetallePedido() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio el recibirá iddetallepedido que se desea eliminar, finalmente retornará el formato JSON el resultado de la operación
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		Logger logger = Logger.getLogger("log_file");
		int iddetallepedido;
        try
        {
        	iddetallepedido = Integer.parseInt(request.getParameter("iddetallepedido"));
        }catch(Exception e)
        {
        	iddetallepedido= 0;
        }
        logger.info("Llamado a servicio con parámetros iddetallepedido " + iddetallepedido);
        PedidoCtrl detallePedido = new PedidoCtrl();
        String respuestaEliminacion = detallePedido.EliminarDetallePedido(iddetallepedido);
        logger.debug("Respuesta eliminación de detalle pedido " + iddetallepedido + " " + respuestaEliminacion);
        PrintWriter out = response.getWriter();
		out.write(respuestaEliminacion);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
