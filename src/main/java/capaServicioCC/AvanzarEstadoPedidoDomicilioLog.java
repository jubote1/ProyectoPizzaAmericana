package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import capaControladorCC.PedidoCtrl;
import capaDAOCC.LogAvanzarEstadoAppDAO;

/**
 * Servicio que modifica datos de Domiciliarios y enrutamiento: invoca
 * LogAvanzarEstadoAppDAO.insertarLogPedidoVirtual, PedidoCtrl.avanzarEstadoPedidoDomicilioLog.
 *
 * Ruta: /ProyectoPizzaAmericana/AvanzarEstadoPedidoDomicilioLog   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idpedidotienda (integer), idtienda (integer), claveusuario (string),
 * observacion_dom (string).
 * Devuelve: application/json;charset=UTF-8; claves: resultado, tipo_error.
 * Lee: tienda.
 * Escribe: log_avanzar_estado_app (INSERT/UPDATE).
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha AvanzarEstadoPedidoDomicilioLog.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/AvanzarEstadoPedidoDomicilioLog")
public class AvanzarEstadoPedidoDomicilioLog extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AvanzarEstadoPedidoDomicilioLog() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como par�metro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocar� el m�todo de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json;charset=UTF-8");
		request.setCharacterEncoding("UTF-8");
		int idPedidoTienda = Integer.parseInt(request.getParameter("idpedidotienda"));
		int idTienda = Integer.parseInt(request.getParameter("idtienda"));
		String claveUsuario = request.getParameter("claveusuario");
		String observacion = request.getParameter("observacion_dom");
		if(observacion == null)
		{
			observacion = "";
		}
		PedidoCtrl PedidoCtrl = new PedidoCtrl();
		int idLog = LogAvanzarEstadoAppDAO.insertarLogPedidoVirtual(idTienda, idPedidoTienda, claveUsuario, observacion);
        String respuesta = PedidoCtrl.avanzarEstadoPedidoDomicilioLog(idPedidoTienda, idTienda, claveUsuario, observacion);
        LogAvanzarEstadoAppDAO.actualizarLogPedidoVirtual(idLog, respuesta);
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
