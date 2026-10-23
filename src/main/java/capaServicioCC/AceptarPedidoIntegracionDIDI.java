package capaServicioCC;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import capaControladorCC.PedidoCtrl;
import capaControladorCC.SolicitudPQRSCtrl;

/**
 * Servicio que modifica datos de Plataformas (Rappi, DiDi, Kuno): invoca
 * PedidoCtrl.aceptarPedidoIntegracionDIDI.
 *
 * Ruta: /ProyectoPizzaAmericana/AceptarPedidoIntegracionDIDI   Metodos: GET, POST (doPost delega en doGet).
 * Tipo: Modifica datos.  Acceso: Sin validacion propia.
 * Recibe (query/form): idordencomercio (string).
 * Devuelve: texto; claves: auth_token, order_id, respuesta.
 * Lee: cliente, estado_pedido, forma_pago, integracion_crm, pedido, pedido_forma_pago, tienda.
 * Escribe: pedido (UPDATE).
 * Sistemas externos: DiDi, Rappi.
 *
 * Documentacion detallada: "Catalogo de servicios del central" (carpeta INVENTARIO SERVICIOS), ficha AceptarPedidoIntegracionDIDI.
 * Estado de esta descripcion: verificada a mano el 2026-10-23 leyendo servlet, controlador y DAO.
 */
@WebServlet("/AceptarPedidoIntegracionDIDI")
public class AceptarPedidoIntegracionDIDI extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AceptarPedidoIntegracionDIDI() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * El servicio recibe como parámetro el idpedido, el idformapgo, el idcliente asociado al pedido, un marcador que nos
	 * indica si el cliente fue insertado o por el contrario actualizado, valor de la forma pago del cliente, con los datos
	 * anteriores se invocará el método de la capa controlador pedido FinalizarPedido.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.addHeader("Access-Control-Allow-Origin", "*");
		BigInteger idOrdenComercio;
		try
		{
			idOrdenComercio = new BigInteger(request.getParameter("idordencomercio"));
		}catch(Exception e)
		{
			idOrdenComercio = new BigInteger("0");
		}
		PedidoCtrl pedCtrl = new PedidoCtrl();
		String respuesta = pedCtrl.aceptarPedidoIntegracionDIDI(idOrdenComercio);
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
